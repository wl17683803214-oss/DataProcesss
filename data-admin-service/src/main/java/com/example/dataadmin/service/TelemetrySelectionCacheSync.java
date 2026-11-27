package com.example.dataadmin.service;

import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetrySourceType;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.common.telemetry.TelemetrySelectionSnapshot;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.mapper.ProcessedTelemetryFilterSelectionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 后台同步数据库已提交的筛选并集，覆盖单项勾选、清空、导入和组件删除。 */
@Component
public class TelemetrySelectionCacheSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetrySelectionCacheSync.class);
    private final ProcessedTelemetryFilterSelectionMapper mapper;
    private final StringRedisTemplate redis;
    private final TelemetryRealtimeProperties properties;
    private final ObjectMapper json = new ObjectMapper();
    private long nextWarning;

    public TelemetrySelectionCacheSync(ProcessedTelemetryFilterSelectionMapper mapper,
            StringRedisTemplate redis, TelemetryRealtimeProperties properties) {
        this.mapper = mapper;
        this.redis = redis;
        this.properties = properties;
    }

    /** 每秒重建一次完整版本，仅有变化时原子替换 Redis 值。 */
    @Scheduled(fixedDelay = 1000)
    public void synchronize() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            Map<String, Set<String>> objects = new TreeMap<>();
            for (ProcessedTelemetryFilterSelection row : mapper.findRealtimeUnion()) {
                String source;
                String identity;
                if (DeviceSatelliteType.SATELLITE.getValue().equals(row.getDeviceSatelliteType())) {
                    source = TelemetrySourceType.RPC.getValue();
                    identity = TelemetryIdentity.rpc(row.getDeviceSatelliteCode());
                } else if (DeviceSatelliteType.DEVICE.getValue().equals(row.getDeviceSatelliteType())) {
                    source = TelemetrySourceType.LOCAL.getValue();
                    identity = TelemetryIdentity.local(row.getDeviceSatelliteId());
                } else {
                    throw new IllegalStateException("已勾选参数的设备卫星类型不正确");
                }
                String key = TelemetrySelectionSnapshot.objectKey(source, identity);
                objects.computeIfAbsent(key, ignored -> new TreeSet<>()).add(row.getTelemetryCode());
            }
            TelemetrySelectionSnapshot snapshot = new TelemetrySelectionSnapshot();
            snapshot.setObjects(objects);
            String key = TelemetrySelectionSnapshot.redisKey(properties);
            String value = json.writeValueAsString(snapshot);
            if (!value.equals(redis.opsForValue().get(key))) {
                redis.opsForValue().set(key, value);
            }
        } catch (Exception exception) {
            // 查询或缓存故障时保留上一版本，后台下一轮继续核对。
            if (System.nanoTime() >= nextWarning) {
                nextWarning = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                LOGGER.warn("实时参数筛选缓存同步失败，稍后重试", exception);
            }
        }
    }
}
