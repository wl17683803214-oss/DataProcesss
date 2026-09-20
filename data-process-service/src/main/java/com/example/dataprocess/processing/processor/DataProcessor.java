package com.example.dataprocess.processing.processor;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.protocol.rpc.PdxpDataPayload;
import com.example.dataprocess.tool.PdxpParser;

import java.util.Optional;

/** 正常PDXP帧的统一数据处理器接口。 */
public interface DataProcessor {

    /**
     * 处理正常PDXP帧，并在转换规则已经实现时返回统一遥测消息。
     *
     * <p>转换规则尚未实现时返回空，调用方不会继续存储参数或发送消息。</p>
     */
    Optional<TelemetryMessage> process(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet,
            PdxpDataPayload payload);
}
