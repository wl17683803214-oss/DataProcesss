package com.example.dataprocess.service;

import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.telemetry.TelemetryRealtimeBatch;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.dataprocess.config.DataProcessingThreadPoolConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 缓存线程组装有限批次，独立发送线程转发给管理服务。 */
public class TelemetryRealtimePublisher implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRealtimePublisher.class);
    private static final byte[] GAP = "{\"gap\":true}".getBytes(StandardCharsets.UTF_8);
    private final ObjectMapper mapper = new ObjectMapper();
    private final StringRedisTemplate redis;
    private final byte[] channel;
    private final ThreadPoolExecutor executor;
    private final AtomicBoolean gap = new AtomicBoolean();
    private long nextLog;

    public TelemetryRealtimePublisher(StringRedisTemplate redis, TelemetryRealtimeProperties properties) {
        // 每个队列元素最多保留一份有限大小的序列化数据，拒绝时登记推送中断。
        this.redis = redis;
        channel = (properties.getKeyPrefix() + TelemetryRealtimeBatch.CHANNEL_SUFFIX).getBytes(StandardCharsets.UTF_8);
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getPushQueueCapacity()),
                DataProcessingThreadPoolConfig.namedThreadFactory("遥测实时发送线程-"),
                (task, pool) -> markGap());
    }

    /** 超限或转发失败后，在下次可用时通知前端重新同步。 */
    public void markGap() {
        gap.set(true);
    }

    /** 保持帧与点各自的到达顺序，避免创建全量参数点的序列化对象。 */
    public void offer(Map<TelemetryFrameRecord, List<TelemetryParameterRecord>> events)
            throws JsonProcessingException {
        // 第一步：每批整帧数固定上限，超出字节限制时再递归拆分。
        List<TelemetryFrameRecord> frames = new ArrayList<>();
        for (TelemetryFrameRecord frame : events.keySet()) {
            if (frame.getExpiresAt() > 0) {
                frames.add(frame);
            }
        }
        for (int offset = 0; offset < frames.size(); offset += 16) {
            encode(frames.subList(offset, Math.min(offset + 16, frames.size())), TelemetryRealtimeBatch::setFrames);
        }
        // 第二步：按到达顺序积累参数点，表格的合并由管理服务在订阅范围内完成。
        List<TelemetryParameterRecord> points = new ArrayList<>(200);
        for (List<TelemetryParameterRecord> rows : events.values()) {
            for (TelemetryParameterRecord row : rows) {
                points.add(row);
                if (points.size() == 200) {
                    encode(points, TelemetryRealtimeBatch::setParameters);
                    points.clear();
                }
            }
        }
        if (!points.isEmpty()) {
            encode(points, TelemetryRealtimeBatch::setParameters);
        }
    }

    /** 控制每次发布的字节数，单条超限时标记中断并继续后续数据。 */
    private <T> void encode(List<T> rows, BiConsumer<TelemetryRealtimeBatch, List<T>> setter)
            throws JsonProcessingException {
        TelemetryRealtimeBatch batch = new TelemetryRealtimeBatch();
        setter.accept(batch, rows);
        byte[] payload = mapper.writeValueAsBytes(batch);
        if (payload.length <= TelemetryRealtimeBatch.MAX_BYTES) {
            executor.execute(() -> send(payload));
        } else if (rows.size() > 1) {
            int middle = rows.size() / 2;
            encode(rows.subList(0, middle), setter);
            encode(rows.subList(middle, rows.size()), setter);
        } else {
            markGap();
        }
    }

    /** 同一实例按顺序发布，发送失败只影响实时展示。 */
    private void send(byte[] payload) {
        try {
            if (gap.getAndSet(false)) {
                publish(GAP);
            }
            publish(payload);
        } catch (RuntimeException exception) {
            markGap();
            long now = System.nanoTime();
            if (now >= nextLog) {
                LOGGER.warn("遥测实时推送失败，下次可用时通知页面重新同步", exception);
                nextLog = now + TimeUnit.SECONDS.toNanos(10);
            }
        }
    }

    private void publish(byte[] payload) {
        redis.execute((RedisCallback<Long>) connection -> connection.publish(channel, payload));
    }

    @Override
    public void close() {
        // 实时增量不参与服务关闭时的持久化等待。
        executor.shutdownNow();
    }
}
