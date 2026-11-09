package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.common.greptime.GreptimeStore;
import com.example.common.telemetry.TelemetryRealtimeEvent;
import com.example.dataprocess.config.PdxpFramingProperties;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.service.TelemetryRecordFactory;
import com.example.dataprocess.tool.PdxpParser;
import java.sql.Timestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 将最终遥测参数和完整采集帧保存到所属设备或卫星的对象分表。 */
@Service
@ConditionalOnProperty(prefix = "greptime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreptimeTelemetryStorageServiceImpl implements IoTDBTelemetryStorageService {
    private final GreptimeStore store;
    private final PdxpFramingProperties framingProperties;

    public GreptimeTelemetryStorageServiceImpl(GreptimeStore store, PdxpFramingProperties framingProperties) {
        this.store = store;
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
        // 统一校验本地回退帧，保留独立的入库重试链路。
        TelemetryRealtimeEvent event = TelemetryRecordFactory.abnormal(config, packet, framingProperties,
                new Timestamp(System.currentTimeMillis()));
        store.save(event.getFrame(), event.getParameters());
    }

    @Override
    public void saveProcessedParameters(CollectInterfaceRuntimeConfig config, TelemetryMessage message, byte[] rawFrame) {
        // 同帧的完整原码和参数使用同一个入库时间，分批重试复用该时间。
        TelemetryRealtimeEvent event = TelemetryRecordFactory.processed(config, message, rawFrame,
                new Timestamp(System.currentTimeMillis()), true);
        store.save(event.getFrame(), event.getParameters());
    }
}
