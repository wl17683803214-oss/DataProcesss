package com.example.dataprocess.service;

import com.example.common.enums.DataAlarmType;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.GreptimeTableRoute;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.common.telemetry.TelemetryRealtimeEvent;
import com.example.dataprocess.alarm.DataAlarmRecorder;
import com.example.dataprocess.config.DataProcessingThreadPoolConfig;
import com.example.dataprocess.config.GreptimeBulkProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.processing.PdxpTiming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 按任务和对象跨帧合并，刷新和重试均在独立后台线程执行。 */
public class GreptimeWriteBuffer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreptimeWriteBuffer.class);
    private final GreptimeStore store;
    private final GreptimeBulkProperties properties;
    private final DataAlarmRecorder alarmRecorder;
    private final ThreadPoolExecutor workers;
    /** 唤醒和额度维护只持有内存锁，组装大列表及网络写入均在锁外。 */
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();
    private final Map<List<String>, PendingGroup> groups = new LinkedHashMap<>();
    /** 额度直到确认成功或最终失败才释放，不能只计算等待区。 */
    private long reservedBytes;
    private boolean closing;
    /** 成功日志每五秒最多采样一次，避免按参数或每批高频打印。 */
    private final AtomicLong nextReportNanos = new AtomicLong();

    /** 只提交固定数量的常驻消费任务，不创建额外的待写执行队列。 */
    public GreptimeWriteBuffer(GreptimeStore store, GreptimeBulkProperties properties,
            DataAlarmRecorder alarmRecorder) {
        properties.validate();
        this.store = store;
        this.properties = properties;
        this.alarmRecorder = alarmRecorder;
        int count = properties.getMaxConcurrentWrites();
        workers = new ThreadPoolExecutor(count, count, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(count),
                DataProcessingThreadPoolConfig.namedThreadFactory("遥测合并入库线程-"),
                new ThreadPoolExecutor.AbortPolicy());
        for (int index = 0; index < count; index++) {
            workers.execute(this::consume);
        }
    }

    /** 只接收存储记录引用，成功返回表示进入缓冲，不代表已经持久化。 */
    public void offer(CollectInterfaceRuntimeConfig config, TelemetryRealtimeEvent event) {
        // 第一步：沿用分表身份校验，按同任务同对象合并，不按正常或异常状态拆组。
        TelemetryFrameRecord frame = event.getFrame();
        GreptimeTableRoute.of(frame.getSourceType(), frame.getSourceIdentity());
        List<String> key = Arrays.asList(frame.getTaskId(), frame.getSourceType(), frame.getSourceIdentity());
        long bytes = GreptimeRecordSizer.event(event);
        // 采样日志复用同一帧的估算，缓冲满而拒绝时仍能看到本帧大小。
        PdxpTiming.recordStorageEstimate(bytes);
        lock.lock();
        try {
            // 第二步：满额或关闭时立即拒绝，由原提交线程记录接口级失败告警。
            if (closing) {
                throw new RejectedExecutionException("遥测合并入库缓冲正在关闭");
            }
            if (bytes > properties.getMaxPendingBytes() - reservedBytes) {
                throw new RejectedExecutionException("遥测合并入库缓冲估算字节额度已满");
            }
            PendingGroup group = groups.get(key);
            if (group == null) {
                // 第三步：第一帧固定等待起点，后续新帧不会让计时重新开始。
                group = new PendingGroup(System.nanoTime());
                groups.put(key, group);
            }
            group.frames.add(new PendingFrame(config, event));
            group.parameters += event.getParameters().size();
            group.bytes += bytes;
            reservedBytes += bytes;
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** 后台线程只负责选择到期分组和写入，不占用采集及记录组装线程。 */
    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            PendingGroup group;
            try {
                group = take();
                if (group == null) {
                    return;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            long started = System.nanoTime();
            try {
                // 参数只复制列表引用，各帧记录和时间不重建，也不按代号去重。
                List<TelemetryFrameRecord> frames = new ArrayList<>(group.frames.size());
                List<TelemetryParameterRecord> parameters = new ArrayList<>(group.parameters);
                for (PendingFrame item : group.frames) {
                    frames.add(item.event.getFrame());
                    parameters.addAll(item.event.getParameters());
                }
                store.saveBatch(frames, parameters);
                report(group, started);
            } catch (RuntimeException exception) {
                // 公共存储已完成有限重试，最终失败在此保留真实对象和接口告警。
                failure(group, "遥测跨帧批量入库失败", exception);
            } finally {
                lock.lock();
                try {
                    reservedBytes -= group.bytes;
                    group.frames.clear();
                    changed.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /** 时间、行数、字节或正常关闭任一条件满足就取走整组，流内继续按行拆批。 */
    private PendingGroup take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (true) {
                long now = System.nanoTime();
                long waitNanos = TimeUnit.MILLISECONDS.toNanos(properties.getFlushIntervalMillis());
                Iterator<PendingGroup> iterator = groups.values().iterator();
                while (iterator.hasNext()) {
                    PendingGroup group = iterator.next();
                    long remaining = TimeUnit.MILLISECONDS.toNanos(properties.getFlushIntervalMillis())
                            - (now - group.firstQueuedAt);
                    if (closing || group.parameters >= properties.getMaxBatchRows()
                            || group.frames.size() >= properties.getMaxBatchRows()
                            || group.bytes >= properties.getMaxBatchBytes() || remaining <= 0L) {
                        iterator.remove();
                        return group;
                    }
                    waitNanos = Math.min(waitNanos, remaining);
                }
                if (closing && groups.isEmpty()) {
                    return null;
                }
                changed.awaitNanos(waitNanos);
            }
        } finally {
            lock.unlock();
        }
    }

    /** 分别输出缓冲等待和真实写入耗时，避免将提交缓冲误认为数据库完成。 */
    private void report(PendingGroup group, long started) {
        long now = System.nanoTime();
        long next = nextReportNanos.get();
        if (now < next || !nextReportNanos.compareAndSet(next, now + TimeUnit.SECONDS.toNanos(5))) {
            return;
        }
        TelemetryFrameRecord frame = group.frames.get(0).event.getFrame();
        String source = TelemetrySourceType.LOCAL.getValue().equals(frame.getSourceType())
                ? TelemetrySourceType.LOCAL.getLabel() : TelemetrySourceType.RPC.getLabel();
        LOGGER.info("遥测跨帧批量入库采样，任务：{}，来源：{}，对象：{}，整帧数量：{}，参数数量：{}，缓冲等待：{}毫秒，写入含重试：{}毫秒",
                frame.getTaskId(), source, frame.getSourceIdentity(), group.frames.size(),
                group.parameters, TimeUnit.NANOSECONDS.toMillis(started - group.firstQueuedAt),
                TimeUnit.NANOSECONDS.toMillis(now - started));
    }

    /** 每个采集接口合并一条存储失败告警，避免按每个参数重复记录。 */
    private void failure(PendingGroup group, String detail, RuntimeException exception) {
        TelemetryFrameRecord frame = group.frames.get(0).event.getFrame();
        LOGGER.error("{}，任务：{}，对象：{}，整帧数量：{}，参数数量：{}", detail,
                frame.getTaskId(), frame.getSourceIdentity(), group.frames.size(), group.parameters, exception);
        if (alarmRecorder == null) {
            return;
        }
        Map<Long, CollectInterfaceRuntimeConfig> interfaces = new LinkedHashMap<>();
        for (PendingFrame item : group.frames) {
            interfaces.putIfAbsent(item.config.getInterfaceId(), item.config);
        }
        for (CollectInterfaceRuntimeConfig config : interfaces.values()) {
            try {
                alarmRecorder.record(config, DataAlarmType.STORAGE_FAILURE, detail + "；对象："
                        + frame.getSourceIdentity() + "；合并整帧数：" + group.frames.size()
                        + "；合并参数数：" + group.parameters + "；原因：" + exception.getMessage());
            } catch (RuntimeException alarmException) {
                LOGGER.error("跨帧入库失败告警记录异常，接口编号：{}", config.getInterfaceId(), alarmException);
            }
        }
    }

    /** 上游入库组装线程池停止后立即刷新所有分组，再释放下游连接和流。 */
    @Override
    public void close() {
        lock.lock();
        try {
            if (closing) {
                return;
            }
            closing = true;
            changed.signalAll();
        } finally {
            lock.unlock();
        }
        workers.shutdown();
        boolean interrupted = false;
        try {
            if (workers.awaitTermination(properties.getShutdownWaitMillis(), TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (InterruptedException exception) {
            interrupted = true;
        }
        // 超时不伪造刷新成功；取消网络等待后，对尚未分发的组明确记错并释放额度。
        workers.shutdownNow();
        List<PendingGroup> cancelled;
        lock.lock();
        try {
            cancelled = new ArrayList<>(groups.values());
            groups.clear();
            for (PendingGroup group : cancelled) {
                reservedBytes -= group.bytes;
            }
        } finally {
            lock.unlock();
        }
        LOGGER.error("遥测合并入库关闭等待结束，未完成记录不会标记为已入库，尚未分发的分组数量：{}", cancelled.size());
        for (PendingGroup group : cancelled) {
            failure(group, "关闭等待超时，遥测合并记录未完成入库",
                    new IllegalStateException("遥测合并入库关闭等待结束"));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** 一帧只持有一份存储记录和接口上下文，后台组装不再复制原码。 */
    private static final class PendingFrame {
        private final CollectInterfaceRuntimeConfig config;
        private final TelemetryRealtimeEvent event;

        private PendingFrame(CollectInterfaceRuntimeConfig config, TelemetryRealtimeEvent event) {
            this.config = config;
            this.event = event;
        }
    }

    /** 已取走的组不再接收新帧，等待和重试期间仍计入总额度。 */
    private static final class PendingGroup {
        private final List<PendingFrame> frames = new ArrayList<>();
        private final long firstQueuedAt;
        private int parameters;
        private long bytes;

        private PendingGroup(long firstQueuedAt) {
            this.firstQueuedAt = firstQueuedAt;
        }
    }
}
