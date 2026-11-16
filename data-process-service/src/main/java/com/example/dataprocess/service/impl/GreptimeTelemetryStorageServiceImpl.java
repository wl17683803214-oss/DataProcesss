package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.common.telemetry.TelemetryRealtimeEvent;
import com.example.dataprocess.config.PdxpFramingProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.service.TelemetryRecordFactory;
import com.example.dataprocess.service.GreptimeWriteBuffer;
import com.example.dataprocess.tool.PdxpParser;
import java.sql.Timestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 将最终遥测参数和完整采集帧保存到所属设备或卫星的对象分表。 */
@Service
@ConditionalOnProperty(prefix = "greptime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreptimeTelemetryStorageServiceImpl implements IoTDBTelemetryStorageService {
    private final GreptimeWriteBuffer buffer;
    private final PdxpFramingProperties framingProperties;

    public GreptimeTelemetryStorageServiceImpl(GreptimeWriteBuffer buffer, PdxpFramingProperties framingProperties) {
        this.buffer = buffer;
        this.framingProperties = framingProperties;
    }

    @Override
    public void savePdxpRawFrame(CollectInterfaceRuntimeConfig config, PdxpParser.PdxpPacket packet) {
        // 完整采集帧由正常批量入口或异常回退入口保存。
    }

    @Override
    public void saveRawFrame(Long interfaceId, TelemetryMessage message) {
        // 不能用去头后的消息原码替代完整采集报文。
    }

    @Override
    public void markPdxpRawFrameAbnormal(CollectInterfaceRuntimeConfig config, PdxpParser.PdxpPacket packet) {
        // 统一校验本地回退帧，和同一对象的正常帧一起进入有界合并缓冲。
        TelemetryRealtimeEvent event = TelemetryRecordFactory.abnormal(config, packet, framingProperties,
                new Timestamp(System.currentTimeMillis()));
        buffer.offer(config, event);
    }

    @Override
    public void saveProcessedParameters(CollectInterfaceRuntimeConfig config, TelemetryMessage message, byte[] rawFrame) {
        // 在等待合并之前固定处理时间，跨帧刷新和失败重试均复用该时间。
        TelemetryRealtimeEvent event = TelemetryRecordFactory.processed(config, message, rawFrame,
                new Timestamp(System.currentTimeMillis()), true);
        buffer.offer(config, event);
    }
}
