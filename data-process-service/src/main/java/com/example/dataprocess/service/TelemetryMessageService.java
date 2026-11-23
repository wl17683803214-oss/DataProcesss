package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /** 计算一条遥测消息在原生批次中的编码长度，供排序缓冲拆批。 */
    long encodedBatchEntrySize(int payloadBytes);

    /** 读取生产者允许的消息体字节数上限。 */
    int producerMaxMessageBytes();

    /** 按接口选择队列，异步发送已排序的遥测消息，成功确认后完成结果。 */
    CompletableFuture<Void> sendOrderedAsync(Long interfaceId, List<TelemetryMessage> messages);
}
