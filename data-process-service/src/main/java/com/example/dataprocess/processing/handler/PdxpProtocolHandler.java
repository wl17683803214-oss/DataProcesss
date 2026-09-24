package com.example.dataprocess.processing.handler;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.collection.CollectInterfaceStatistics;
import com.example.dataprocess.collection.ReceivedDatagram;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.processing.processor.DataProcessor;
import com.example.dataprocess.processing.processor.LocalDataProcessor;
import com.example.dataprocess.processing.processor.RpcDataProcessor;
import com.example.dataprocess.service.CalibrationProcessingService;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.service.TelemetryMessageService;
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

/** PDXP协议接收和业务处理入口，RPC仍是正常PDXP帧后的处理分支。 */
@Component
public class PdxpProtocolHandler implements ProtocolHandler {

    /** PDXP传输协议。 */
    private static final int TRANSFER_PROTOCOL_PDXP = 2;
    /** PDXP协议日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            PdxpProtocolHandler.class);
    /** 采集统计。 */
    private final CollectInterfaceStatistics statistics;
    /** 数据处理线程池。 */
    private final ThreadPoolExecutor dataProcessExecutor;
    /** IoTDB数据存储服务。 */
    private final IoTDBTelemetryStorageService storageService;
    /** 本地数据处理器。 */
    private final LocalDataProcessor localProcessor;
    /** RPC数据处理器。 */
    private final RpcDataProcessor rpcProcessor;
    /** 按遥测参数校准公式执行野值检测的服务。 */
    private final CalibrationProcessingService calibrationProcessingService;
    /** 遥测消息RocketMQ发送服务。 */
    private final TelemetryMessageService messageService;

    public PdxpProtocolHandler(
            CollectInterfaceStatistics statistics,
            @Qualifier("dataProcessExecutor")
            ThreadPoolExecutor dataProcessExecutor,
            IoTDBTelemetryStorageService storageService,
            LocalDataProcessor localProcessor,
            RpcDataProcessor rpcProcessor,
            CalibrationProcessingService calibrationProcessingService,
            TelemetryMessageService messageService) {
        this.statistics = statistics;
        this.dataProcessExecutor = dataProcessExecutor;
        this.storageService = storageService;
        this.localProcessor = localProcessor;
        this.rpcProcessor = rpcProcessor;
        this.calibrationProcessingService = calibrationProcessingService;
        this.messageService = messageService;
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
        return (connection, running) -> receiveTcpPackets(
                config, connection, running);
    }

    /** 从TCP字节流持续恢复完整PDXP包。 */
    private void receiveTcpPackets(
            CollectInterfaceRuntimeConfig config,
            TcpTool.TcpConnection connection,
            BooleanSupplier running) throws IOException {
        while (running.getAsBoolean() && connection.isOpen()) {
            PdxpParser.PdxpPacket packet = connection.receivePdxpPacket();
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
            LOGGER.error("数据处理线程池已满，本次PDXP {}报文已拒绝，接口编号：{}",
                    transferName, config.getInterfaceId(), exception);
        }
    }

    /** 解析一个传输报文，并逐个处理其中的PDXP包。 */
    public void handle(ReceivedDatagram datagram) {
        List<PdxpParser.PdxpPacket> packets;
        try {
            // PDXP解析只在数据处理线程中执行。
            packets = PdxpParser.parseTransportUdp(datagram.getData());
        } catch (Exception exception) {
            // TODO 异常帧写入IoTDB的字段结构和路径确认后，保存完整原始报文。
            LOGGER.error("PDXP报文解析失败，异常帧暂未写入IoTDB，接口编号：{}",
                    datagram.getInterfaceConfig().getInterfaceId(), exception);
            return;
        }

        if (packets.isEmpty()) {
            // TODO 异常帧写入IoTDB的字段结构和路径确认后，保存无法形成完整PDXP包的原始报文。
            LOGGER.warn("PDXP报文结构不完整，异常帧暂未写入IoTDB，接口编号：{}",
                    datagram.getInterfaceConfig().getInterfaceId());
            return;
        }

        for (PdxpParser.PdxpPacket packet : packets) {
            // 一个UDP报文可能包含多帧，必须按解析出的完整PDXP帧逐个统计。
            statistics.recordCollection(datagram.getInterfaceConfig());
            handleNormalPacket(datagram.getInterfaceConfig(), packet);
        }
    }

    /** 完成业务处理后批量保存整帧和参数，再发送消息。 */
    private void handleNormalPacket(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        try {
            // 第一步：拆分自定义数据、遥测原始数据和遥测帧头。
            PdxpDataPayload payload = PdxpDataPayloadParser.parse(
                    packet.getData());
            // 第二步：rpc_enabled只决定正常PDXP帧后的业务处理分支。
            DataProcessor processor = Integer.valueOf(1).equals(
                    config.getRpcEnabled()) ? rpcProcessor : localProcessor;
            Optional<TelemetryMessage> processedMessage =
                    processor.process(config, packet, payload);
            if (!processedMessage.isPresent()) {
                // 没有最终Proto时不写IoTDB，避免产生缺少设备身份的孤立帧。
                LOGGER.info("遥测消息转换失败，当前帧不写入IoTDB，接口编号：{}，包序号：{}",
                        config.getInterfaceId(), packet.getSequenceNumber());
                return;
            }

            TelemetryMessage telemetryMessage = processedMessage.get();
            // 第三步：按每个遥测参数携带的校准公式执行野值检测。
            telemetryMessage = calibrationProcessingService.process(
                    config, telemetryMessage);
            // 第四步：校准完成后按最终保留参数统计真实野值数量。
            statistics.recordWildValue(config, telemetryMessage
                    .getTelemetriesMap().values().stream()
                    .filter(telemetry -> telemetry.getIsWildValue())
                    .count());
            // 第五步：单独隔离IoTDB入库异常，避免存储故障阻断消息发送链路。
            try {
                storageService.saveProcessedParameters(
                        config, telemetryMessage);
            } catch (Exception storageException) {
                // TODO 后续根据监控方案补充IoTDB入库失败告警和失败数据补偿机制。
                LOGGER.error("IoTDB入库失败，继续发送当前遥测消息，接口编号：{}，包序号：{}",
                        config.getInterfaceId(), packet.getSequenceNumber(), storageException);
            }
            // 第六步：无论IoTDB入库是否成功，都继续发送最终遥测Proto消息。
            messageService.send(telemetryMessage.toByteArray());
            // 第七步：全部处理步骤成功后，一帧只累计一次处理量。
            statistics.recordFrameProcessing(
                    config.getTaskId(), config.getInterfaceId());
        } catch (Exception exception) {
            // 处理失败时只记录日志，未生成Proto的整包不落入IoTDB。
            LOGGER.error("正常PDXP帧后续处理失败，接口编号：{}，包序号：{}",
                    config.getInterfaceId(), packet.getSequenceNumber(), exception);
        }
    }

}
