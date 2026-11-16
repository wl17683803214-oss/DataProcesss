package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.common.enums.CollectionDataType;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetrySourceType;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.common.telemetry.TelemetrySelectionSnapshot;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 只在后台读取 Redis，处理线程使用不可变筛选快照。 */
@Component
public class TelemetrySelectionCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetrySelectionCache.class);
    private final StringRedisTemplate redis;
    private final TelemetryRealtimeProperties properties;
    private final ObjectMapper json = new ObjectMapper();
    private volatile Map<String, Set<String>> objects = Collections.emptyMap();
    private String loaded;
    private long nextWarning;

    public TelemetrySelectionCache(StringRedisTemplate redis, TelemetryRealtimeProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** 原子切换完整版本，首次同步前或无勾选时返回空集合。 */
    @Scheduled(fixedDelay = 1000)
    public void refresh() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String value = redis.opsForValue().get(TelemetrySelectionSnapshot.redisKey(properties));
            if (value == null) {
                objects = Collections.emptyMap();
                loaded = null;
            } else if (!value.equals(loaded)) {
                TelemetrySelectionSnapshot snapshot = json.readValue(value, TelemetrySelectionSnapshot.class);
                Map<String, Set<String>> replacement = new HashMap<>();
                for (Map.Entry<String, Set<String>> entry : snapshot.getObjects().entrySet()) {
                    replacement.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
                }
                objects = Collections.unmodifiableMap(replacement);
                loaded = value;
            }
        } catch (Exception exception) {
            // 缓存访问失败时沿用上一版本，不在逐帧处理路径访问外部服务。
            if (System.nanoTime() >= nextWarning) {
                nextWarning = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                LOGGER.warn("实时参数筛选缓存读取失败，沿用上一版本", exception);
            }
        }
    }

    /** 与持久化身份一致，本地按主键匹配，远程按返回的卫星代号匹配。 */
    public Set<String> selected(CollectInterfaceRuntimeConfig config, TelemetryMessage message) {
        boolean remote = CollectionDataType.require(config.getRpcEnabled()) == CollectionDataType.SATELLITE;
        // 无法确定真实对象时不缓存参数，异常帧仍交给原有后台校验。
        if ((remote && message.getSatCode().trim().isEmpty())
                || (!remote && (config.getDeviceSatelliteId() == null || config.getDeviceSatelliteId() <= 0))) {
            return Collections.emptySet();
        }
        String source = remote ? TelemetrySourceType.RPC.getValue() : TelemetrySourceType.LOCAL.getValue();
        String identity = remote ? TelemetryIdentity.rpc(message.getSatCode())
                : TelemetryIdentity.local(config.getDeviceSatelliteId());
        return objects.getOrDefault(TelemetrySelectionSnapshot.objectKey(config.getTaskId(), source, identity),
                Collections.emptySet());
    }

    /** 排队期间取消的参数在写缓存前再次校验，避免旧任务恢复已取消的数据。 */
    public boolean contains(com.example.common.greptime.TelemetryParameterRecord row) {
        return objects.getOrDefault(TelemetrySelectionSnapshot.objectKey(row.getTaskId(), row.getSourceType(),
                row.getSourceIdentity()), Collections.emptySet()).contains(row.getTelemetryCode());
    }
}
