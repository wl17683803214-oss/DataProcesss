package com.example.dataadmin.service.monitor.impl;

import com.example.dataadmin.config.SystemMonitorProperties;
import com.example.dataadmin.entity.SysAlarmEvent;
import com.example.dataadmin.entity.SysMonitorSnapshot;
import com.example.dataadmin.enums.SystemHealthAlarmChangeType;
import com.example.dataadmin.mapper.SysAlarmEventMapper;
import com.example.dataadmin.mapper.SysMonitorSnapshotMapper;
import com.example.dataadmin.service.monitor.SystemMonitorService;
import com.example.dataadmin.service.monitor.support.SystemResourceMetrics;
import com.example.dataadmin.service.monitor.RocketMqMonitorService;
import com.example.dataadmin.vo.alarm.SystemHealthAlarmChangeVO;
import com.example.dataadmin.vo.alarm.SystemMonitorPushVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 系统健康监控实现，负责采样、快照缓存、批量持久化和健康告警推送。 */
@Service
@Transactional(readOnly = true)
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private static final String CURRENT_KEY = "system:monitor:current";
    private static final String PROCESS_HEARTBEAT_KEY =
            "system:monitor:process-heartbeat";
    private final SysAlarmEventMapper systemAlarmMapper;
    private final SysMonitorSnapshotMapper snapshotMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RocketMqMonitorService rocketMqMonitorService;
    /** 系统告警和处理页面共用的机器资源采集组件。 */
    @org.springframework.beans.factory.annotation.Autowired
    private SystemResourceMetrics systemResourceMetrics;

    /** 实时查询采集接口总数与已启用数量。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.dataadmin.mapper.CollectInterfaceConfigMapper interfaceMapper;
    /** 通过注册中心读取处理服务健康实例。 */
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient;

    /** 查询注册中心实际服务状态，查询失败与无实例分开表示。 */
    private String readServiceStatus() {
        try {
            return discoveryClient.getInstances("data-process-service").isEmpty()
                    ? "DOWN" : "RUNNING";
        } catch (Exception exception) {
            return "ERROR";
        }
    }

    /** 按配置周期批量持久化暂存的快照。 */
    private final List<SysMonitorSnapshot> pendingSnapshots =
            new ArrayList<SysMonitorSnapshot>();

    /** 记录各指标本轮持续超限的起始时间。 */
    private final Map<String, LocalDateTime> breachStarts =
            new HashMap<String, LocalDateTime>();
    /** 采样、入库与告警持续时间配置。 */
    private final SystemMonitorProperties monitorProperties;

    /** CPU 使用率告警阈值，单位为百分比，默认 85%。 */
    @Value("${system-monitor.cpu-warning-percent}")
    private double cpuThreshold;

    /** 内存使用率告警阈值，单位为百分比，默认 90%。 */
    @Value("${system-monitor.memory-warning-percent}")
    private double memoryThreshold;

    /** 磁盘使用率告警阈值，单位为百分比，默认 85%。 */
    @Value("${system-monitor.disk-warning-percent}")
    private double diskThreshold;

    /** 消息队列待处理消息数告警阈值，默认积压 1000 条时告警。 */
    @Value("${system-monitor.queue-warning-depth}")
    private long queueThreshold;

    /** 处理服务心跳正常判断时限，单位毫秒。 */
    @Value("${system-monitor.process-heartbeat-timeout-millis:15000}")
    private long heartbeatTimeoutMillis;

    /** 注入健康采样、快照存储和告警推送所需的既有组件。 */
    public SystemMonitorServiceImpl(
            SysAlarmEventMapper systemAlarmMapper,
            SysMonitorSnapshotMapper snapshotMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,
            RocketMqMonitorService rocketMqMonitorService,
            SystemMonitorProperties monitorProperties) {
        // 统一保存组件引用，保持原有采样和事务处理流程。
        this.systemAlarmMapper = systemAlarmMapper;
        this.snapshotMapper = snapshotMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.rocketMqMonitorService = rocketMqMonitorService;
        this.monitorProperties = monitorProperties;
    }

    /** 按配置周期采集状态、判断告警，并推送给系统告警页面。 */
    @Override
    @Scheduled(fixedRateString = "#{@systemMonitorProperties.sampleIntervalMillis}")
    @Transactional
    public void collectSystemMonitorStatus() {
        // 第一步：采集当前系统资源、数据库、消息队列和处理服务心跳状态。
        SysMonitorSnapshot snapshot = buildSnapshot();

        // 第二步：写入 Redis，供前端首次打开页面时快速查询最新状态。
        cacheCurrentSnapshot(snapshot);

        // 第三步：暂存在内存中，按配置周期批量写入数据库，减少频繁写库。
        synchronized (pendingSnapshots) {
            pendingSnapshots.add(snapshot);
        }

        // 第四步：判断各项指标持续超过阈值是否达到配置时长，并收集本轮发生变化的告警。
        List<SystemHealthAlarmChangeVO> alarmEvents =
                evaluateHealthAlarms(snapshot);

        // 第五步：系统状态和告警变化使用同一个消息推送，前端只需订阅一个主题。
        SystemMonitorPushVO pushMessage = new SystemMonitorPushVO();
        pushMessage.setSnapshot(snapshot);
        pushMessage.setAlarmEvents(alarmEvents);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        messagingTemplate.convertAndSend("/topic/system-monitor", pushMessage);
                    }
                });
    }

    /** 按配置周期通过 MyBatis 写入暂存的系统状态快照。 */
    @Override
    @Scheduled(fixedRateString = "#{@systemMonitorProperties.snapshotFlushIntervalMillis}")
    @Transactional
    public void flushSystemMonitorSnapshots() {
        List<SysMonitorSnapshot> snapshots;
        synchronized (pendingSnapshots) {
            snapshots = new ArrayList<SysMonitorSnapshot>(pendingSnapshots);
            pendingSnapshots.clear();
        }
        // 数据库批次回滚时恢复快照，保证临时写库失败不会丢失已采样数据。
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            synchronized (pendingSnapshots) { pendingSnapshots.addAll(0, snapshots); }
                        }
                    }
                });
        for (SysMonitorSnapshot snapshot : snapshots) {
            snapshotMapper.insert(snapshot);
        }
    }

    @Override
    public SysMonitorSnapshot getSystemMonitorCurrent() {
        // 页面主动查询时即时采集，避免缓存掩盖接口开关和服务状态变化。
        return buildSnapshot();
    }

    @Override
    public List<SysMonitorSnapshot> getSystemMonitorHistory(
            int minutes) {
        // 查询区间与保留策略独立，留空保留天数时仍允许查询已有历史数据。
        int safeMinutes = Math.max(1, minutes);
        return snapshotMapper.findHistory(
                LocalDateTime.now().minusMinutes(safeMinutes));
    }

    /**
     * 组装一次系统状态快照。
     * CPU、内存和磁盘为当前管理服务所在机器的数据；数据库和消息队列数据通过实际连接获取。
     */
    private SysMonitorSnapshot buildSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        Long heartbeatMillis = readProcessHeartbeat();
        double cpu = readCpuUsage();
        double memory = readMemoryUsage();
        double disk = readDiskUsage();

        SysMonitorSnapshot snapshot = new SysMonitorSnapshot();
        snapshot.setSampleTime(now);

        // 按处理服务在注册中心的健康实例判断真实可用状态。
        snapshot.setServiceStatus(readServiceStatus());

        // 这里统计的是 data-admin-service 当前 JVM 的运行时长，单位为秒。
        snapshot.setUptimeSeconds(
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000L);

        // 节点卡片按采集接口统计，在线表示已启用，不代表网络连接在线。
        com.example.dataadmin.vo.collection.CollectionOverviewVO counts = interfaceMapper.findNodeCounts();
        snapshot.setOnlineNodeCount(counts.getOnlineTotal().intValue());
        snapshot.setTotalNodeCount(counts.getInterfaceTotal().intValue());
        snapshot.setDatabaseLatencyMs(readDatabaseLatency());
        snapshot.setMessageQueueDepth(readQueueDepth());
        snapshot.setLastHeartbeatTime(heartbeatMillis == null
                ? null : timestampToLocalDateTime(heartbeatMillis));
        snapshot.setHeartbeatStatus(isHeartbeatNormal(heartbeatMillis)
                ? "NORMAL" : "WAITING");
        snapshot.setCpuUsagePercent(cpu);
        snapshot.setMemoryUsagePercent(memory);
        snapshot.setDiskUsagePercent(disk);
        snapshot.setHealthScore(calculateHealthScore(cpu, memory, disk));
        return snapshot;
    }

    private void cacheCurrentSnapshot(SysMonitorSnapshot snapshot) {
        try {
            redisTemplate.opsForValue().set(
                    CURRENT_KEY,
                    objectMapper.writeValueAsString(snapshot),
                    30,
                    TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // Redis 是实时缓存，失败不应阻断告警判断和历史落库。
        }
    }

    /** 判断全部系统指标，并返回本轮新增、更新或恢复的告警事件。 */
    private List<SystemHealthAlarmChangeVO> evaluateHealthAlarms(
            SysMonitorSnapshot snapshot) {
        List<SystemHealthAlarmChangeVO> changes =
                new ArrayList<SystemHealthAlarmChangeVO>();

        addAlarmChange(changes, checkMetric(
                snapshot, "cpu_usage", "计算服务CPU",
                snapshot.getCpuUsagePercent(), cpuThreshold, "CPU使用率"));
        addAlarmChange(changes, checkMetric(
                snapshot, "memory_usage", "计算服务内存",
                snapshot.getMemoryUsagePercent(), memoryThreshold, "内存使用率"));
        addAlarmChange(changes, checkMetric(
                snapshot, "disk_usage", "日志与缓存磁盘",
                snapshot.getDiskUsagePercent(), diskThreshold, "磁盘使用率"));
        Double queueDepth = snapshot.getMessageQueueDepth() == null ? null
                : snapshot.getMessageQueueDepth().doubleValue();
        addAlarmChange(changes, checkMetric(
                snapshot, "queue_depth", "消息队列",
                queueDepth, queueThreshold, "消息积压数量"));
        return changes;
    }

    /**
     * 检查单个指标。
     * 连续超阈值达到配置时长才触发；持续异常更新原事件，恢复时关闭原事件。
     */
    private SystemHealthAlarmChangeVO checkMetric(
            SysMonitorSnapshot snapshot,
            String metricCode,
            String targetName,
            Double actualValue,
            double threshold,
            String metricName) {
        if (actualValue == null) {
            // 数据缺失时中断连续超限计时，避免把缺失期间计为持续异常。
            breachStarts.remove(metricCode);
            return null;
        }

        String breachKey = metricCode;
        if (actualValue <= threshold) {
            breachStarts.remove(breachKey);

            // 没有活动告警时不执行无意义的更新，也不会向前端发送重复恢复消息。
            SysAlarmEvent open = systemAlarmMapper.findOpen(
                    metricCode);
            if (open == null) {
                return null;
            }
            systemAlarmMapper.recoverById(
                    open.getId(), snapshot.getSampleTime());
            open.setAlarmStatus(1);
            open.setRecoverTime(snapshot.getSampleTime());
            open.setUpdateTime(snapshot.getSampleTime());
            return buildAlarmChange(
                    SystemHealthAlarmChangeType.RECOVERED, open);
        }

        // 根据快照时间计算持续时长，调整采样周期不会改变告警等待时间。
        LocalDateTime firstBreach = breachStarts.get(breachKey);
        if (firstBreach == null || snapshot.getSampleTime().isBefore(firstBreach)) {
            firstBreach = snapshot.getSampleTime();
            breachStarts.put(breachKey, firstBreach);
        }
        if (Duration.between(firstBreach, snapshot.getSampleTime()).toMillis()
                < monitorProperties.getAlarmDurationMillis()) {
            return null;
        }

        SysAlarmEvent open = systemAlarmMapper.findOpen(
                metricCode);
        if (open != null) {
            int level = actualValue > threshold * 1.1 ? 3 : 2;
            String content = metricName + "为" + format(actualValue) + "，持续超过阈值" + format(threshold);
            systemAlarmMapper.updateActive(open.getId(), actualValue, snapshot.getSampleTime(), level, content);
            open.setAlarmLevel(level);
            open.setAlarmContent(content);
            open.setCurrentValue(actualValue);
            open.setLastAlarmTime(snapshot.getSampleTime());
            open.setUpdateTime(snapshot.getSampleTime());
            open.setTriggerCount(open.getTriggerCount() == null
                    ? 1 : open.getTriggerCount() + 1);
            return buildAlarmChange(
                    SystemHealthAlarmChangeType.UPDATED, open);
        }

        SysAlarmEvent event = new SysAlarmEvent();
        event.setAlarmTime(snapshot.getSampleTime());
        event.setObjName(targetName);
        // 告警类型复用已有监测指标编码，中文类型名称由查询实体从枚举生成。
        event.setAlarmType(metricCode);
        event.setMetricCode(metricCode);
        event.setAlarmLevel(actualValue > threshold * 1.1 ? 3 : 2);
        event.setAlarmContent(metricName + "为" + format(actualValue)
                + "，持续超过阈值" + format(threshold));
        event.setCurrentValue(actualValue);
        event.setThresholdValue(threshold);
        event.setFirstAlarmTime(snapshot.getSampleTime());
        event.setLastAlarmTime(snapshot.getSampleTime());
        event.setAlarmStatus(0);
        event.setTriggerCount(1);
        systemAlarmMapper.insert(event);
        return buildAlarmChange(SystemHealthAlarmChangeType.CREATED, event);
    }

    /** 只收集实际发生变化的告警，正常且无活动告警时不加入推送数组。 */
    private void addAlarmChange(
            List<SystemHealthAlarmChangeVO> changes,
            SystemHealthAlarmChangeVO change) {
        if (change != null) {
            changes.add(change);
        }
    }

    /** 组装一条前端可直接处理的告警变化消息。 */
    private SystemHealthAlarmChangeVO buildAlarmChange(
            SystemHealthAlarmChangeType changeType,
            SysAlarmEvent alarmEvent) {
        SystemHealthAlarmChangeVO change = new SystemHealthAlarmChangeVO();
        change.setChangeType(changeType);
        change.setAlarmEvent(alarmEvent);
        return change;
    }

    /** 通过 MyBatis 轻量查询测量数据库连接耗时。 */
    private Long readDatabaseLatency() {
        try {
            long start = System.nanoTime();
            snapshotMapper.ping();
            return (System.nanoTime() - start) / 1000000L;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long readQueueDepth() {
        // RocketMQ积压量由结果主题和下游消费者组的位点差值计算。
        return rocketMqMonitorService.readBacklog();
    }

    private Long readProcessHeartbeat() {
        try {
            String value = redisTemplate.opsForValue().get(PROCESS_HEARTBEAT_KEY);
            return value == null ? null : Long.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** CPU与内存统一委托共用组件采集，保证处理页面和系统告警口径一致。 */
    private double readCpuUsage() {
        // 读取管理服务所在机器整体CPU使用率。
        return systemResourceMetrics.readCpuUsage();
    }

    /** 读取管理服务所在机器的物理内存使用率。 */
    private double readMemoryUsage() {
        // 与处理页面复用同一物理内存计算逻辑。
        return systemResourceMetrics.readMemoryUsage();
    }

    private double readDiskUsage() {
        File disk = new File(".");
        long total = disk.getTotalSpace();
        return total <= 0 ? 0 : round(
                (total - disk.getUsableSpace()) * 100.0 / total);
    }

    /** 根据最后上报时间判断处理服务心跳是否正常。 */
    private boolean isHeartbeatNormal(Long heartbeatMillis) {
        // 按配置时限判断最后一次成功上报是否仍有效。
        return heartbeatMillis != null
                && heartbeatMillis >= System.currentTimeMillis() - heartbeatTimeoutMillis;
    }

    private int calculateHealthScore(double cpu, double memory, double disk) {
        return Math.max(0,
                (int) Math.round(100 - Math.max(cpu, Math.max(memory, disk))));
    }

    private static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return new java.sql.Timestamp(timestamp).toLocalDateTime();
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}
