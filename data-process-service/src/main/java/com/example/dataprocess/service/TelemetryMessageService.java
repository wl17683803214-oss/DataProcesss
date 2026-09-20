package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;

/**
 * 遥测消息处理服务。
 */
public interface TelemetryMessageService {

    /**
     * 将单条 Protobuf 数据解析成遥测消息对象。
     *
     * @param data 单条 Protobuf 遥测数据
     * @return 解析后的遥测消息对象
     */
    TelemetryMessage parse(byte[] data);

    /**
     * 将遥测Proto消息体发送到RocketMQ结果主题。
     *
     * @param protobufData 遥测Proto消息体
     */
    void send(byte[] protobufData);
}
