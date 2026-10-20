package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import com.example.common.enums.TelemetryFrameStatus;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.dataprocess.config.PdxpFramingProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.tool.PdxpParser;
import com.google.protobuf.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 将最终消息和完整采集帧保存到两张公共时序表。 */
@Service
@ConditionalOnProperty(prefix = "greptime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreptimeTelemetryStorageServiceImpl implements IoTDBTelemetryStorageService {
    private final GreptimeStore store;
    private final PdxpFramingProperties framingProperties;

    public GreptimeTelemetryStorageServiceImpl(GreptimeStore store, PdxpFramingProperties framingProperties) {
        this.store = store;
        this.framingProperties = framingProperties;
    }

    /** 旧入口缺少最终参数身份，继续保持不写入的行为。 */
    @Override
    public void savePdxpRawFrame(CollectInterfaceRuntimeConfig config, PdxpParser.PdxpPacket packet) {
        // 完整采集帧由正常批量入口或异常回退入口保存。
    }

    /** 旧入口没有完整采集报文，继续由统一入口保存。 */
    @Override
    public void saveRawFrame(Long interfaceId, TelemetryMessage message) {
        // 不能用去头后的消息原码替代完整采集报文。
    }

    /** 身份和数据域完整的本地解析失败帧仍允许单独保存。 */
    @Override
    public void markPdxpRawFrameAbnormal(CollectInterfaceRuntimeConfig config, PdxpParser.PdxpPacket packet) {
        // 第一步：保留本地异常回退的身份和报文校验，不将远程失败改写成本地帧。
        requireConfig(config);
        if (packet == null || Integer.valueOf(1).equals(config.getRpcEnabled())) {
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
        long time = packet.getSendDate().atStartOfDay().plus(packet.getTimeSinceMidnight())
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
        // 第三步：异常帧仅写整帧表，不产生参数或消息。
        TelemetryFrameRecord frame = frame(config, time, TelemetrySourceType.LOCAL, identity,
                TelemetryType.TM_TYPE_DEVICE_TM, deviceName, deviceName.substring(0, separator).trim(),
                packet.getRawPacket(), TelemetryFrameStatus.ABNORMAL);
        store.save(frame, Collections.emptyList());
    }

    /** 固定一次消息的全部时间和身份，再交给公共组件进行分批写入和有限重试。 */
    @Override
    public void saveProcessedParameters(CollectInterfaceRuntimeConfig config,
            TelemetryMessage message, byte[] rawFrame) {
        // 第一步：写入必须有真实任务、接口和完整采集报文。
        requireConfig(config);
        if (message == null || !message.hasProtoHead() || !config.getTaskId().equals(message.getProtoHead().getTaskId())) {
            throw new IllegalArgumentException("遥测消息缺少任务或与采集接口任务不一致");
        }
        if (rawFrame == null || rawFrame.length == 0) {
            throw new IllegalArgumentException("遥测整帧写入缺少完整采集原码");
        }
        boolean remote = Integer.valueOf(1).equals(config.getRpcEnabled());
        TelemetrySourceType source = remote ? TelemetrySourceType.RPC : TelemetrySourceType.LOCAL;
        String identity = remote ? TelemetryIdentity.rpc(message.getSatCode(), message.getChannelCode())
                : TelemetryIdentity.local(config.getDeviceSatelliteId());
        long time = sourceTimeMillis(message);
        java.sql.Timestamp processedAt = new java.sql.Timestamp(System.currentTimeMillis());
        // 第二步：帧检查状态与参数告警独立，本地正常结果保持整帧正常。
        TelemetryFrameStatus status = remote && !message.getFrameCheckStatus()
                ? TelemetryFrameStatus.ABNORMAL : TelemetryFrameStatus.NORMAL;
        TelemetryFrameRecord frame = frame(config, time, source, identity, message.getTmType(),
                remote ? displayName(message.getSatName(), message.getSatCode()) : config.getDeviceSatelliteName(),
                remote ? displayName(message.getChannelName(), message.getChannelCode()) : message.getChannelName(),
                rawFrame, status);
        // 第三步：复用协议已有枚举，保存英文枚举名，不重复创建值类型和状态枚举。
        List<TelemetryParameterRecord> rows = new ArrayList<>(message.getTelemetriesCount());
        for (Map.Entry<String, Telemetry> entry : message.getTelemetriesMap().entrySet()) {
            Telemetry telemetry = entry.getValue();
            TelemetryParameterRecord row = new TelemetryParameterRecord();
            row.setSourceTime(new java.sql.Timestamp(time));
            row.setTaskId(config.getTaskId());
            row.setSourceType(source.getValue());
            row.setSourceIdentity(identity);
            row.setTelemetryType(message.getTmType().name());
            row.setTelemetryCode(telemetry.getTmSymbol().trim().isEmpty() ? entry.getKey() : telemetry.getTmSymbol());
            row.setTelemetryName(telemetry.getTmName());
            row.setValueType(telemetry.getValueType().name());
            row.setValue(telemetry.getValue());
            row.setValueText(telemetry.getValueText());
            row.setRaw(telemetry.getRawData().toByteArray());
            row.setStateName(telemetry.getStateName());
            row.setStateType(telemetry.getStateType().name());
            row.setStateIndex(telemetry.getStateIndex());
            row.setProcessedAt(processedAt);
            rows.add(row);
        }
        // 第四步：存储组件按稳定主键重试，失败沿用现有异步告警链路。
        store.save(frame, rows);
    }

    /** 两种来源和正常异常帧共用相同字段，完整原码始终保持二进制。 */
    private TelemetryFrameRecord frame(CollectInterfaceRuntimeConfig config, long time,
            TelemetrySourceType source, String identity, TelemetryType type,
            String deviceName, String channelName, byte[] raw, TelemetryFrameStatus status) {
        TelemetryFrameRecord frame = new TelemetryFrameRecord();
        frame.setSourceTime(new java.sql.Timestamp(time));
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
    private void requireConfig(CollectInterfaceRuntimeConfig config) {
        if (config == null || config.getTaskId() == null || config.getTaskId().trim().isEmpty()
                || config.getInterfaceId() == null || config.getInterfaceId() <= 0) {
            throw new IllegalArgumentException("遥测写入缺少有效任务或采集接口");
        }
    }

    /** 无源时间时沿用当前时间的既有回退规则。 */
    private long sourceTimeMillis(TelemetryMessage message) {
        if (!message.hasTime()) {
            return System.currentTimeMillis();
        }
        Timestamp time = message.getTime();
        return time.getSeconds() * 1000L + time.getNanos() / 1000000L;
    }

    /** 远程名称缺失时显示返回编码。 */
    private String displayName(String name, String code) {
        return name == null || name.trim().isEmpty() ? code : name;
    }

    /** 文本空值沿用原存储的空串约定。 */
    private String text(String value) {
        return value == null ? "" : value;
    }
}
