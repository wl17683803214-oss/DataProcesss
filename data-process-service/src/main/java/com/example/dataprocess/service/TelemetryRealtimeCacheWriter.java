package com.example.dataprocess.service;

import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.telemetry.TelemetryRealtimeCache;
import com.example.common.telemetry.TelemetryRealtimeEvent;
import java.util.function.Supplier;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.dataprocess.config.DataProcessingThreadPoolConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 只接受不等待的快照提交，Redis操作、合并和序列化全部在独立线程执行。 */
public class TelemetryRealtimeCacheWriter implements AutoCloseable {
    /** 仅由后台线程输出缓存失败和丢弃统计，避免热路径频繁写日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRealtimeCacheWriter.class);
    /** 公共缓存读写实现。 */
    private final TelemetryRealtimeCache cache;
    /** 快照写入后提交实时增量的独立发送队列。 */
    private final TelemetryRealtimePublisher publisher;
    /** 有界队列与刷新配置。 */
    private final TelemetryRealtimeProperties properties;
    /** 提交解析结果的引用，缓存记录由后台线程组装。 */
    private final ArrayBlockingQueue<Snapshot> queue;
    /** 参数点数量包含排队和正在写入的批次。 */
    private final AtomicInteger pendingParameters = new AtomicInteger();
    /** 完整帧字节数包含排队和正在写入的批次。 */
    private final AtomicLong pendingFrameBytes = new AtomicLong();
    /** 队列满、预算不足或缓存故障时跳过的批次数量。 */
    private final AtomicLong skipped = new AtomicLong();
    /** 缓存故障后的短暂熔断，避免持续浪费序列化和连接资源。 */
    private volatile long retryAfterNanos;
    /** 关闭后不再接受新缓存任务。 */
    private volatile boolean closed;
    /** 独立守护线程，不复用采集、解析、入库或消息线程池。 */
    private final ScheduledExecutorService executor;
    /** 后台日志按十秒汇总一次。 */
    private long nextReportNanos;

    /** 创建有界缓存工作线程，不在启动时访问Redis。 */
    public TelemetryRealtimeCacheWriter(TelemetryRealtimeCache cache, TelemetryRealtimeProperties properties,
            TelemetryRealtimePublisher publisher) {
        properties.validate();
        this.cache = cache;
        this.publisher = publisher;
        this.properties = properties;
        queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
        executor = Executors.newSingleThreadScheduledExecutor(
                DataProcessingThreadPoolConfig.namedThreadFactory("遥测实时缓存线程-"));
        if (properties.isEnabled()) {
            executor.scheduleWithFixedDelay(this::flush, properties.getFlushIntervalMillis(),
                    properties.getFlushIntervalMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** 解析完成后调用；容量不足立即放弃本次缓存，不等待、不回退到调用线程。 */
    public void offer(int points, int frameBytes, Supplier<TelemetryRealtimeEvent> preparation) {
        // 第一步：关闭、禁用和故障熔断时只更新计数，不接触Redis。
        if (closed || !properties.isEnabled()) {
            return;
        }
        if (System.nanoTime() < retryAfterNanos) {
            skipped.incrementAndGet();
            publisher.markGap();
            return;
        }
        // 第二步：预占参数和整帧预算，避免少量大消息绕过队列条数上限。
        if (pendingParameters.addAndGet(points) > properties.getMaxPendingParameters()) {
            pendingParameters.addAndGet(-points);
            skipped.incrementAndGet();
            publisher.markGap();
            return;
        }
        if (pendingFrameBytes.addAndGet(frameBytes) > properties.getMaxPendingFrameBytes()) {
            pendingFrameBytes.addAndGet(-frameBytes);
            pendingParameters.addAndGet(-points);
            skipped.incrementAndGet();
            publisher.markGap();
            return;
        }
        // 第三步：只尝试一次立即提交，队列满时释放预算并返回。
        if (!queue.offer(new Snapshot(points, frameBytes, preparation))) {
            pendingFrameBytes.addAndGet(-frameBytes);
            pendingParameters.addAndGet(-points);
            skipped.incrementAndGet();
            publisher.markGap();
        }
    }

    /** 一轮只获取当前有界队列中的数据，后续新数据留到下一轮。 */
    private void flush() {
        List<Snapshot> snapshots = new ArrayList<>();
        queue.drainTo(snapshots);
        try {
            // 第一步：使用对象引用组成批次，公共组件合并重复参数并批量写入。
            if (!snapshots.isEmpty()) {
                Map<TelemetryFrameRecord, List<TelemetryParameterRecord>> batches = new LinkedHashMap<>();
                int points = 0;
                for (Snapshot snapshot : snapshots) {
                    if (!batches.isEmpty() && points + snapshot.points > 20000) {
                        writeAndPublish(batches);
                        batches.clear();
                        points = 0;
                    }
                    try {
                        // 单帧组装异常独立处理，后续帧仍进入推送批次。
                        TelemetryRealtimeEvent event = snapshot.preparation.get();
                        event.getFrame().setRealtimeId(java.util.UUID.randomUUID().toString());
                        batches.put(event.getFrame(), event.getParameters());
                        points += snapshot.points;
                    } catch (RuntimeException exception) {
                        skipped.incrementAndGet();
                        publisher.markGap();
                    }
                }
                writeAndPublish(batches);
            }
        } catch (Exception exception) {
            // 第二步：缓存失败不重试整批、不重新入库；短暂熔断只影响实时快照。
            skipped.addAndGet(snapshots.size());
            publisher.markGap();
            retryAfterNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            if (System.nanoTime() >= nextReportNanos) {
                LOGGER.warn("遥测实时缓存写入失败，已跳过当前快照，入库结果不受影响", exception);
            }
        } finally {
            // 第三步：无论写入是否成功，都释放已完成批次的预算。
            for (Snapshot snapshot : snapshots) {
                pendingParameters.addAndGet(-snapshot.points);
                pendingFrameBytes.addAndGet(-snapshot.frameBytes);
            }
            if (System.nanoTime() >= nextReportNanos) {
                long count = skipped.getAndSet(0);
                if (count > 0) {
                    LOGGER.warn("遥测实时缓存因容量限制或访问失败跳过快照，批次数量：{}", count);
                }
                nextReportNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            }
        }
    }

    /** 先写快照，再提交同批增量，订阅初始化可用帧标识消除重叠。 */
    private void writeAndPublish(Map<TelemetryFrameRecord, List<TelemetryParameterRecord>> batches)
            throws java.io.IOException {
        if (!batches.isEmpty()) {
            cache.writeSnapshots(batches);
            publisher.offer(batches);
        }
    }

    /** 服务关闭时取消实时缓存任务，不延长入库线程的关闭等待。 */
    @Override
    public void close() {
        // 实时快照允许丢弃，持久化数据继续由原有存储组件负责。
        closed = true;
        executor.shutdownNow();
        queue.clear();
    }

    /** 一条解析结果的后台组装任务及已预占的额度。 */
    private static final class Snapshot {
        private final int points;
        private final Supplier<TelemetryRealtimeEvent> preparation;
        private final int frameBytes;

        private Snapshot(int points, int frameBytes, Supplier<TelemetryRealtimeEvent> preparation) {
            this.points = points;
            this.preparation = preparation;
            this.frameBytes = frameBytes;
        }
    }
}
