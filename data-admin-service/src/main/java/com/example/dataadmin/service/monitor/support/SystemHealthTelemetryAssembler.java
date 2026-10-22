package com.example.dataadmin.service.monitor.support;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import SatDataCenter.DataExchange.TmTc.Version.ExchangeTopicType;
import SatDataCenter.DataExchange.TmTc.Version.ProtoHeadInfo;
import SatDataCenter.DataExchange.TmTc.Version.SubTopicName;
import com.example.dataadmin.entity.SysMonitorSnapshot;
import com.example.dataadmin.enums.SystemHealthTelemetryParameter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** 将健康快照转换为设备遥测同款消息，不经过采集报文解析链路。 */
@Component
public class SystemHealthTelemetryAssembler {
    /** 快照中的本地时间按北京时间解释。 */
    private static final ZoneId SAMPLE_ZONE = ZoneId.of("Asia/Shanghai");

    /** 使用调用方已确定的任务与业务身份构造一帧完整消息。 */
    public TelemetryMessage assemble(SysMonitorSnapshot snapshot, String taskId,
            SubTopicName business, String businessId) {
        // 第一步：检查消息身份，避免发送缺少归属信息的遥测帧。
        if (snapshot == null || snapshot.getSampleTime() == null || blank(taskId)
                || business == null || business == SubTopicName.UNRECOGNIZED
                || business == SubTopicName.SUBTOPIC_UNSPECIFIED_TYPE || blank(businessId)) {
            throw new IllegalArgumentException("健康上报缺少采样时间、任务编号或业务标识");
        }
        Instant now = Instant.now();
        ProtoHeadInfo head = ProtoHeadInfo.newBuilder()
                .setTopicType(ExchangeTopicType.TEST_DATA_TYPE)
                .setBussiness(business)
                .setTaskId(taskId.trim())
                .setMsgSource("数据处理")
                .setMsgTime(timestamp(now))
                .build();
        TelemetryMessage.Builder message = TelemetryMessage.newBuilder()
                .setProtoHead(head)
                .setTime(timestamp(snapshot.getSampleTime().atZone(SAMPLE_ZONE).toInstant()))
                .setChannelName("sjcl")
                .setTmType(TelemetryType.TM_TYPE_DEVICE_TM)
                .setBussinessId(businessId.trim());

        // 第二步：按枚举顺序编码各参数，相同原码同时写入参数和整帧数据域。
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        for (SystemHealthTelemetryParameter definition : SystemHealthTelemetryParameter.values()) {
            BigDecimal value = definition.read(snapshot);
            BigInteger integer = value.multiply(BigDecimal.valueOf(definition.getScale())).toBigIntegerExact();
            byte[] raw = littleEndian(integer, definition.getBitWidth());
            frame.write(raw, 0, raw.length);
            Telemetry telemetry = Telemetry.newBuilder()
                    .setTableIndex(definition.getTableIndex())
                    .setTmSymbol(definition.getValue())
                    .setTmName(definition.getLabel())
                    .setSystemName("数据处理")
                    .setRawData(ByteString.copyFrom(raw))
                    .setValueType(ValueType.VALUE_TYPE_DOUBLE)
                    .setValue(value.doubleValue())
                    .setValueText(definition.valueText(snapshot, value))
                    .build();
            message.putTelemetries(definition.getValue(), telemetry);
        }
        // 第三步：所有参数完整且编码成功后才设置帧校验通过。
        return message.setFrameRawData(ByteString.copyFrom(frame.toByteArray()))
                .setFrameCheckStatus(true).build();
    }

    /** 无符号整数按低字节在前写入，不截断超出位宽的数值。 */
    private byte[] littleEndian(BigInteger value, int bitWidth) {
        // 先检查位宽，再逐字节写入固定长度的数据。
        if (value.signum() < 0 || value.bitLength() > bitWidth) {
            throw new IllegalArgumentException("健康参数原码超出位宽范围");
        }
        byte[] result = new byte[bitWidth / 8];
        for (int index = 0; index < result.length; index++) {
            result[index] = value.shiftRight(index * 8).byteValue();
        }
        return result;
    }

    /** 保留时间戳的秒和纳秒。 */
    private Timestamp timestamp(Instant value) {
        // 统一使用标准时间戳，避免手工换算造成精度丢失。
        return Timestamp.newBuilder().setSeconds(value.getEpochSecond()).setNanos(value.getNano()).build();
    }

    /** 检查必填的文本身份。 */
    private boolean blank(String value) {
        // 空值和纯空格均不能作为消息标识。
        return value == null || value.trim().isEmpty();
    }
}
