package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import com.example.common.enums.TelemetryFrameStatus;
import com.example.common.enums.CollectionDataType;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.dataprocess.config.PdxpFramingProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import com.example.dataprocess.tool.PdxpParser;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.example.common.telemetry.TelemetryRealtimeEvent;

/** 复用遥测入库与推送的身份、帧状态和参数转换规则。 */
public final class TelemetryRecordFactory {
    private TelemetryRecordFactory() {
    }

    /** 复用本地异常帧的校验，仅组装完整帧。 */
    public static TelemetryRealtimeEvent abnormal(CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet, PdxpFramingProperties framingProperties, Timestamp writeTime) {
        // 第一步：保留本地异常回退的身份和报文校验，不将远程失败改写成本地帧。
        requireConfig(config);
        if (packet == null || CollectionDataType.require(config.getRpcEnabled()) != CollectionDataType.DEVICE) {
            throw new IllegalArgumentException("异常整帧回退需要有效的本地处理报文");
        }
        String identity = TelemetryIdentity.local(config.getDeviceSatelliteId());
        String deviceName = text(config.getDeviceSatelliteName());
        int separator = deviceName.indexOf('_');
        if (separator <= 0 || separator == deviceName.length() - 1
                || deviceName.substring(0, separator).trim().isEmpty()
                || deviceName.substring(separator + 1).trim().isEmpty()) {
            throw new IllegalArgumentException("异常整帧缺少有效的设备名称和通道名称：" + deviceName);
        }
        // 第二步：复用当前按本地和远程分别配置的附加头长度。
        PdxpFramingProperties.Framing framing = framingProperties.select(config.getRpcEnabled());
        PdxpDataPayloadParser.parse(packet.getData(), framing.getCustomDataLength(), framing.getTelemetryHeaderLength());
        // 第三步：异常帧仅写整帧表，不产生参数或消息。
        TelemetryFrameRecord frame = frame(config, writeTime, TelemetrySourceType.LOCAL, identity,
                TelemetryType.TM_TYPE_DEVICE_TM, deviceName, deviceName.substring(0, separator).trim(),
                packet.getRawPacket(), TelemetryFrameStatus.ABNORMAL);
        return new TelemetryRealtimeEvent(frame, Collections.emptyList());
    }

    /** 从解析结果组装帧和参数，实时展示省略参数原码。 */
    public static TelemetryRealtimeEvent processed(CollectInterfaceRuntimeConfig config,
            TelemetryMessage message, byte[] rawFrame, Timestamp writeTime, boolean includeParameterRaw) {
        // 第一步：写入必须有真实任务、接口和完整采集报文。
        requireConfig(config);
        if (message == null || !message.hasProtoHead() || !config.getTaskId().equals(message.getProtoHead().getTaskId())) {
            throw new IllegalArgumentException("遥测消息缺少任务或与采集接口任务不一致");
        }
        if (rawFrame == null || rawFrame.length == 0) {
            throw new IllegalArgumentException("遥测整帧写入缺少完整采集原码");
        }
        boolean remote = CollectionDataType.require(config.getRpcEnabled()) == CollectionDataType.SATELLITE;
        TelemetrySourceType source = remote ? TelemetrySourceType.RPC : TelemetrySourceType.LOCAL;
        // 远程身份只使用返回消息的卫星代号，本地继续使用设备卫星主键。
        String identity = remote ? TelemetryIdentity.rpc(message.getSatCode())
                : TelemetryIdentity.local(config.getDeviceSatelliteId());
        // 第二步：帧检查状态与参数告警独立，本地正常结果保持整帧正常。
        TelemetryFrameStatus status = remote && !message.getFrameCheckStatus()
                ? TelemetryFrameStatus.ABNORMAL : TelemetryFrameStatus.NORMAL;
        TelemetryFrameRecord frame = frame(config, writeTime, source, identity, message.getTmType(),
                remote ? displayName(message.getSatName(), message.getSatCode()) : config.getDeviceSatelliteName(),
                remote ? displayName(message.getChannelName(), message.getChannelCode()) : message.getChannelName(),
                rawFrame, status);
        // 第三步：复用协议已有枚举，保存英文枚举名，不重复创建值类型和状态枚举。
        List<TelemetryParameterRecord> rows = new ArrayList<>(message.getTelemetriesCount());
        for (Map.Entry<String, Telemetry> entry : message.getTelemetriesMap().entrySet()) {
            Telemetry telemetry = entry.getValue();
            TelemetryParameterRecord row = new TelemetryParameterRecord();
            row.setSourceTime(writeTime);
            row.setTaskId(config.getTaskId());
            row.setSourceType(source.getValue());
            row.setSourceIdentity(identity);
            row.setTelemetryType(message.getTmType().name());
            row.setTelemetryCode(telemetry.getTmSymbol().trim().isEmpty() ? entry.getKey() : telemetry.getTmSymbol());
            row.setTelemetryName(telemetry.getTmName());
            row.setValueType(telemetry.getValueType().name());
            // 二进制保留项和文本没有有效数值，写空值，避免曲线出现虚假的零点。
            boolean withoutNumber = telemetry.getValueType()
                    == SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType.VALUE_TYPE_BINARY
                    || telemetry.getValueType()
                    == SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType.VALUE_TYPE_TEXT;
            row.setValue(withoutNumber ? null : telemetry.getValue());
            row.setValueText(telemetry.getValueText());
            // 完整参数原码只随持久化记录写入。
            if (includeParameterRaw) {
                row.setRaw(telemetry.getRawData().toByteArray());
            }
            row.setStateName(telemetry.getStateName());
            row.setStateType(telemetry.getStateType().name());
            row.setStateIndex(telemetry.getStateIndex());
            row.setProcessedAt(writeTime);
            rows.add(row);
        }
        return new TelemetryRealtimeEvent(frame, rows);
    }

    /** 整帧统一保存本次入库时间和完整二进制原码。 */
    private static TelemetryFrameRecord frame(CollectInterfaceRuntimeConfig config, Timestamp writeTime,
            TelemetrySourceType source, String identity, TelemetryType type,
            String deviceName, String channelName, byte[] raw, TelemetryFrameStatus status) {
        TelemetryFrameRecord frame = new TelemetryFrameRecord();
        frame.setSourceTime(writeTime);
        frame.setTaskId(config.getTaskId());
        frame.setInterfaceId(config.getInterfaceId());
        frame.setSourceType(source.getValue());
        frame.setSourceIdentity(identity);
        frame.setTelemetryType(type.name());
        frame.setDeviceSatelliteName(text(deviceName));
        frame.setChannelName(text(channelName));
        frame.setRaw(raw);
        frame.setCheckStatus(status.getValue());
        return frame;
    }

    /** 不使用占位任务或接口写入，避免不同数据源混在一起。 */
    private static void requireConfig(CollectInterfaceRuntimeConfig config) {
        if (config == null || config.getTaskId() == null || config.getTaskId().trim().isEmpty()
                || config.getInterfaceId() == null || config.getInterfaceId() <= 0) {
            throw new IllegalArgumentException("遥测写入缺少有效任务或采集接口");
        }
    }

    /** 远程名称缺失时显示返回编码。 */
    private static String displayName(String name, String code) {
        return name == null || name.trim().isEmpty() ? code : name;
    }

    /** 文本空值沿用原存储的空串约定。 */
    private static String text(String value) {
        return value == null ? "" : value;
    }
}
