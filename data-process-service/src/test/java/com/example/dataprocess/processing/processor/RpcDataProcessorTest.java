package com.example.dataprocess.processing.processor;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import SatDataCenter.DataExchange.TmTc.Version.ExchangeTopicType;
import SatDataCenter.DataExchange.TmTc.Version.SubTopicName;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.protocol.rpc.PdxpDataPayload;
import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import com.example.dataprocess.protocol.rpc.ProtocolConfigParser;
import com.example.dataprocess.protocol.rpc.TelemetryRpcClient;
import com.example.dataprocess.tool.PdxpParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tm.processing.v1.Tm;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RPC遥测处理流程测试。 */
class RpcDataProcessorTest {

    /** 验证RPC请求使用遥测头和中间遥测原码组成完整数据。 */
    @Test
    void shouldUseTelemetryFrameAsRequestData() throws Exception {
        TelemetryRpcClient rpcClient = mock(TelemetryRpcClient.class);
        // RPC返回一条合法的空遥测消息。
        when(rpcClient.process(any())).thenReturn(
                TelemetryMessage.newBuilder().build().toByteArray());
        RpcDataProcessor processor = processor(rpcClient);
        PdxpDataPayload payload = payload(
                new byte[]{0x11, 0x22},
                new byte[]{0x01, 0x02, 0x03, 0x04});

        assertTrue(processor.process(
                interfaceConfig(), packet(), payload).isPresent());

        ArgumentCaptor<Tm.FrameRequest> requestCaptor =
                ArgumentCaptor.forClass(Tm.FrameRequest.class);
        verify(rpcClient).process(requestCaptor.capture());
        Tm.FrameRequest request = requestCaptor.getValue();
        assertArrayEquals(
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x11, 0x22},
                request.getData().toByteArray());
        assertEquals(6, request.getLength());
        assertEquals("SAT-001", request.getSatCode());
        assertEquals("CH-01", request.getChannel());
        assertEquals("遥测", request.getDataSource());
    }

    /** 验证协议字段中的数字配置值会转换为RPC需要的字符串。 */
    @Test
    void shouldConvertProtocolFieldValueToString() throws Exception {
        TelemetryRpcClient rpcClient = mock(TelemetryRpcClient.class);
        // 返回合法消息以便检查已经发出的请求。
        when(rpcClient.process(any())).thenReturn(
                TelemetryMessage.newBuilder().build().toByteArray());
        RpcDataProcessor processor = processor(rpcClient);
        CollectInterfaceRuntimeConfig config = new CollectInterfaceRuntimeConfig();
        config.setTaskId("TASK-7");
        config.setProtocolConfigParams("{\"fields\":["
                + "{\"field\":\"sat_code\",\"value\":1001}]}");

        processor.process(config, packet(), payload(new byte[]{0x01}, new byte[4]));
        ArgumentCaptor<Tm.FrameRequest> requestCaptor =
                ArgumentCaptor.forClass(Tm.FrameRequest.class);
        verify(rpcClient).process(requestCaptor.capture());

        Tm.FrameRequest request = requestCaptor.getValue();
        assertEquals("1001", request.getSatCode());
        assertEquals("", request.getChannel());
        assertEquals("", request.getDataSource());
    }

    /** 验证RPC返回非法遥测消息时结束当前帧处理。 */
    @Test
    void shouldRejectInvalidTelemetryMessage() throws Exception {
        TelemetryRpcClient rpcClient = mock(TelemetryRpcClient.class);
        // 非法字节不能解析成遥测Proto消息。
        when(rpcClient.process(any())).thenReturn(new byte[]{(byte) 0xFF});
        RpcDataProcessor processor = processor(rpcClient);

        assertFalse(processor.process(
                interfaceConfig(), packet(),
                payload(new byte[]{0x01}, new byte[4])).isPresent());
    }

    /** 验证RPC返回的遥测参数可以完整解析并返回。 */
    @Test
    void shouldParseTelemetryMessageReturnedByRpc() throws Exception {
        TelemetryMessage response = TelemetryMessage.newBuilder()
                .putTelemetries("TMK1001", Telemetry.newBuilder()
                        .setTableIndex(219)
                        .setTmSymbol("TMK1001")
                        .setTmName("电压")
                        .setValue(42.0D)
                        .setValueText("42")
                        .setValueType(ValueType.VALUE_TYPE_RAW)
                        .build())
                .build();
        TelemetryRpcClient rpcClient = mock(TelemetryRpcClient.class);
        // RPC客户端直接返回完整遥测消息的序列化结果。
        when(rpcClient.process(any())).thenReturn(response.toByteArray());
        RpcDataProcessor processor = processor(rpcClient);

        TelemetryMessage result = processor.process(
                interfaceConfig(), packet(),
                payload(new byte[]{0x01}, new byte[4])).get();

        Telemetry telemetry = result.getTelemetriesOrThrow("TMK1001");
        assertEquals(219, telemetry.getTableIndex());
        assertEquals("电压", telemetry.getTmName());
        assertEquals(42.0D, telemetry.getValue());
        assertEquals(ValueType.VALUE_TYPE_RAW, telemetry.getValueType());
        assertEquals(ExchangeTopicType.TEST_DATA_TYPE,
                result.getProtoHead().getTopicType());
        assertEquals(SubTopicName.DATA_SAT_PHYVALUE,
                result.getProtoHead().getBussiness());
        assertTrue(result.getProtoHead().hasMsgTime());
        assertEquals("TASK-7", result.getProtoHead().getTaskId());
        assertEquals("数据处理", result.getProtoHead().getMsgSource());
    }

    /** 创建使用真实配置解析器的RPC处理器。 */
    private RpcDataProcessor processor(TelemetryRpcClient rpcClient) {
        return new RpcDataProcessor(
                rpcClient, new ProtocolConfigParser(new ObjectMapper()));
    }

    /** 创建测试使用的采集接口配置。 */
    private CollectInterfaceRuntimeConfig interfaceConfig() {
        CollectInterfaceRuntimeConfig config =
                new CollectInterfaceRuntimeConfig();
        config.setInterfaceId(11L);
        config.setTaskId("TASK-7");
        config.setProtocolConfigParams("{\"fields\":["
                + "{\"field\":\"channel\",\"value\":\"CH-01\"},"
                + "{\"field\":\"data_source\",\"value\":\"遥测\"},"
                + "{\"field\":\"sat_code\",\"value\":\"SAT-001\"}]}");
        return config;
    }

    /** 创建只提供发送时间和序号的最小PDXP包。 */
    private PdxpParser.PdxpPacket packet() throws Exception {
        byte[] datagram = new byte[PdxpParser.HEADER_LENGTH];
        datagram[0] = (byte) 0x80;
        datagram[24] = 0x01;
        List<PdxpParser.PdxpPacket> packets = PdxpParser.parseUdp(datagram);
        return packets.get(0);
    }

    /** 创建包含自定义头、中间遥测原码和遥测头的数据域。 */
    private PdxpDataPayload payload(byte[] data, byte[] telemetryHeader) {
        byte[] bytes = new byte[5 + data.length + telemetryHeader.length];
        // 前五字节保留为测试自定义头，中间和末尾分别写入两段数据。
        System.arraycopy(data, 0, bytes, 5, data.length);
        System.arraycopy(telemetryHeader, 0, bytes, 5 + data.length,
                telemetryHeader.length);
        return PdxpDataPayloadParser.parse(bytes);
    }
}
