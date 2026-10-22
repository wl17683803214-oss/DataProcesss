package com.example.dataadmin.service.monitor.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.Version.SubTopicName;
import com.example.dataadmin.config.SystemMonitorProperties;
import com.example.dataadmin.entity.SysMonitorSnapshot;
import com.example.dataadmin.enums.ExperimentTaskStatus;
import com.example.dataadmin.mapper.ExperimentTaskMapper;
import com.example.dataadmin.service.monitor.SystemHealthReportService;
import com.example.dataadmin.service.monitor.support.SystemHealthTelemetryAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/** 从缓存读取健康快照，以原始消息字节向运行中的任务分别上报。 */
@Service
public class SystemHealthReportServiceImpl implements SystemHealthReportService {
    /** 中文上报日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemHealthReportServiceImpl.class);
    /** 现有健康快照缓存键。 */
    public static final String SNAPSHOT_KEY = "system:monitor:current";
    /** 上报周期、主题和快照时效设置。 */
    private final SystemMonitorProperties properties;
    /** 读取现有快照缓存。 */
    private final StringRedisTemplate redis;
    /** 复用应用的日期与时间序列化配置。 */
    private final ObjectMapper objectMapper;
    /** 只查询运行中的任务编号。 */
    private final ExperimentTaskMapper taskMapper;
    /** 独立的消息组装组件。 */
    private final SystemHealthTelemetryAssembler assembler;
    /** 延迟取得发送模板，关闭上报时无需创建发送客户端。 */
    private final ObjectProvider<RocketMQTemplate> senderProvider;
    /** 独立的串行调度器。 */
    private final TaskScheduler scheduler;
    /** 启动时注册的定时任务。 */
    private ScheduledFuture<?> scheduledTask;
    /** 连续失败日志的最近输出时间。 */
    private long lastWarningMillis;
    /** 本次启动是否已记录首次成功发送。 */
    private boolean successLogged;

    /** 注入现有缓存、任务查询和消息发送组件。 */
    public SystemHealthReportServiceImpl(SystemMonitorProperties properties, StringRedisTemplate redis,
            ObjectMapper objectMapper, ExperimentTaskMapper taskMapper,
            SystemHealthTelemetryAssembler assembler, ObjectProvider<RocketMQTemplate> senderProvider,
            @Qualifier("systemHealthReportScheduler") TaskScheduler scheduler) {
        // 保存依赖，定时任务仅在容器完成注入后注册。
        this.properties = properties;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.taskMapper = taskMapper;
        this.assembler = assembler;
        this.senderProvider = senderProvider;
        this.scheduler = scheduler;
    }

    /** 启用后校验固定配置，再启动独立调度。 */
    @PostConstruct
    public void start() {
        // 关闭上报不影响健康采样、页面推送和历史保存。
        SystemMonitorProperties.MqReport report = properties.getMqReport();
        if (!report.isEnabled()) {
            return;
        }
        // 错误的协议身份在启动时即明确报告，避免持续发送无效消息。
        if (blank(report.getTopic()) || blank(report.getBusinessId())) {
            throw new IllegalArgumentException("健康上报主题和参数表业务标识不能为空");
        }
        business();
        if (senderProvider.getIfAvailable() == null) {
            throw new IllegalArgumentException("健康上报已启用，但未配置消息生产者");
        }
        // 初次上报等待一个周期，后续每轮完成后再等待配置的间隔。
        scheduledTask = scheduler.scheduleWithFixedDelay(this::reportLatestSnapshot,
                new Date(System.currentTimeMillis() + report.getIntervalMillis()), report.getIntervalMillis());
    }

    /** 每轮只读取一次快照，每个任务独立组装并发送。 */
    @Override
    public synchronized void reportLatestSnapshot() {
        // 第一步：关闭后立即返回，发送过程不参与健康告警数据库事务。
        SystemMonitorProperties.MqReport report = properties.getMqReport();
        if (!report.isEnabled()) {
            return;
        }
        try {
            // 第二步：没有运行任务时不读取缓存，也不发送无任务归属的消息。
            List<String> taskIds = taskMapper.findTaskIdsByStatus(ExperimentTaskStatus.RUNNING.getCode());
            if (taskIds.isEmpty()) {
                return;
            }
            String json = redis.opsForValue().get(SNAPSHOT_KEY);
            if (blank(json)) {
                throw new IllegalStateException("Redis中没有健康快照，本轮跳过上报");
            }
            SysMonitorSnapshot snapshot = objectMapper.readValue(json, SysMonitorSnapshot.class);
            validateAge(snapshot, report.getMaxSnapshotAgeMillis());
            String destination = blank(report.getTag()) ? report.getTopic()
                    : report.getTopic() + ":" + report.getTag().trim();
            RocketMQTemplate sender = senderProvider.getObject();
            // 第三步：某个任务发送失败时继续其他任务，下一轮再上报最新快照。
            for (String taskId : taskIds) {
                try {
                    // 慢发送期间快照可能过期，每个任务发送前重新检查。
                    validateAge(snapshot, report.getMaxSnapshotAgeMillis());
                    TelemetryMessage message = assembler.assemble(snapshot, taskId, business(), report.getBusinessId());
                    SendResult result = sender.syncSend(destination, message.toByteArray());
                    if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                        throw new IllegalStateException("健康消息未获得发送成功确认");
                    }
                    if (!successLogged) {
                        LOGGER.info("健康消息首次上报成功，主题：{}，任务编号：{}，参数数量：{}",
                                report.getTopic(), taskId, message.getTelemetriesCount());
                        successLogged = true;
                    }
                } catch (Exception exception) {
                    warn("健康消息上报失败，任务编号：" + taskId, exception);
                }
            }
        } catch (Exception exception) {
            // 缓存、任务查询和采样异常均不能终止后续调度。
            warn("健康消息本轮未上报", exception);
        }
    }

    /** 以采样时间检查时效，不能以缓存键的存活时间代替。 */
    private void validateAge(SysMonitorSnapshot snapshot, long maximumAge) {
        // 同时拒绝缺失时间、未来时间和过期快照。
        if (snapshot == null || snapshot.getSampleTime() == null) {
            throw new IllegalArgumentException("健康快照缺少采样时间");
        }
        long age = Duration.between(snapshot.getSampleTime(),
                LocalDateTime.now(ZoneId.of("Asia/Shanghai"))).toMillis();
        if (age < 0 || age > maximumAge) {
            throw new IllegalArgumentException("健康快照已过期或采样时间异常");
        }
    }

    /** 使用已有协议枚举，不创建或修改生成类中的协议值。 */
    private SubTopicName business() {
        // 对未知枚举或未指定枚举拒绝启动上报。
        try {
            SubTopicName value = SubTopicName.valueOf(properties.getMqReport().getBusiness().trim());
            if (value == SubTopicName.UNRECOGNIZED || value == SubTopicName.SUBTOPIC_UNSPECIFIED_TYPE) {
                throw new IllegalArgumentException("未指定健康消息业务枚举");
            }
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("健康消息业务枚举配置无效", exception);
        }
    }

    /** 连续异常最多每三十秒输出一次，避免每秒重复打印日志。 */
    private void warn(String message, Exception exception) {
        // 上报故障持续存在时限制日志量，首次故障仍立即记录。
        long now = System.currentTimeMillis();
        if (now - lastWarningMillis >= 30000) {
            LOGGER.warn(message + "，原因：{}", exception.getMessage());
            lastWarningMillis = now;
        }
    }

    /** 应用停止时取消后续上报，由容器释放调度器线程。 */
    @PreDestroy
    public void stop() {
        // 不再注册新一轮发送，已经发送的消息无需回滚。
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
    }

    /** 判定配置或缓存文本是否为空。 */
    private boolean blank(String value) {
        // 纯空格与未配置保持一致。
        return value == null || value.trim().isEmpty();
    }
}
