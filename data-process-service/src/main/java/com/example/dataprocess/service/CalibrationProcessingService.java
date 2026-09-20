package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;

/** 按遥测参数携带的校准公式执行野值检测。 */
public interface CalibrationProcessingService {
    /** 使用接口预加载的校准配置检测野值并缓存野值详情。 */
    TelemetryMessage process(
            CollectInterfaceRuntimeConfig interfaceConfig,
            TelemetryMessage message);
}
