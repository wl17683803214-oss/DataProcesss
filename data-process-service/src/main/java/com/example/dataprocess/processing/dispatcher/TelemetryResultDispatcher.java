package com.example.dataprocess.processing.dispatcher;

import com.example.dataprocess.processing.PdxpTiming;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelecommandMessages.CommandExecResultMsg;
import com.example.common.enums.DataAlarmType;
import com.example.common.greptime.GreptimeProperties;
import com.example.dataprocess.alarm.DataAlarmRecorder;
import com.example.dataprocess.config.DataProcessingThreadPoolConfig;
import com.example.dataprocess.config.PdxpFramingProperties;
import com.example.dataprocess.config.ResultDispatchProperties;
import com.example.dataprocess.config.TimingLogProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.logging.PdxpFrameLogContext;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.service.TelemetryMessageService;
import com.example.dataprocess.service.CommandResponseMessageService;
import com.example.dataprocess.service.TelemetryRealtimeCacheWriter;
import com.example.dataprocess.service.TelemetryRecordFactory;
import com.example.dataprocess.service.TelemetrySelectionCache;
import com.example.dataprocess.tool.PdxpParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.sql.Timestamp;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 遥测存储独立排队，结果消息按采集接口连续重排并串行发送。 */
@Service
public class TelemetryResultDispatcher {
    /** 异步结果处理日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryResultDispatcher.class);
    /** 遥测存储服务及其专用执行器，禁用入库时不创建执行器。 */
    private final IoTDBTelemetryStorageService storageService;
    private final Executor storageExecutor;
    /** 消息发送服务及离线调用者提供的执行器。 */
    private final TelemetryMessageService messageService;
    private final Executor messageExecutor;
    /** 运行环境按接口连续重排，离线直接执行入口保留原有行为。 */
    private final InterfaceMessageOrderBuffer messageOrderBuffer;
    /** 指令响应复用现有消息执行器，保留原有离线构造方法。 */
    private CommandResponseMessageService commandResponseMessageService;
    /** 实时分发与入库、消息发送分别排队，离线入口保留原有构造方式。 */
    private TelemetryRealtimeCacheWriter realtimeWriter;
    private PdxpFramingProperties framingProperties;
    /** 运行时共用日志配置，离线构造保留默认采样规则。 */
    @Autowired
    private TimingLogProperties timingLog = new TimingLogProperties();
    /** 仅实时缓存使用已勾选并集，数据库持久化继续使用完整处理结果。 */
    @Autowired
    private TelemetrySelectionCache selectionCache;

    @Autowired
    public void setRealtimeWriter(TelemetryRealtimeCacheWriter writer, PdxpFramingProperties framing) {
        this.realtimeWriter = writer;
        this.framingProperties = framing;
    }

    /** 注入指令响应发送服务，避免旧调用方增加无关构造参数。 */
    @Autowired
    public void setCommandResponseMessageService(CommandResponseMessageService service) {
        this.commandResponseMessageService = service;
    }
    /** 复用现有异步告警记录组件。 */
    private final DataAlarmRecorder alarmRecorder;
    /** 入库线程池与消息缓冲共享的关闭等待上限。 */
    private final int shutdownWaitSeconds;

    /** 运行环境复用时序存储开关，并独立配置消息重排。 */
    @Autowired
    public TelemetryResultDispatcher(
            IoTDBTelemetryStorageService storageService,
            TelemetryMessageService messageService,
            DataAlarmRecorder alarmRecorder,
            ResultDispatchProperties properties,
            GreptimeProperties greptimeProperties,
            InterfaceMessageOrderBuffer messageOrderBuffer) {
        // 第一步：先校验全部配置，避免部分线程池创建后配置校验失败。
        if (properties.getStorageThreadCount() <= 0 || properties.getStorageQueueCapacity() <= 0
                || properties.getShutdownWaitSeconds() <= 0) {
            throw new IllegalArgumentException("遥测结果线程数、队列容量和关闭等待时间必须大于零");
        }
        // 第二步：启用入库时才创建有界线程池，消息始终提交给接口重排队列。
        this.storageService = storageService;
        this.messageService = messageService;
        this.alarmRecorder = alarmRecorder;
        this.storageExecutor = greptimeProperties.isEnabled()
                ? createExecutor(properties.getStorageThreadCount(), properties.getStorageQueueCapacity(), "遥测入库线程-")
                : null;
        this.messageExecutor = null;
        this.messageOrderBuffer = messageOrderBuffer;
        this.shutdownWaitSeconds = properties.getShutdownWaitSeconds();
    }

    /** 保留离线构造能力，由调用方提供执行策略，不创建后台线程。 */
    public TelemetryResultDispatcher(IoTDBTelemetryStorageService storageService,
            TelemetryMessageService messageService, Executor storageExecutor,
            Executor messageExecutor) {
        // 离线入口复用同一提交逻辑，执行器生命周期由调用方管理。
        this.storageService = storageService;
        this.messageService = messageService;
        this.storageExecutor = storageExecutor;
        this.messageExecutor = messageExecutor;
        this.messageOrderBuffer = null;
        this.alarmRecorder = null;
        this.shutdownWaitSeconds = 0;
    }

    /** 所需入库任务和消息重排任务均被接受后返回成功，不等待后台完成。 */
    public boolean submit(CollectInterfaceRuntimeConfig config,
            TelemetryMessage message, PdxpParser.PdxpPacket packet) {
        // 第一步：复制接口上下文，避免排队期间接口配置更新改变任务和设备身份。
        CollectInterfaceRuntimeConfig snapshot = snapshot(config);
        // 第二步：完整采集帧供实时分发和入库使用，消息发送继续使用原处理结果。
        byte[] rawFrame = packet.getRawPacket();
        // 解析结束时只提交引用，缓存与推送组装在后台进行。
        if (realtimeWriter != null) {
            Timestamp processTime = new Timestamp(System.currentTimeMillis());
            // 同一帧固定使用入队时的筛选版本，参数额度按本帧实际命中数量预占。
            java.util.Set<String> selected = selectionCache == null ? java.util.Collections.emptySet()
                    : selectionCache.selected(snapshot, message);
            int selectedCount = 0;
            for (String code : selected) {
                if (message.getTelemetriesMap().containsKey(code)) {
                    selectedCount++;
                }
            }
            realtimeWriter.offer(selectedCount, rawFrame.length,
                    () -> TelemetryRecordFactory.processed(snapshot, message, rawFrame, processTime, false, selected));
        }
        // 第三步：入库和消息任务独立提交，即使一方拒绝也尝试另一方。
        long sequenceNumber = packet.getSequenceNumber();
        boolean storageAccepted = submitStorage(snapshot, message, rawFrame, sequenceNumber);
        // 单条模式沿用发送上下文，批量模式由接口队列直接使用遥测消息引用。
        Runnable sendAction = () -> PdxpFrameLogContext.runWithInterface(config,
                () -> messageService.send(message.toByteArray()));
        boolean messageAccepted = messageOrderBuffer == null
                ? submitMessage(snapshot, sequenceNumber, message.getSerializedSize(), "遥测消息发送", sendAction)
                : messageOrderBuffer.offerTelemetry(snapshot, sequenceNumber, message, sendAction);
        return storageAccepted && messageAccepted;
    }

    /** 关闭入库时不提交任务，消息仍可入队并累计帧处理量。 */
    private boolean submitStorage(CollectInterfaceRuntimeConfig snapshot, TelemetryMessage message,
            byte[] rawFrame, long sequenceNumber) {
        if (storageExecutor == null) {
            return true;
        }
        // 只为命中包序号采样的任务记录组装排队时间。
        boolean timed = timingLog.shouldSample(sequenceNumber);
        long storageQueuedAt = timed ? System.nanoTime() : 0L;
        return execute(storageExecutor, snapshot, sequenceNumber,
                DataAlarmType.STORAGE_FAILURE, "遥测入库", () -> {
                    // 开始执行时再检查开关，避免为等待期间已关闭的采样准备上下文。
                    boolean collectTiming = timed && timingLog.isEnabled();
                    long storageStart = collectTiming ? System.nanoTime() : 0L;
                    if (collectTiming) {
                        // 先保留排队耗时，记录组装后复用缓冲估算值，避免重复遍历参数。
                        PdxpTiming.beginStorageSample();
                    }
                    try {
                        storageService.saveProcessedParameters(snapshot, message, rawFrame);
                    } finally {
                        if (collectTiming) {
                            // 先固定结束时间并清理上下文，日志输出不计入记录组装耗时。
                            long storageFinished = System.nanoTime();
                            Long estimatedBytes = PdxpTiming.endStorageSample();
                            if (timingLog.isEnabled()) {
                                PdxpTiming.logStorageQueue(snapshot.getInterfaceId(), sequenceNumber,
                                        storageStart - storageQueuedAt, estimatedBytes);
                                PdxpTiming.log(snapshot.getInterfaceId(), sequenceNumber, "记录组装和缓冲提交",
                                        storageFinished - storageStart);
                            }
                        }
                    }
                });
    }

    /** 指令响应只提交消息任务，提交成功后由调用方按帧累计处理统计。 */
    public boolean submitCommandResponse(CollectInterfaceRuntimeConfig config,
            CommandExecResultMsg message, long sequenceNumber) {
        // 第一步：固定接口身份，确保排队期间配置切换不改变消息所属上下文。
        CollectInterfaceRuntimeConfig snapshot = snapshot(config);
        // 第二步：指令响应复用同一接口的包序号缓冲和有序发送规则。
        return submitMessage(snapshot, sequenceNumber, message.getSerializedSize(),
                "指令响应消息发送", () ->
                        commandResponseMessageService.send(message,
                                snapshot.getInterfaceId(), sequenceNumber));
    }

    /** 运行环境在此保留包序号，处理结果入队成功即返回，不等待到期或发送确认。 */
    private boolean submitMessage(CollectInterfaceRuntimeConfig config, long sequenceNumber,
            int serializedSize, String operation, Runnable action) {
        if (messageOrderBuffer != null) {
            return messageOrderBuffer.offer(config, sequenceNumber, serializedSize, action);
        }
        // 离线调用者自行决定执行策略，已有直接构造入口不额外创建后台线程。
        return execute(messageExecutor, config, sequenceNumber,
                DataAlarmType.MESSAGE_FAILURE, operation, action::run);
    }

    /** 本地解析失败时分别提交异常帧的实时展示与入库任务。 */
    public boolean submitAbnormalLocalFrame(CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        // 第一步：固定本帧任务和设备身份，避免接口配置在排队期间变化。
        CollectInterfaceRuntimeConfig snapshot = snapshot(config);
        // 本地异常回退复用入库校验，组装失败时只影响对应实时记录。
        if (realtimeWriter != null) {
            Timestamp processTime = new Timestamp(System.currentTimeMillis());
            realtimeWriter.offer(0, packet.getRawPacketLength(),
                    () -> TelemetryRecordFactory.abnormal(snapshot, packet, framingProperties, processTime));
        }
        // 第二步：关闭入库时只保留实时展示，返回未提交以免记录已入库排队的日志。
        if (storageExecutor == null) {
            return false;
        }
        // 第三步：复用队列拒绝、存储异常和告警处理，不阻塞参数解析线程。
        return execute(storageExecutor, snapshot, packet.getSequenceNumber(),
                DataAlarmType.STORAGE_FAILURE, "本地异常整帧入库", () ->
                        storageService.markPdxpRawFrameAbnormal(snapshot, packet));
    }

    /** 复制接口上下文时共享准备好的解码快照，跳过会重新构建它的兼容入口。 */
    private CollectInterfaceRuntimeConfig snapshot(CollectInterfaceRuntimeConfig config) {
        CollectInterfaceRuntimeConfig snapshot = new CollectInterfaceRuntimeConfig();
        BeanUtils.copyProperties(config, snapshot, "pdxpFields", "formulaScripts");
        return snapshot;
    }

    /** 将执行失败与提交失败分别记录，拒绝策略不会回退到处理线程。 */
    private boolean execute(Executor executor, CollectInterfaceRuntimeConfig config,
            long sequenceNumber, DataAlarmType type, String operation, ResultAction action) {
        try {
            // 工作线程仅执行本类结果任务，业务异常单独记录并保留原始堆栈。
            executor.execute(() -> {
                try {
                    action.run();
                } catch (Exception exception) {
                    failure(config, sequenceNumber, type, operation + "异步执行失败", exception);
                }
            });
            return true;
        } catch (RejectedExecutionException exception) {
            // 队列已满或服务关闭时立即返回失败，不等待队列、不在调用线程执行。
            failure(config, sequenceNumber, type, operation + "任务提交失败，队列已满或服务正在关闭", exception);
            return false;
        }
    }

    /** 复用现有中文告警类别及日志上下文。 */
    private void failure(CollectInterfaceRuntimeConfig config, long sequenceNumber,
            DataAlarmType type, String detail, Exception exception) {
        // 先记录执行错误，告警记录异常也不能影响另一个结果任务提交。
        LOGGER.error("{}，接口编号：{}，包序号：{}", detail,
                config.getInterfaceId(), sequenceNumber, exception);
        if (alarmRecorder != null) {
            try {
                alarmRecorder.record(config, type, "包序号：" + sequenceNumber
                        + "；" + detail + "；原因：" + exception.getMessage());
            } catch (RuntimeException alarmException) {
                LOGGER.error("异步结果告警记录失败，接口编号：{}，包序号：{}",
                        config.getInterfaceId(), sequenceNumber, alarmException);
            }
        }
    }

    /** 创建固定线程数、有界队列和立即拒绝策略的结果执行器。 */
    private ThreadPoolExecutor createExecutor(int threads, int capacity, String prefix) {
        // 复用现有线程命名方式，不采用调用线程执行策略。
        return new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(capacity),
                DataProcessingThreadPoolConfig.namedThreadFactory(prefix),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** 在存储与消息服务销毁前等待结果任务，超过总等待上限后取消剩余任务。 */
    @PreDestroy
    public void shutdown() {
        // 第一步：离线执行器由调用方管理；运行环境停止接受新入库任务。
        if (shutdownWaitSeconds == 0) {
            return;
        }
        ThreadPoolExecutor storagePool = (ThreadPoolExecutor) storageExecutor;
        if (storagePool != null) {
            storagePool.shutdown();
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(shutdownWaitSeconds);
        // 第二步：消息缓冲停止接收并立即按序刷新，两个环节共用关闭期限。
        messageOrderBuffer.shutdown(deadline);
        if (storagePool != null) {
            awaitShutdown(storagePool, deadline, "遥测入库");
        }
    }

    /** 等待一个执行器结束，并保留当前线程的中断状态。 */
    private void awaitShutdown(ThreadPoolExecutor executor, long deadline, String operation) {
        try {
            // 使用单调时钟计算剩余等待时间，避免系统时间调整导致无限等待。
            if (executor.awaitTermination(Math.max(0L, deadline - System.nanoTime()),
                    TimeUnit.NANOSECONDS)) {
                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("{}线程池关闭等待被中断", operation);
        }
        // 到达等待上限后中断执行中的任务；外部客户端是否立即退出取决于其超时设置。
        LOGGER.warn("{}线程池关闭等待结束，取消排队任务数量：{}",
                operation, executor.shutdownNow().size());
    }

    /** 支持存储接口声明的受检异常。 */
    @FunctionalInterface
    private interface ResultAction {
        void run() throws Exception;
    }
}
