package com.example.dataadmin.service.impl;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.DmAlarmEvent;
import com.example.dataadmin.entity.SysAlarmEvent;
import com.example.dataadmin.entity.SysHealthAlarmEvent;
import com.example.dataadmin.entity.SysMonitorSnapshot;
import com.example.dataadmin.enums.SystemHealthAlarmChangeType;
import com.example.dataadmin.mapper.DmAlarmEventMapper;
import com.example.dataadmin.mapper.SysAlarmEventMapper;
import com.example.dataadmin.mapper.SysHealthAlarmEventMapper;
import com.example.dataadmin.mapper.SysMonitorSnapshotMapper;
import com.example.dataadmin.service.AlarmManagementService;
import com.example.dataadmin.service.RocketMqMonitorService;
import com.example.dataadmin.vo.alarm.SystemHealthAlarmChangeVO;
import com.example.dataadmin.vo.alarm.SystemMonitorPushVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 告警管理业务实现，包含数据告警和系统健康监控。 */
@Service
@Transactional(readOnly = true)
public class AlarmManagementServiceImpl implements AlarmManagementService {

    private static final String CURRENT_KEY = "system:monitor:current";
    private static final String PROCESS_HEARTBEAT_KEY =
            "system:monitor:process-heartbeat";
    private final SysAlarmEventMapper systemAlarmMapper;
    private final DmAlarmEventMapper dataAlarmMapper;
    private final SysMonitorSnapshotMapper snapshotMapper;
    private final SysHealthAlarmEventMapper healthAlarmMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RocketMqMonitorService rocketMqMonitorService;

    /** 每 30 秒批量持久化一次，最多暂存 6 个快照。 */
    private final List<SysMonitorSnapshot> pendingSnapshots =
            new ArrayList<SysMonitorSnapshot>();

    /** 记录各指标连续超过阈值的次数。 */
    private final Map<String, Integer> breachCounts =
            new HashMap<String, Integer>();

    /** 系统状态快照保留天数，默认保留 7 天。 */
    @Value("${system-monitor.snapshot-retention-days}")
    private int retentionDays;

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

    public AlarmManagementServiceImpl(
            SysAlarmEventMapper systemAlarmMapper,
            DmAlarmEventMapper dataAlarmMapper,
            SysMonitorSnapshotMapper snapshotMapper,
            SysHealthAlarmEventMapper healthAlarmMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,
            RocketMqMonitorService rocketMqMonitorService) {
        this.systemAlarmMapper = systemAlarmMapper;
        this.dataAlarmMapper = dataAlarmMapper;
        this.snapshotMapper = snapshotMapper;
        this.healthAlarmMapper = healthAlarmMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.rocketMqMonitorService = rocketMqMonitorService;
    }

    /** 每 5 秒采集状态、判断告警，并推送给系统告警页面。 */
    @Scheduled(fixedRate = 5000L)
    @Transactional
    public void collectSystemMonitorStatus() {
        // 第一步：采集当前系统资源、数据库、消息队列和处理服务心跳状态。
        SysMonitorSnapshot snapshot = buildSnapshot();

        // 第二步：写入 Redis，供前端首次打开页面时快速查询最新状态。
        cacheCurrentSnapshot(snapshot);

        // 第三步：暂存在内存中，每 30 秒批量写入数据库，减少频繁写库。
        synchronized (pendingSnapshots) {
            pendingSnapshots.add(snapshot);
        }

        // 第四步：判断各项指标是否连续三次超过阈值，并收集本轮发生变化的告警。
        List<SystemHealthAlarmChangeVO> alarmEvents =
                evaluateHealthAlarms(snapshot);

        // 第五步：系统状态和告警变化使用同一个消息推送，前端只需订阅一个主题。
        SystemMonitorPushVO pushMessage = new SystemMonitorPushVO();
        pushMessage.setSnapshot(snapshot);
        pushMessage.setAlarmEvents(alarmEvents);
        messagingTemplate.convertAndSend(
                "/topic/system-monitor", pushMessage);
    }

    /** 每 30 秒通过 MyBatis 写入暂存的系统状态快照。 */
    @Scheduled(fixedRate = 30000L)
    @Transactional
    public void flushSystemMonitorSnapshots() {
        List<SysMonitorSnapshot> snapshots;
        synchronized (pendingSnapshots) {
            snapshots = new ArrayList<SysMonitorSnapshot>(pendingSnapshots);
            pendingSnapshots.clear();
        }
        for (SysMonitorSnapshot snapshot : snapshots) {
            snapshotMapper.insert(snapshot);
        }
    }

    /** 每天清理七天前的系统状态快照。 */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void clearExpiredSystemMonitorSnapshots() {
        snapshotMapper.deleteBefore(LocalDateTime.now().minusDays(retentionDays));
    }

    @Override
    public SysMonitorSnapshot getSystemMonitorCurrent() {
        try {
            String cached = redisTemplate.opsForValue().get(
                    CURRENT_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, SysMonitorSnapshot.class);
            }
        } catch (Exception ignored) {
            // Redis 不可用时即时采集，保证页面仍可展示。
        }
        return buildSnapshot();
    }

    @Override
    public List<SysMonitorSnapshot> getSystemMonitorHistory(
            int minutes) {
        int maximum = retentionDays * 24 * 60;
        int safeMinutes = Math.max(1, Math.min(minutes, maximum));
        return snapshotMapper.findHistory(
                LocalDateTime.now().minusMinutes(safeMinutes));
    }

    @Override
    public List<SysHealthAlarmEvent> getSystemHealthAlarms(
            Integer status) {
        return healthAlarmMapper.findAll(status);
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

        // 当前定时采集任务能够执行时，先将管理服务自身标记为运行中。
        // 后续接入多处理节点后，应根据各节点上报的心跳汇总实际服务状态。
        snapshot.setServiceStatus("RUNNING");

        // 这里统计的是 data-admin-service 当前 JVM 的运行时长，单位为秒。
        snapshot.setUptimeSeconds(
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000L);

        // 当前按单节点部署处理；多节点版本应从 Redis 心跳或 Nacos 实例列表动态统计。
        snapshot.setOnlineNodeCount(1);
        snapshot.setTotalNodeCount(1);
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
     * 连续三次超阈值才触发；持续异常更新原事件，恢复时关闭原事件。
     */
    private SystemHealthAlarmChangeVO checkMetric(
            SysMonitorSnapshot snapshot,
            String metricCode,
            String targetName,
            Double actualValue,
            double threshold,
            String metricName) {
        if (actualValue == null) {
            return null;
        }

        String breachKey = metricCode;
        if (actualValue <= threshold) {
            breachCounts.remove(breachKey);

            // 没有活动告警时不执行无意义的更新，也不会向前端发送重复恢复消息。
            SysHealthAlarmEvent open = healthAlarmMapper.findOpen(
                    metricCode);
            if (open == null) {
                return null;
            }
            healthAlarmMapper.recoverById(
                    open.getId(), snapshot.getSampleTime());
            open.setAlarmStatus(1);
            open.setRecoverTime(snapshot.getSampleTime());
            open.setUpdateTime(snapshot.getSampleTime());
            return buildAlarmChange(
                    SystemHealthAlarmChangeType.RECOVERED, open);
        }

        int count = breachCounts.containsKey(breachKey)
                ? breachCounts.get(breachKey) + 1 : 1;
        breachCounts.put(breachKey, count);
        if (count < 3) {
            return null;
        }

        SysHealthAlarmEvent open = healthAlarmMapper.findOpen(
                metricCode);
        if (open != null) {
            healthAlarmMapper.updateActive(
                    open.getId(), actualValue, snapshot.getSampleTime());
            open.setCurrentValue(actualValue);
            open.setLastAlarmTime(snapshot.getSampleTime());
            open.setUpdateTime(snapshot.getSampleTime());
            open.setTriggerCount(open.getTriggerCount() == null
                    ? 1 : open.getTriggerCount() + 1);
            return buildAlarmChange(
                    SystemHealthAlarmChangeType.UPDATED, open);
        }

        SysHealthAlarmEvent event = new SysHealthAlarmEvent();
        event.setAlarmTime(snapshot.getSampleTime());
        event.setTargetName(targetName);
        event.setMetricCode(metricCode);
        event.setAlarmLevel(actualValue > threshold * 1.1 ? 3 : 2);
        event.setAlarmContent(metricName + "为" + format(actualValue)
                + "，连续3次超过阈值" + format(threshold));
        event.setCurrentValue(actualValue);
        event.setThresholdValue(threshold);
        event.setFirstAlarmTime(snapshot.getSampleTime());
        event.setLastAlarmTime(snapshot.getSampleTime());
        event.setAlarmStatus(0);
        event.setTriggerCount(1);
        healthAlarmMapper.insert(event);
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
            SysHealthAlarmEvent alarmEvent) {
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

    private double readCpuUsage() {
        java.lang.management.OperatingSystemMXBean bean =
                ManagementFactory.getOperatingSystemMXBean();
        if (!(bean instanceof com.sun.management.OperatingSystemMXBean)) {
            return 0;
        }
        double value = ((com.sun.management.OperatingSystemMXBean) bean)
                .getSystemCpuLoad();
        return round(Math.max(0, value * 100));
    }

    private double readMemoryUsage() {
        com.sun.management.OperatingSystemMXBean bean =
                (com.sun.management.OperatingSystemMXBean)
                        ManagementFactory.getOperatingSystemMXBean();
        long total = bean.getTotalPhysicalMemorySize();
        return total <= 0 ? 0 : round(
                (total - bean.getFreePhysicalMemorySize()) * 100.0 / total);
    }

    private double readDiskUsage() {
        File disk = new File(".");
        long total = disk.getTotalSpace();
        return total <= 0 ? 0 : round(
                (total - disk.getUsableSpace()) * 100.0 / total);
    }

    private boolean isHeartbeatNormal(Long heartbeatMillis) {
        return heartbeatMillis != null
                && heartbeatMillis >= System.currentTimeMillis() - 15000L;
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

    @Override
    public List<SysAlarmEvent> listSystemAlarms() {
        return systemAlarmMapper.findAll();
    }

    @Override
    public SysAlarmEvent getSystemAlarm(Long id) {
        return systemAlarmMapper.findById(id);
    }

    @Override
    @Transactional
    public Long createSystemAlarm(SysAlarmEvent alarm) {
        systemAlarmMapper.insert(alarm);
        return alarm.getId();
    }

    @Override
    @Transactional
    public boolean updateSystemAlarm(SysAlarmEvent alarm) {
        return systemAlarmMapper.update(alarm) > 0;
    }

    @Override
    @Transactional
    public boolean deleteSystemAlarm(Long id) {
        return systemAlarmMapper.delete(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateSystemAlarmStatus(Long id, Integer alarmStatus) {
        return systemAlarmMapper.updateStatus(id, alarmStatus) > 0;
    }

    @Override
    public PageResult<DmAlarmEvent> pageDataAlarms(
            String taskId,
            String sourceName,
            String keyword,
            Integer alarmLevel,
            Integer alarmStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {
        validateDataAlarmQuery(
                taskId, alarmLevel, alarmStatus, startTime, endTime);
        int safePageNum = pageNum == null ? 1 : Math.max(1, pageNum);
        int safePageSize = pageSize == null ? 20
                : Math.min(100, Math.max(1, pageSize));

        // PageHelper 只会分页紧随其后的第一条 MyBatis 查询，二者之间不要插入其他查询。
        PageHelper.startPage(safePageNum, safePageSize);
        List<DmAlarmEvent> records = dataAlarmMapper.findAll(
                taskId,
                trimToNull(sourceName),
                trimToNull(keyword),
                alarmLevel,
                alarmStatus,
                startTime,
                endTime);
        PageInfo<DmAlarmEvent> pageInfo = new PageInfo<>(records);

        return new PageResult<DmAlarmEvent>(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                records);
    }

    @Override
    public List<String> listDataAlarmSources(String taskId) {
        if (taskId != null && taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        return dataAlarmMapper.findSourceNames(taskId);
    }

    @Override
    public DmAlarmEvent getDataAlarm(Long id) {
        return dataAlarmMapper.findById(id);
    }

    @Override
    @Transactional
    public Long createDataAlarm(DmAlarmEvent alarm) {
        dataAlarmMapper.insert(alarm);
        return alarm.getId();
    }

    @Override
    @Transactional
    public boolean updateDataAlarm(DmAlarmEvent alarm) {
        return dataAlarmMapper.update(alarm) > 0;
    }

    @Override
    @Transactional
    public boolean deleteDataAlarm(Long id) {
        return dataAlarmMapper.delete(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateDataAlarmStatus(Long id, Integer alarmStatus) {
        return dataAlarmMapper.updateStatus(id, alarmStatus) > 0;
    }

    /** 校验数据监测告警列表的组合查询条件。 */
    private void validateDataAlarmQuery(
            String taskId,
            Integer alarmLevel,
            Integer alarmStatus,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (taskId != null && taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        if (alarmLevel != null && (alarmLevel < 1 || alarmLevel > 5)) {
            throw new IllegalArgumentException("告警级别只能为1至5");
        }
        if (alarmStatus != null && alarmStatus != 0 && alarmStatus != 1) {
            throw new IllegalArgumentException("告警状态只能为0或1");
        }
        if (startTime != null && endTime != null
                && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    /** 将空白查询文本统一转换为null，避免生成无意义的SQL条件。 */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
