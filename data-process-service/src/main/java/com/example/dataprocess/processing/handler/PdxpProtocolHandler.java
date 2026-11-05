package com.example.dataprocess.processing.handler;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelecommandMessages.CommandExecResultMsg;
import com.example.common.enums.CollectionDataType;
import com.example.dataprocess.collection.CollectInterfaceStatistics;
import com.example.dataprocess.collection.ReceivedDatagram;
import com.example.dataprocess.config.PdxpFramingProperties;
import com.example.dataprocess.config.CommandResponseProperties;
import com.example.dataprocess.protocol.command.CommandResponseParser;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.processing.processor.DataProcessor;
import com.example.dataprocess.processing.processor.LocalDataProcessor;
import com.example.dataprocess.processing.processor.RpcDataProcessor;
import com.example.dataprocess.service.CalibrationProcessingService;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.service.TelemetryMessageService;
import com.example.dataprocess.processing.dispatcher.TelemetryResultDispatcher;
import com.example.dataprocess.protocol.rpc.PdxpDataPayload;
import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import com.example.dataprocess.tool.PdxpParser;
import com.example.dataprocess.tool.TcpTool;
import com.example.dataprocess.tool.UdpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BooleanSupplier;

/** PDXP统一接收入口，按采集数据类型分流设备、卫星和指令响应。 */
@Component
public class PdxpProtocolHandler implements ProtocolHandler {
    /** 运行环境注入同一份拆帧配置，离线构造时保持既有默认长度。 */
    private PdxpFramingProperties framingProperties = new PdxpFramingProperties();
    /** 指令响应保留独立传输头长度，避免套用设备和卫星的遥测拆帧参数。 */
    private CommandResponseProperties commandResponseProperties = new CommandResponseProperties();

    /** 运行时绑定指令响应拆帧参数，离线调用沿用无附加头默认值。 */
    @org.springframework.beans.factory.annotation.Autowired
    public void setCommandResponseProperties(CommandResponseProperties properties) {
        this.commandResponseProperties = properties;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setFramingProperties(PdxpFramingProperties framingProperties) {
        this.framingProperties = framingProperties;
    }

    /** 告警组件采用独立注入，复用已有离线构造入口。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.dataprocess.alarm.DataAlarmRecorder alarmRecorder;

    /** 提交异常上下文，采集线程不直接写数据库。 */
    private void alarm(com.example.dataprocess.entity.CollectInterfaceRuntimeConfig config,
            com.example.common.enums.DataAlarmType type, String detail) {
        if (alarmRecorder != null) { alarmRecorder.record(config, type, detail); }
    }


    /** PDXP传输协议。 */
    private static final int TRANSFER_PROTOCOL_PDXP = 2;
    /** PDXP协议日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            PdxpProtocolHandler.class);
    /** 采集统计。 */
    private final CollectInterfaceStatistics statistics;
    /** 数据处理线程池。 */
    private final ThreadPoolExecutor dataProcessExecutor;
    /** 遥测入库和消息发送的独立异步调度服务。 */
    private final TelemetryResultDispatcher resultDispatcher;
    /** 本地数据处理器。 */
    private final LocalDataProcessor localProcessor;
    /** RPC数据处理器。 */
    private final RpcDataProcessor rpcProcessor;
    /** 按遥测参数校准公式执行野值检测的服务。 */
    private final CalibrationProcessingService calibrationProcessingService;
    /** 运行环境使用独立结果线程池。 */
    @org.springframework.beans.factory.annotation.Autowired
    public PdxpProtocolHandler(
            CollectInterfaceStatistics statistics,
            @Qualifier("dataProcessExecutor")
            ThreadPoolExecutor dataProcessExecutor,
            TelemetryResultDispatcher resultDispatcher,
            LocalDataProcessor localProcessor,
            RpcDataProcessor rpcProcessor,
            CalibrationProcessingService calibrationProcessingService) {
        // 处理线程只保留业务处理和任务提交职责。
        this.statistics = statistics;
        this.dataProcessExecutor = dataProcessExecutor;
        this.resultDispatcher = resultDispatcher;
        this.localProcessor = localProcessor;
        this.rpcProcessor = rpcProcessor;
        this.calibrationProcessingService = calibrationProcessingService;
    }

    /** 保留已有离线构造入口，由直接执行器确定结果任务执行时机。 */
    public PdxpProtocolHandler(CollectInterfaceStatistics statistics,
            ThreadPoolExecutor dataProcessExecutor, IoTDBTelemetryStorageService storageService,
            LocalDataProcessor localProcessor, RpcDataProcessor rpcProcessor,
            CalibrationProcessingService calibrationProcessingService,
            TelemetryMessageService messageService) {
        // 离线入口复用统一提交逻辑，运行环境始终选择上方异步构造器。
        this(statistics, dataProcessExecutor,
                new TelemetryResultDispatcher(storageService, messageService, Runnable::run, Runnable::run),
                localProcessor, rpcProcessor, calibrationProcessingService);
    }

    /** 返回PDXP接收处理器支持的传输协议。 */
    @Override
    public List<Integer> supportedTransferProtocols() {
        return Collections.singletonList(TRANSFER_PROTOCOL_PDXP);
    }

    /** 创建一个UDP PDXP协议会话。 */
    @Override
    public UdpSession createUdpSession(
            CollectInterfaceRuntimeConfig config,
            UdpTool.DatagramEndpoint endpoint) {
        return packet -> submit(config, packet.getData(), "UDP");
    }

    /** 创建一个TCP PDXP协议会话。 */
    @Override
    public TcpSession createTcpSession(CollectInterfaceRuntimeConfig config) {
        // 指令响应当前按已约定的UDP报文格式接收，直接创建会话也需校验。
        if (CollectionDataType.require(config.getRpcEnabled()) == CollectionDataType.COMMAND_RESPONSE) {
            throw new IllegalArgumentException("指令响应采集必须使用UDP传输方式");
        }
        return (connection, running) -> receiveTcpPackets(
                config, connection, running);
    }

    /** 从TCP字节流持续恢复完整PDXP包。 */
    private void receiveTcpPackets(
            CollectInterfaceRuntimeConfig config,
            TcpTool.TcpConnection connection,
            BooleanSupplier running) throws IOException {
        while (running.getAsBoolean() && connection.isOpen()) {
            // TCP恢复完整帧时使用与后续业务处理一致的传输头长度。
            PdxpParser.PdxpPacket packet = connection.receivePdxpPacket(
                    framingProperties.select(config.getRpcEnabled()).getTransportHeaderLength());
            submit(config, packet.getRawPacket(), "TCP");
        }
    }

    /** 统计完整报文并提交PDXP业务处理线程池。 */
    private void submit(
            CollectInterfaceRuntimeConfig config,
            byte[] packetData,
            String transferName) {
        // 传输层字节用于秒级速率，采集量要等解析出完整PDXP帧后再累计。
        statistics.recordReceivedBytes(config, packetData.length);
        ReceivedDatagram datagram = new ReceivedDatagram(config, packetData);
        try {
            dataProcessExecutor.execute(() -> handle(datagram));
        } catch (RejectedExecutionException exception) {
            alarm(config, com.example.common.enums.DataAlarmType.PROCESS_QUEUE, "处理线程池已满，当前报文被拒绝");
            LOGGER.error("数据处理线程池已满，本次PDXP {}报文已拒绝，接口编号：{}",
                    transferName, config.getInterfaceId(), exception);
        }
    }

    /** 解析一个传输报文，并逐个处理其中的PDXP包。 */
    public void handle(ReceivedDatagram datagram) {
        List<PdxpParser.PdxpPacket> packets;
        try {
            // 数据处理线程按接口模式拆帧，TCP接收端已完成定界及反转义。
            packets = PdxpParser.parseTransportUdp(datagram.getData(),
                    transportHeaderLength(datagram.getInterfaceConfig()));
        } catch (Exception exception) {
            alarm(datagram.getInterfaceConfig(), com.example.common.enums.DataAlarmType.PACKET_PARSE,
                    "报文长度：" + datagram.getData().length + "；解析失败：" + exception.getMessage());
            LOGGER.error("PDXP报文解析失败，异常帧暂未写入GreptimeDB，接口编号：{}",
                    datagram.getInterfaceConfig().getInterfaceId(), exception);
            return;
        }

        if (packets.isEmpty()) {
            alarm(datagram.getInterfaceConfig(), com.example.common.enums.DataAlarmType.PACKET_PARSE,
                    "报文长度：" + datagram.getData().length + "；未形成完整报文，当前数据不进入处理链路");
            LOGGER.warn("PDXP报文结构不完整，异常帧暂未写入GreptimeDB，接口编号：{}",
                    datagram.getInterfaceConfig().getInterfaceId());
            return;
        }

        for (PdxpParser.PdxpPacket packet : packets) {
            // 一个UDP报文可能包含多帧，必须按解析出的完整PDXP帧逐个统计。
            statistics.recordCollection(datagram.getInterfaceConfig());
            // 先按采集类型分流，指令响应直接组装并发送消息。
            if (CollectionDataType.require(datagram.getInterfaceConfig().getRpcEnabled())
                    == CollectionDataType.COMMAND_RESPONSE) {
                handleCommandResponse(datagram.getInterfaceConfig(), packet);
            } else {
                handleNormalPacket(datagram.getInterfaceConfig(), packet);
            }
        }
    }

    /** 按采集类型选择UDP传输头长度。 */
    private int transportHeaderLength(CollectInterfaceRuntimeConfig config) {
        // 指令响应配置独立长度，设备与卫星继续复用已有分组。
        if (CollectionDataType.require(config.getRpcEnabled()) == CollectionDataType.COMMAND_RESPONSE) {
            return commandResponseProperties.getTransportHeaderLength();
        }
        return framingProperties.select(config.getRpcEnabled()).getTransportHeaderLength();
    }

    /** 解析完整指令响应并提交已有消息队列，失败帧不累计处理成功量。 */
    private void handleCommandResponse(CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        try {
            // 第一步：复用既有固定协议解析器，只组装约定的五个消息字段。
            CommandExecResultMsg message = CommandResponseParser.parse(packet);
            // 第二步：发送任务被接受后累计一次帧级处理量，与遥测异步统计口径一致。
            if (resultDispatcher.submitCommandResponse(config, message, packet.getSequenceNumber())) {
                statistics.recordFrameProcessing(config.getTaskId(), config.getInterfaceId());
            }
        } catch (Exception exception) {
            // 第三步：单帧组装失败记录告警，后续报文继续使用同一采集接口接收。
            alarm(config, com.example.common.enums.DataAlarmType.PACKET_PARSE,
                    "包序号：" + packet.getSequenceNumber() + "；指令响应处理失败：" + exception.getMessage());
            LOGGER.error("指令响应处理失败，接口编号：{}，包序号：{}",
                    config.getInterfaceId(), packet.getSequenceNumber(), exception);
        }
    }

    /** 完成业务处理后分别提交入库和消息任务，再累计业务处理量。 */
    private void handleNormalPacket(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        com.example.common.enums.DataAlarmType stage = com.example.common.enums.DataAlarmType.PACKET_PARSE;
        // 只有本地参数处理失败且数据域已成功拆分，才允许保存异常整帧。
        boolean localProcessing = CollectionDataType.require(config.getRpcEnabled()) == CollectionDataType.DEVICE;
        boolean payloadParsed = false;
        // 告警组件随后报错时也不重复提交同一异常整帧。
        boolean abnormalFrameAttempted = false;
        try {
            // 第一步：按接口处理模式选择长度，再拆分数据域。
            PdxpFramingProperties.Framing framing = framingProperties.select(config.getRpcEnabled());
            PdxpDataPayload payload = PdxpDataPayloadParser.parse(
                    packet.getData(), framing.getCustomDataLength(),
                    framing.getTelemetryHeaderLength());
            payloadParsed = true;
            // 第二步：业务处理器与拆帧长度均使用同一远程开关选择。
            DataProcessor processor = CollectionDataType.require(config.getRpcEnabled())
                    == CollectionDataType.SATELLITE ? rpcProcessor : localProcessor;
            stage = CollectionDataType.require(config.getRpcEnabled()) == CollectionDataType.SATELLITE
                    ? com.example.common.enums.DataAlarmType.RPC_FAILURE : com.example.common.enums.DataAlarmType.PROCESS_CONFIG;
            Optional<TelemetryMessage> processedMessage =
                    processor.process(config, packet, payload);
            if (!processedMessage.isPresent()) {
                // 本地没有解析结果时只保存异常整帧，RPC没有结果时保持不入库。
                if (localProcessing) {
                    abnormalFrameAttempted = true;
                    saveLocalAbnormalFrame(config, packet);
                }
                alarm(config, stage, "包序号：" + packet.getSequenceNumber() + "；未生成有效遥测结果，不写参数、不发送遥测消息");
                LOGGER.info("遥测消息转换失败，不写参数、不发送遥测消息，接口编号：{}，包序号：{}",
                        config.getInterfaceId(), packet.getSequenceNumber());
                return;
            }

            TelemetryMessage telemetryMessage = processedMessage.get();
            // 本地及远程结果保留参数状态，范围越限不写入或恢复告警管理记录。
            stage = com.example.common.enums.DataAlarmType.CALIBRATION_RULE;
            // 第三步：按每个遥测参数携带的校准公式执行野值检测。
            telemetryMessage = calibrationProcessingService.process(
                    config, telemetryMessage);
            // 第四步：校准完成后按最终保留参数统计真实野值数量。
            statistics.recordWildValue(config, telemetryMessage
                    .getTelemetriesMap().values().stream()
                    .filter(telemetry -> telemetry.getIsWildValue())
                    .count());
            // 第五步：两个结果任务独立提交，处理线程不等待外部服务响应。
            if (!resultDispatcher.submit(config, telemetryMessage, packet)) {
                // 提交失败已有独立告警，部分被接受的任务继续执行，当前帧不累计成功处理量。
                return;
            }
            // 第六步：业务处理完成且两个任务均被接受后累计一次，不代表外部写入成功。
            statistics.recordFrameProcessing(
                    config.getTaskId(), config.getInterfaceId());
        } catch (Exception exception) {
            // 仅回退本地参数处理阶段，不把数据域拆分、RPC、校准或结果任务错误误记为帧异常。
            if (localProcessing && payloadParsed && !abnormalFrameAttempted
                    && stage == com.example.common.enums.DataAlarmType.PROCESS_CONFIG) {
                saveLocalAbnormalFrame(config, packet);
            }
            // 校准层已记录具体参数时不重复生成接口级告警。
            if (!(exception instanceof com.example.dataprocess.exception.RecordedDataAlarmException)) {
                alarm(config, stage, "包序号：" + packet.getSequenceNumber() + "；当前帧处理失败：" + exception.getMessage());
            }
            LOGGER.error("正常PDXP帧后续处理失败，接口编号：{}，包序号：{}",
                    config.getInterfaceId(), packet.getSequenceNumber(), exception);
        }
    }

    /** 异常整帧异步保存失败不能掩盖原始参数解析错误。 */
    private void saveLocalAbnormalFrame(CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        try {
            // 这里只提交已有入库线程池，实际写入仍会校验任务、设备、通道和数据域。
            if (resultDispatcher.submitAbnormalLocalFrame(config, packet)) {
                LOGGER.info("本地参数解析失败，异常整帧已提交入库，接口编号：{}，包序号：{}",
                        config.getInterfaceId(), packet.getSequenceNumber());
            }
        } catch (RuntimeException exception) {
            LOGGER.error("本地异常整帧任务提交失败，接口编号：{}，包序号：{}",
                    config.getInterfaceId(), packet.getSequenceNumber(), exception);
        }
    }

}
