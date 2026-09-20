package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 遥测消息RocketMQ发送测试。 */
class TelemetryMessageServiceImplTest {

    /** 验证当前主流程向配置的RocketMQ主题和标签发送Proto原始消息。 */
    @Test
    void shouldSendFramedMessageToConfiguredDestination() {
        // 准备RocketMQ发送模板和包含主题、标签的遥测消息服务。
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        TelemetryMessageServiceImpl service =
                new TelemetryMessageServiceImpl(
                        rocketMQTemplate,
                        "telemetry-result",
                        "processed");
        SendResult sendResult = mock(SendResult.class);
        MessageQueue messageQueue = mock(MessageQueue.class);
        DefaultMQProducer producer = mock(DefaultMQProducer.class);
        // 补齐发送成功日志所读取的服务端返回信息。
        when(rocketMQTemplate.syncSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn(sendResult);
        when(rocketMQTemplate.getProducer()).thenReturn(producer);
        when(producer.getNamesrvAddr()).thenReturn("127.0.0.1:9876");
        when(sendResult.getMessageQueue()).thenReturn(messageQueue);
        TelemetryMessage message = TelemetryMessage.newBuilder()
                .setBussinessId("业务001")
                .build();

        // 直接发送完整遥测Proto消息，不再增加自定义消息头。
        byte[] protobufData = message.toByteArray();
        service.send(protobufData);

        // 捕获发送内容，确认目标和完整二进制数据均未发生变化。
        ArgumentCaptor<byte[]> payloadCaptor =
                ArgumentCaptor.forClass(byte[].class);
        verify(rocketMQTemplate).syncSend(
                org.mockito.ArgumentMatchers.eq(
                        "telemetry-result:processed"),
                payloadCaptor.capture());
        assertArrayEquals(protobufData, payloadCaptor.getValue());
        assertEquals(message.toByteArray().length, protobufData.length);
    }
}
