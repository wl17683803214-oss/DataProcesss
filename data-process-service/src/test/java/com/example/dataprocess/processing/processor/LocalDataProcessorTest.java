package com.example.dataprocess.processing.processor;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.StateType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import SatDataCenter.DataExchange.TmTc.Version.ExchangeTopicType;
import SatDataCenter.DataExchange.TmTc.Version.SubTopicName;
import com.example.dataprocess.collection.CollectInterfaceStatistics;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.protocol.rpc.PdxpProtocolField;
import com.example.dataprocess.protocol.rpc.PdxpDataPayload;
import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import com.example.dataprocess.protocol.rpc.ProtocolConfigParser;
import com.example.dataprocess.tool.PdxpParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** 验证协议配置字段的连续取位、消息组装和状态判断。 */
class LocalDataProcessorTest {
    /** 使用真实解析器准备接口启动时已经加载的字段快照。 */
    private final ProtocolConfigParser configParser =
            new ProtocolConfigParser(new ObjectMapper());
    /** 本地处理只读取接口配置快照。 */
    private final LocalDataProcessor processor =
            new LocalDataProcessor(mock(CollectInterfaceStatistics.class));

    /** 验证排序、跨字节读取及基础Protobuf字段映射。 */
    @Test
    void shouldMapSequentialBitsToTelemetry() throws Exception {
        TelemetryMessage message = process(
                fields(field("2", "9", "电压"), field("1", "3", "温度"), field("3", "4", "电流")),
                (byte) 0xCD, (byte) 0xAB);
        Telemetry voltage = message.getTelemetriesOrThrow("电压");
        assertEquals(5, message.getTelemetriesOrThrow("温度").getValue());
        assertEquals(377, voltage.getValue());
        assertEquals(2, voltage.getTableIndex());
        assertEquals("电压", voltage.getTmName());
        assertEquals("x*0.1", voltage.getCalibrationFormula());
        assertEquals(ValueType.VALUE_TYPE_RAW, voltage.getValueType());
        assertArrayEquals(new byte[]{0x79, 0x01}, voltage.getRawData().toByteArray());
        assertEquals(ExchangeTopicType.TEST_DATA_TYPE,
                message.getProtoHead().getTopicType());
        assertEquals(SubTopicName.DATA_SUBSYSTEM_CSCL_PHYVALUE,
                message.getProtoHead().getBussiness());
        assertTrue(message.getProtoHead().hasMsgTime());
        assertEquals("TASK-7", message.getProtoHead().getTaskId());
        assertEquals("数据处理", message.getProtoHead().getMsgSource());
        assertEquals("设备通道", message.getChannelName());
        assertEquals("", message.getSatCode());
        assertEquals("业务001", message.getBussinessId());
        assertEquals(TelemetryType.TM_TYPE_DEVICE_TM, message.getTmType());
        assertArrayEquals(new byte[]{(byte) 0xCD, (byte) 0xAB},
                message.getFrameRawData().toByteArray());
    }

    /** 验证命中预警范围时写正常类型和从零开始的状态索引。 */
    @Test
    void shouldSetNormalStateAndIndex() throws Exception {
        String item = alarmField("加电:[41,43]/断电:[-1,1]/过程:[1,41]", "");
        Telemetry telemetry = process(fields(item), (byte) 42).getTelemetriesOrThrow("电源");
        assertEquals(StateType.STATE_TYPE_NORMAL, telemetry.getStateType());
        assertEquals(0, telemetry.getStateIndex());
        assertEquals("加电", telemetry.getStateName());
    }

    /** 验证未命中时报警，预警为空时使用正常范围。 */
    @Test
    void shouldAlarmOrFallbackToNormalRange() throws Exception {
        Telemetry alarm = process(fields(alarmField("加电:[41,43]", "")), (byte) 50)
                .getTelemetriesOrThrow("电源");
        assertEquals(StateType.STATE_TYPE_ALARM, alarm.getStateType());
        Telemetry normal = process(fields(alarmField("", "关闭:[0,0]/开启:[1,1]")), (byte) 1)
                .getTelemetriesOrThrow("电源");
        assertEquals(StateType.STATE_TYPE_NORMAL, normal.getStateType());
        assertEquals(1, normal.getStateIndex());
        assertEquals("开启", normal.getStateName());
    }

    /** 执行实际PDXP数据域解析。 */
    private TelemetryMessage process(String params, byte... data) throws Exception {
        byte[] bytes = new byte[data.length + 9];
        Arrays.fill(bytes, (byte) 0xFF);
        System.arraycopy(data, 0, bytes, 5, data.length);
        PdxpDataPayload payload = PdxpDataPayloadParser.parse(bytes);
        CollectInterfaceRuntimeConfig config = new CollectInterfaceRuntimeConfig();
        config.setTaskId("TASK-7");
        config.setProtocolConfigId(21L);
        config.setProtocolConfigParams("{\"deviceSatelliteId\":12}");
        config.setDeviceSatelliteId(12L);
        config.setDeviceSatelliteName("设备通道_业务001");
        config.setPdxpFields(configParser.parseFields(params, PdxpProtocolField.class));
        byte[] header = new byte[PdxpParser.HEADER_LENGTH];
        header[0] = (byte) 0x80;
        header[24] = 1;
        return processor.process(config, PdxpParser.parseUdp(header).get(0), payload).get();
    }

    /** 构造fields根对象。 */
    private String fields(String... items) {
        return "{\"fields\":[" + String.join(",", items) + "]}";
    }

    /** 构造普通字段。 */
    private String field(String index, String width, String code) {
        return "{\"id\":81,\"tableIndex\":\"" + index + "\",\"bitWidth\":\"" + width
                + "\",\"telemetryName\":\"" + code + "\",\"telemetryCode\":\"" + code
                + "\",\"alarmFlag\":\"0\",\"normalValue\":\"\",\"warningValue\":\"\","
                + "\"calibrationFormula\":\"x*0.1\"}";
    }

    /** 构造需要状态判断的字段。 */
    private String alarmField(String warning, String normal) {
        return field("1", "8", "电源")
                .replace("\"alarmFlag\":\"0\"", "\"alarmFlag\":\"1\"")
                .replace("\"warningValue\":\"\"", "\"warningValue\":\"" + warning + "\"")
                .replace("\"normalValue\":\"\"", "\"normalValue\":\"" + normal + "\"");
    }

}
