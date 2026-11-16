package com.example.dataadmin.scheduler;

import com.example.common.enums.CollectionRateUnit;
import com.example.dataadmin.entity.CollectInterfaceConfig;
import com.example.dataadmin.mapper.CollectInterfaceConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 每秒复用接口采集速度缓存，向配置主题发送一条全量JSON快照。 */
@Component
public class InterfaceSpeedReportScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterfaceSpeedReportScheduler.class);
    /** 与处理服务现有采集速度缓存保持一致，不读取遥测参数及整帧数据。 */
    private static final String CACHE_PREFIX = "dashboard:collection:current:";
    /** 名称和任务归属每三十秒刷新，速度仍每秒读取最新缓存。 */
    private static final long METADATA_REFRESH_NANOS = TimeUnit.SECONDS.toNanos(30);
    /** 缓存字段独立检查五秒时效，避免其他接口刷新整键有效期后保留旧速度。 */
    private static final long MAX_SAMPLE_AGE_MILLIS = 5000L;
    private final CollectInterfaceConfigMapper interfaceMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RocketMQTemplate> senderProvider;
    private final Environment environment;
    /** 独立串行线程，不与管理服务的清理、采样及心跳定时任务争用调度线程。 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(action -> {
        Thread thread = new Thread(action, "接口速度上报线程");
        thread.setDaemon(true);
        return thread;
    });
    /** 以下状态仅由串行上报任务访问，接口元数据整批成功后才替换。 */
    private List<CollectInterfaceConfig> interfaces;
    private long nextMetadataRefreshNanos;
    private long lastReportSecond = Long.MIN_VALUE;
    private long lastWarningNanos;
    private String lastSuccessTopic;

    /** 复用已有配置、数据库映射、Redis、JSON及消息模板，不额外创建生产者。 */
    public InterfaceSpeedReportScheduler(CollectInterfaceConfigMapper interfaceMapper,
            StringRedisTemplate redis, ObjectMapper objectMapper,
            ObjectProvider<RocketMQTemplate> senderProvider, Environment environment) {
        this.interfaceMapper = interfaceMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.senderProvider = senderProvider;
        this.environment = environment;
    }

    /** 容器完成依赖注入后注册一秒节拍，关闭开关时不查询接口或缓存。 */
    @PostConstruct
    public void start() {
        scheduler.scheduleAtFixedRate(this::report, 1L, 1L, TimeUnit.SECONDS);
    }

    /** 本轮只发送当前快照，延迟或失败后不排队补发已经过时的秒级速度。 */
    public synchronized void report() {
        String topic = "";
        try {
            if (!environment.getProperty("interface-speed-report.enabled", Boolean.class, false)) {
                lastSuccessTopic = null;
                return;
            }
            // 第一步：同一秒最多采集一次，慢请求结束后的补调度不发送过时消息。
            OffsetDateTime sampledAt = OffsetDateTime.now();
            long sampleSecond = sampledAt.toEpochSecond();
            if (sampleSecond == lastReportSecond) { return; }
            lastReportSecond = sampleSecond;
            topic = environment.getProperty("interface-speed-report.topic", "").trim();
            if (topic.isEmpty() || topic.contains(":")) {
                throw new IllegalArgumentException("接口速度上报主题不能为空，且应仅填写主题名");
            }
            String systemId = environment.getProperty("interface-speed-report.system-id", "sjcl").trim();
            if (systemId.isEmpty()) { throw new IllegalArgumentException("接口速度上报系统标识不能为空"); }
            // 第二步：复用已有映射查询未删除接口，名称及任务归属在内存中定期刷新。
            List<CollectInterfaceConfig> configured = interfaceSnapshot();
            Map<String, Map<Object, Object>> taskMetrics = new LinkedHashMap<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            double totalRateKbps = 0D;
            for (CollectInterfaceConfig config : configured) {
                if (config.getId() == null || config.getTaskId() == null || config.getTaskId().trim().isEmpty()) {
                    continue;
                }
                // 第三步：同一任务只读一次Hash，不逐个接口往返Redis，不修改原有缓存。
                String taskId = config.getTaskId();
                Map<Object, Object> metrics = taskMetrics.get(taskId);
                if (metrics == null) {
                    metrics = redis.opsForHash().entries(CACHE_PREFIX + taskId);
                    taskMetrics.put(taskId, metrics);
                }
                // 读取结束后再判断时效，避免把本轮开始后刚更新的缓存误判为未来数据。
                double rateKbps = cachedRate(metrics.get("interface:" + config.getId()), LocalDateTime.now());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("taskId", taskId);
                row.put("interfaceName", config.getInterfaceName());
                row.put("receiveRate", formatRate(rateKbps));
                rows.add(row);
                totalRateKbps += rateKbps;
            }
            // 第四步：原始千字节速率先相加再换算，不能相加已格式化或修约的字符串。
            Map<String, Object> total = new LinkedHashMap<>();
            total.put("interfaceCount", rows.size());
            total.put("receiveRate", formatRate(totalRateKbps));
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("systemId", systemId);
            message.put("sampleTime", sampledAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            message.put("total", total);
            message.put("interfaces", rows);
            // 第五步：发送原始JSON文本，复用管理服务已有的名称服务、生产者及认证配置。
            RocketMQTemplate sender = senderProvider.getIfAvailable();
            if (sender == null) {
                throw new IllegalStateException("管理服务未配置消息生产者，请补充rocketmq.producer.group");
            }
            String json = objectMapper.writeValueAsString(message);
            SendResult result = sender.syncSend(topic, json);
            if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                throw new IllegalStateException("接口速度消息未获得成功发送确认");
            }
            if (!topic.equals(lastSuccessTopic)) {
                LOGGER.info("接口速度MQ上报成功，主题：{}，接口数量：{}，总采集速度：{}",
                        topic, rows.size(), total.get("receiveRate"));
                lastSuccessTopic = topic;
            }
        } catch (Exception exception) {
            // 单次失败不退出周期任务；连续失败最多每三十秒输出一次，避免每秒刷屏。
            warn("接口速度MQ上报失败，本轮快照已跳过，主题：" + topic, exception);
        }
    }

    /** 首次查询失败时跳过上报；后续刷新失败时沿用已有元数据，并延后重试。 */
    private List<CollectInterfaceConfig> interfaceSnapshot() {
        long now = System.nanoTime();
        if (interfaces != null && now - nextMetadataRefreshNanos < 0L) { return interfaces; }
        try {
            List<CollectInterfaceConfig> loaded = interfaceMapper.findAllByTaskId(null);
            interfaces = loaded == null ? Collections.emptyList() : new ArrayList<>(loaded);
        } catch (RuntimeException exception) {
            if (interfaces == null) { throw exception; }
            warn("接口速度上报刷新接口名称和任务归属失败，暂用已有元数据", exception);
        }
        nextMetadataRefreshNanos = now + METADATA_REFRESH_NANOS;
        return interfaces;
    }

    /** 单字段缺失、过期或格式异常按零展示，不让旧接口速度被整键续期后继续累计。 */
    private double cachedRate(Object value, LocalDateTime now) {
        if (value == null) { return 0D; }
        try {
            JsonNode metric = objectMapper.readTree(String.valueOf(value));
            LocalDateTime sampleTime = LocalDateTime.parse(metric.path("sampleTime").asText());
            long age = Duration.between(sampleTime, now).toMillis();
            if (age < 0L || age > MAX_SAMPLE_AGE_MILLIS) { return 0D; }
            JsonNode rate = metric.path("interfaceRateKbps");
            double number = rate.isNumber() ? rate.asDouble() : Double.NaN;
            return Double.isFinite(number) && number >= 0D ? number : 0D;
        } catch (Exception ignored) {
            return 0D;
        }
    }

    /** 复用页面的单位枚举和修约格式，返回包含KB/s或MB/s的字符串。 */
    private String formatRate(double rateKbps) {
        return CollectionRateUnit.fromKilobytes(rateKbps).format(rateKbps);
    }

    /** 使用单调时钟限制异常日志频率，保留原始异常便于定位连接与路由问题。 */
    private void warn(String detail, Exception exception) {
        long now = System.nanoTime();
        if (lastWarningNanos == 0L || now - lastWarningNanos >= TimeUnit.SECONDS.toNanos(30)) {
            lastWarningNanos = now;
            LOGGER.warn(detail, exception);
        }
    }

    /** 关闭时取消等待中的上报，不销毁与其他功能共用的消息模板。 */
    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
                LOGGER.warn("接口速度上报线程关闭等待超时");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
