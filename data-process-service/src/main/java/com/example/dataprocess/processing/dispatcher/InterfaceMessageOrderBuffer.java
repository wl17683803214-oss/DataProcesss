package com.example.dataprocess.processing.dispatcher;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.common.enums.DataAlarmType;
import com.example.dataprocess.alarm.DataAlarmRecorder;
import com.example.dataprocess.config.DataProcessingThreadPoolConfig;
import com.example.dataprocess.config.ResultDispatchProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.service.TelemetryMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/** 多线程无锁投递，每个接口的周期任务独占排序索引并串行发送消息。 */
@Component
public class InterfaceMessageOrderBuffer {

    /** 重排、容量和关闭过程统一使用中文日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(InterfaceMessageOrderBuffer.class);
    /** PDXP包序号使用无符号三十二位整数。 */
    private static final long SEQUENCE_MASK = 0xFFFFFFFFL;
    /** 在相邻半个序号空间内识别回绕，避免零排在最大序号前面。 */
    private static final long SEQUENCE_HALF = 0x80000000L;
    /** 每次回绕递增的完整序号空间。 */
    private static final long SEQUENCE_RANGE = 0x100000000L;
    /** 缓冲对象及索引节点的保守估算开销，消息正文按序列化长度计入。 */
    private static final long RECORD_OVERHEAD_BYTES = 256L;

    /** 沿用结果线程池配置，并绑定连续重排参数。 */
    private final ResultDispatchProperties properties;
    /** 启动时固定发送开关，开关不改变消息的投递和排序。 */
    private final boolean sendEnabled;
    /** 批量模式在服务启动时确定，复用原有接口队列和消息发送组件。 */
    private final boolean batchEnabled;
    private final TelemetryMessageService messageService;
    /** 复用现有告警分类，不新增对外枚举。 */
    private final DataAlarmRecorder alarmRecorder;
    /** 所有接口共享调度线程，但每个接口的同一个周期任务不会重叠执行。 */
    private final ScheduledThreadPoolExecutor scheduler;
    /** 只按采集接口主键分组，不按信源拆分队列。 */
    private final ConcurrentMap<Long, InterfaceQueue> interfaces = new ConcurrentHashMap<>();
    /** 使用原子额度限制无锁队列，不调用线性复杂度的队列长度统计。 */
    private final AtomicInteger pendingCount = new AtomicInteger();
    private final AtomicLong pendingBytes = new AtomicLong();
    /** 关闭时等待已进入投递过程的生产线程完成发布。 */
    private final AtomicInteger activeOffers = new AtomicInteger();
    private final AtomicBoolean closing = new AtomicBoolean();

    /** 创建固定数量的共享线程，接口首次收到消息时才登记周期任务。 */
    public InterfaceMessageOrderBuffer(ResultDispatchProperties properties, DataAlarmRecorder alarmRecorder,
            TelemetryMessageService messageService) {
        this.properties = properties;
        this.sendEnabled = properties.getMessageOrder().isSendEnabled();
        this.batchEnabled = properties.getMessageOrder().isBatchEnabled();
        this.alarmRecorder = alarmRecorder;
        this.messageService = messageService;
        this.scheduler = new ScheduledThreadPoolExecutor(properties.getMessageThreadCount(),
                DataProcessingThreadPoolConfig.namedThreadFactory("接口消息发送线程-"));
        // 已取消的任务立即移出调度队列，关闭后不继续执行周期任务。
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    /** 仅发布消息和预占额度，业务处理线程不操作排序索引，也不等待消息发送。 */
    public boolean offer(CollectInterfaceRuntimeConfig config, long sequenceNumber,
            int serializedSize, Runnable sendAction) {
        return offer(config, sequenceNumber, serializedSize, sendAction, null);
    }

    /** 遥测同时保存组批所需的消息引用，序列化仍由后台发送线程完成。 */
    public boolean offerTelemetry(CollectInterfaceRuntimeConfig config, long sequenceNumber,
            TelemetryMessage telemetry, Runnable sendAction) {
        return offer(config, sequenceNumber, telemetry.getSerializedSize(), sendAction,
                batchEnabled ? telemetry : null);
    }

    /** 单条和批量发送共用入队及容量检查。 */
    private boolean offer(CollectInterfaceRuntimeConfig config, long sequenceNumber,
            int serializedSize, Runnable sendAction, TelemetryMessage telemetry) {
        activeOffers.incrementAndGet();
        try {
            // 第一步：关闭后拒绝新消息，非法接口和序号不能建立排序队列。
            if (closing.get() || config.getInterfaceId() == null || serializedSize < 0
                    || sequenceNumber < 0L || sequenceNumber > SEQUENCE_MASK) {
                report(config, sequenceNumber, "消息缓冲正在关闭或接口、包序号、消息长度无效", null);
                return false;
            }
            InterfaceQueue queue = interfaces.computeIfAbsent(config.getInterfaceId(), this::createQueue);
            long bytes = serializedSize + RECORD_OVERHEAD_BYTES;
            // 第二步：依次预占接口条数、全局条数和字节额度，失败时归还已占额度。
            if (!reserveCount(queue.count, properties.getMessageOrder().getMaxPendingPerInterface())) {
                report(config, sequenceNumber, "本接口待发送消息数量已达上限，当前消息被拒绝", null);
                return false;
            }
            if (!reserveCount(pendingCount, properties.getMessageQueueCapacity())) {
                queue.count.decrementAndGet();
                report(config, sequenceNumber, "全局待发送消息数量已达上限，当前消息被拒绝", null);
                return false;
            }
            if (!reserveBytes(bytes)) {
                queue.count.decrementAndGet();
                pendingCount.decrementAndGet();
                report(config, sequenceNumber, "全局待发送消息字节数已达上限，当前消息被拒绝", null);
                return false;
            }
            // 第三步：计时起点固定在入队前，后续消息及定时扫描都不会重置它。
            queue.incoming.offer(new PendingMessage(config, sequenceNumber, bytes, sendAction, telemetry));
            return true;
        } finally {
            activeOffers.decrementAndGet();
        }
    }

    /** 每个接口只登记一个周期任务，由调度器保证前后执行之间的可见性和互斥。 */
    private InterfaceQueue createQueue(Long interfaceId) {
        InterfaceQueue queue = new InterfaceQueue(interfaceId);
        long interval = properties.getMessageOrder().getScanIntervalMillis();
        queue.future = scheduler.scheduleWithFixedDelay(queue, interval, interval, TimeUnit.MILLISECONDS);
        return queue;
    }

    /** 使用原子比较更新预占条数，不引入显式互斥锁。 */
    private boolean reserveCount(AtomicInteger counter, int limit) {
        int current = counter.get();
        while (current < limit) {
            if (counter.compareAndSet(current, current + 1)) {
                return true;
            }
            current = counter.get();
        }
        return false;
    }

    /** 使用减法校验剩余额度，避免字节数相加溢出。 */
    private boolean reserveBytes(long bytes) {
        long current = pendingBytes.get();
        long limit = properties.getMessageOrder().getMaxPendingBytes();
        while (bytes <= limit - current) {
            if (pendingBytes.compareAndSet(current, current + bytes)) {
                return true;
            }
            current = pendingBytes.get();
        }
        return false;
    }

    /** 发送成功或明确丢弃时一次性归还接口、全局条数及字节额度。 */
    private void release(InterfaceQueue queue, PendingMessage message) {
        queue.count.decrementAndGet();
        pendingBytes.addAndGet(-message.bytes);
        pendingCount.decrementAndGet();
    }

    /** 告警组件自身失败时保留日志，不让周期任务永久停止。 */
    private void report(CollectInterfaceRuntimeConfig config, long sequenceNumber,
            String detail, RuntimeException exception) {
        LOGGER.warn("{}，接口编号：{}，包序号：{}", detail, config.getInterfaceId(), sequenceNumber, exception);
        if (alarmRecorder != null) {
            try {
                alarmRecorder.record(config, DataAlarmType.MESSAGE_FAILURE,
                        "包序号：" + sequenceNumber + "；" + detail);
            } catch (RuntimeException alarmException) {
                LOGGER.error("消息重排告警记录失败，接口编号：{}", config.getInterfaceId(), alarmException);
            }
        }
    }

    /** 正常销毁沿用结果配置中的关闭等待上限。 */
    @PreDestroy
    public void shutdown() {
        shutdown(System.nanoTime() + TimeUnit.SECONDS.toNanos(properties.getShutdownWaitSeconds()));
    }

    /** 关闭时停止接收新消息，现有周期任务按序刷新，超过期限明确报告未发送数量。 */
    public void shutdown(long deadline) {
        if (!closing.compareAndSet(false, true)) {
            return;
        }
        // 第一步：保留周期调度，关闭标记使剩余消息无需继续等待重排时间。
        while ((pendingCount.get() > 0 || activeOffers.get() > 0)
                && System.nanoTime() < deadline && !Thread.currentThread().isInterrupted()) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1L));
        }
        scheduler.shutdown();
        try {
            // 第二步：等待已经进入发送过程的任务退出，和入库共用关闭期限。
            if (scheduler.awaitTermination(Math.max(0L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                if (pendingCount.get() == 0) {
                    LOGGER.info("接口消息重排关闭完成，全部待发送消息已处理");
                } else {
                    LOGGER.warn("接口消息重排关闭结束，未发送消息数量：{}，估算字节数：{}",
                            pendingCount.get(), pendingBytes.get());
                }
                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        // 第三步：外部客户端未及时结束时中断任务，并保留未完成量供排查。
        scheduler.shutdownNow();
        LOGGER.warn("接口消息重排关闭超时或被中断，未发送消息数量：{}，估算字节数：{}",
                pendingCount.get(), pendingBytes.get());
    }

    /** 一个接口的所有可变排序状态只由自身周期任务访问。 */
    private final class InterfaceQueue implements Runnable {

        /** 多个业务线程只向无锁入口投递。 */
        private final ConcurrentLinkedQueue<PendingMessage> incoming = new ConcurrentLinkedQueue<>();
        /** 接口条数包含尚未移入排序索引的消息及重试消息。 */
        private final AtomicInteger count = new AtomicInteger();
        /** 序号索引同时负责去重和按序取出，插入及删除均为对数复杂度。 */
        private final NavigableMap<Long, PendingMessage> bySequence = new TreeMap<>();
        /** 到期索引支持在小序号被提前发送时同步删除，避免残留到期记录。 */
        private final TreeSet<PendingMessage> byTime = new TreeSet<>(
                Comparator.comparingLong((PendingMessage message) -> message.queuedAt)
                        .thenComparingLong(message -> message.index));
        /** 仅用于日志及本接口任务的取消。 */
        private final Long interfaceId;
        private volatile ScheduledFuture<?> future;
        /** 时间相同时按本任务登记顺序区分消息。 */
        private long nextIndex;
        /** 失败后留在队列中，重试间隔内仍继续收取其他消息。 */
        private long retryAt;
        /** 记录当前组批轮次的最大可发序号，以及下轮最早开始时间。 */
        private Long roundLastSequence;
        private long nextRoundAt = System.nanoTime();
        /** 原批一直保留在排序索引中，失败重试也使用同一批成员。 */
        private PendingBatch pendingBatch;

        private InterfaceQueue(Long interfaceId) {
            this.interfaceId = interfaceId;
        }

        /** 捕获非预期运行异常，防止调度器取消此接口后续所有执行。 */
        @Override
        public void run() {
            try {
                if (batchEnabled && sendEnabled) {
                    sendBatchRound();
                } else {
                    sendReadyMessages();
                }
            } catch (RuntimeException exception) {
                LOGGER.error("接口消息重排任务执行失败，接口编号：{}，后续周期继续重试", interfaceId, exception);
            }
            // 关闭时只取消已排空接口，尚有消息的接口继续按序发送或重试。
            if (closing.get() && count.get() == 0 && activeOffers.get() == 0 && future != null) {
                future.cancel(false);
            }
        }

        /** 到期包只放行不大于自身序号的前缀，较小包无需单独等满配置时间。 */
        private void sendReadyMessages() {
            if (retryAt != 0L && System.nanoTime() - retryAt < 0L) {
                // 等待重试时仍将新消息纳入排序和队列内去重。
                drainIncoming();
                return;
            }
            for (int released = 0; released < properties.getMessageOrder().getMaxSendsPerRun(); released++) {
                // 每次发送前收取新到消息，使等待期间到达的小序号能进入本次前缀。
                drainIncoming();
                if (bySequence.isEmpty()) {
                    return;
                }
                PendingMessage oldest = byTime.first();
                long waitNanos = TimeUnit.MILLISECONDS.toNanos(properties.getMessageOrder().getWaitMillis());
                // 有待发消息到期时，队首序号必定不大于它；到期包发完后重新按剩余消息判断。
                if (!closing.get() && System.nanoTime() - oldest.queuedAt < waitNanos) {
                    return;
                }
                if (!sendSingle(bySequence.firstEntry().getValue())) {
                    return;
                }
            }
        }

        /** 等确认期间继续收取新消息，确认慢时从当前积压中选取下一轮。 */
        private void sendBatchRound() {
            drainIncoming();
            // 第一步：由当前接口的周期任务处理回调结果，确保排序索引始终由一个任务修改。
            if (pendingBatch != null && pendingBatch.confirmation != null && !consumeConfirmation()) {
                return;
            }
            long now = System.nanoTime();
            if (retryAt != 0L && now - retryAt < 0L) {
                return;
            }
            if (pendingBatch != null) {
                submitBatch();
                return;
            }

            // 第二步：本轮拆出的批次不再等待组批间隔，排完才按实际时间决定能否开下一轮。
            if (bySequence.isEmpty()) {
                roundLastSequence = null;
                return;
            }
            long first = bySequence.firstKey();
            if (roundLastSequence == null || first > roundLastSequence) {
                roundLastSequence = null;
                Long readyThrough = readyThrough(now);
                if (readyThrough == null || first > readyThrough) {
                    return;
                }
                PendingMessage head = bySequence.firstEntry().getValue();
                if (head.telemetry == null) {
                    sendSingle(head);
                    return;
                }
                if (!closing.get() && now - nextRoundAt < 0L) {
                    return;
                }
                roundLastSequence = readyThrough;
                nextRoundAt = now + TimeUnit.MILLISECONDS.toNanos(properties.getMessageOrder().getBatchIntervalMillis());
            }

            // 第三步：只合并排序前缀中的连续遥测记录，指令响应仍占用自己的顺序位置。
            PendingMessage head = bySequence.firstEntry().getValue();
            if (head.telemetry == null) {
                sendSingle(head);
                return;
            }
            pendingBatch = selectBatch();
            submitBatch();
        }

        /** 所有已到期消息中的最大序号确定本轮可发送的前缀。 */
        private Long readyThrough(long now) {
            if (closing.get()) {
                return bySequence.lastKey();
            }
            Long last = null;
            long wait = TimeUnit.MILLISECONDS.toNanos(properties.getMessageOrder().getWaitMillis());
            for (PendingMessage message : byTime) {
                if (now - message.queuedAt < wait) {
                    break;
                }
                if (last == null || message.extendedSequence > last) {
                    last = message.extendedSequence;
                }
            }
            return last;
        }

        /** 按原生编码大小及条数拆分，首条超出组批额度时按单条发送。 */
        private PendingBatch selectBatch() {
            ResultDispatchProperties.MessageOrder order = properties.getMessageOrder();
            int maxBytes = Math.min(order.getBatchMaxBytes(), messageService.producerMaxMessageBytes());
            List<PendingMessage> entries = new ArrayList<>();
            List<TelemetryMessage> messages = new ArrayList<>();
            long bytes = 0L;
            for (PendingMessage message : bySequence.headMap(roundLastSequence, true).values()) {
                if (message.telemetry == null || entries.size() >= order.getBatchMaxMessages()) {
                    break;
                }
                long size = messageService.encodedBatchEntrySize(message.telemetry.getSerializedSize());
                if (!entries.isEmpty() && size > maxBytes - bytes) {
                    break;
                }
                entries.add(message);
                messages.add(message.telemetry);
                bytes += size;
            }
            return new PendingBatch(entries, messages);
        }

        /** 本接口只持有一份待确认结果，提交异常按原批重试。 */
        private void submitBatch() {
            try {
                pendingBatch.confirmation = messageService.sendOrderedAsync(interfaceId, pendingBatch.messages);
            } catch (RuntimeException exception) {
                scheduleRetry(pendingBatch.entries.get(0), exception);
            }
        }

        /** 回调成功后释放本批；失败时保留本批所有成员及发送边界。 */
        private boolean consumeConfirmation() {
            if (!pendingBatch.confirmation.isDone()) {
                return false;
            }
            try {
                pendingBatch.confirmation.join();
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause();
                RuntimeException failure = cause instanceof RuntimeException
                        ? (RuntimeException) cause : new IllegalStateException("遥测批次发送失败", cause);
                pendingBatch.confirmation = null;
                scheduleRetry(pendingBatch.entries.get(0), failure);
                return false;
            }
            for (PendingMessage entry : pendingBatch.entries) {
                removeSent(entry);
            }
            pendingBatch = null;
            retryAt = 0L;
            return true;
        }

        /** 单条发送复用已有动作和失败告警。 */
        private boolean sendSingle(PendingMessage message) {
            try {
                if (sendEnabled) {
                    message.sendAction.run();
                }
            } catch (RuntimeException exception) {
                scheduleRetry(message, exception);
                return false;
            }
            removeSent(message);
            retryAt = 0L;
            return true;
        }

        /** 失败后保留队首并延迟重试。 */
        private void scheduleRetry(PendingMessage head, RuntimeException exception) {
            retryAt = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(properties.getMessageOrder().getRetryIntervalMillis());
            report(head.config, head.sequenceNumber, "消息发送失败，保留本接口消息等待重试", exception);
        }

        /** 入口可能新增更小序号，因此必须按本次确认的记录删除索引。 */
        private void removeSent(PendingMessage message) {
            bySequence.remove(message.extendedSequence);
            byTime.remove(message);
            release(this, message);
        }

        /** 有界收取入口消息，持续高负载时也给到期判断和发送留出执行机会。 */
        private void drainIncoming() {
            for (int drained = 0; drained < properties.getMessageQueueCapacity(); drained++) {
                PendingMessage message = incoming.poll();
                if (message == null) {
                    return;
                }
                // 将无符号序号展开为连续逻辑序号，正常回绕后零位于最大序号之后。
                message.extendedSequence = extendSequence(message.sequenceNumber);
                if (bySequence.containsKey(message.extendedSequence)) {
                    report(message.config, message.sequenceNumber, "本接口缓冲区存在相同包序号，重复消息被丢弃", null);
                    release(this, message);
                } else {
                    // 每条消息同时登记序号和时间索引，所有操作都由本任务串行执行。
                    message.index = nextIndex++;
                    bySequence.put(message.extendedSequence, message);
                    byTime.add(message);
                }
            }
        }

        /** 以当前队列的最大逻辑序号展开回绕；当前待发包的跨度应小于半个序号空间。 */
        private long extendSequence(long sequenceNumber) {
            // 队列为空时直接以新包序号开始，不沿用已经发送的序号。
            if (bySequence.isEmpty()) {
                return sequenceNumber;
            }
            long referenceSequence = bySequence.lastKey();
            long distance = (sequenceNumber - (referenceSequence & SEQUENCE_MASK)) & SEQUENCE_MASK;
            if (distance >= SEQUENCE_HALF) {
                distance -= SEQUENCE_RANGE;
            }
            return referenceSequence + distance;
        }
    }

    /** 保留原批排序和成员，回调只更新确认结果。 */
    private static final class PendingBatch {
        private final List<PendingMessage> entries;
        private final List<TelemetryMessage> messages;
        private CompletableFuture<Void> confirmation;

        private PendingBatch(List<PendingMessage> entries, List<TelemetryMessage> messages) {
            this.entries = entries;
            this.messages = messages;
        }
    }

    /** 队列保存处理结果及原始包序号，不改变发送给下游的消息字段。 */
    private static final class PendingMessage {

        /** 接口快照用于失败告警，发送动作自行保留原接口的日志上下文。 */
        private final CollectInterfaceRuntimeConfig config;
        /** 原始序号用于日志，展开序号只在内部排序时使用。 */
        private final long sequenceNumber;
        private final long bytes;
        private final Runnable sendAction;
        /** 可组批的遥测消息，指令响应用原来的单条发送动作。 */
        private final TelemetryMessage telemetry;
        /** 单调时钟避免系统时间调整改变等待时长。 */
        private final long queuedAt = System.nanoTime();
        /** 以下字段由所属接口的周期任务首次登记时填写。 */
        private long extendedSequence;
        private long index;

        private PendingMessage(CollectInterfaceRuntimeConfig config, long sequenceNumber,
                long bytes, Runnable sendAction, TelemetryMessage telemetry) {
            this.config = config;
            this.sequenceNumber = sequenceNumber;
            this.bytes = bytes;
            this.sendAction = sendAction;
            this.telemetry = telemetry;
        }
    }
}
