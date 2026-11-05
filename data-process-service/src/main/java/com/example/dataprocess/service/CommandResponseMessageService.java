package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelecommandMessages.CommandExecResultMsg;
import com.example.dataprocess.config.CommandResponseProperties;
import com.example.dataprocess.protocol.command.CommandResponseResult;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 将已组装的单指令响应消息发送到配置的指令结果主题。 */
@Service
public class CommandResponseMessageService {
    /** 指令响应消息发送日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandResponseMessageService.class);
    /** 复用遥测链路已经配置的消息发送模板。 */
    private final RocketMQTemplate rocketMQTemplate;
    /** 指令响应消息主题。 */
    private final CommandResponseProperties properties;

    /** 注入消息模板及指令响应配置。 */
    public CommandResponseMessageService(RocketMQTemplate rocketMQTemplate,
            CommandResponseProperties properties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
    }

    /** 在已有消息线程池内发送原始消息字节，发送异常由调度器统一告警。 */
    public void send(CommandExecResultMsg message, Long interfaceId, long sequenceNumber) {
        // 第一步：发送五字段消息的协议缓冲区字节，主题独立于遥测结果主题。
        SendResult sendResult = rocketMQTemplate.syncSend(properties.getTopic(), message.toByteArray());
        // 第二步：代理未确认成功时抛出异常，交给现有消息失败告警处理。
        if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
            throw new IllegalStateException("指令响应消息发送未获得成功确认");
        }
        // 第三步：结果用中文展示，消息标识用于联调定位和发送结果追踪。
        LOGGER.info("指令响应消息发送成功，接口编号：{}，包序号：{}，卫星代号：{}，"
                        + "指令代号：{}，指令标识：{}，指令结果：{}，目标主题：{}，消息标识：{}",
                interfaceId, sequenceNumber, message.getSatCode(), message.getCmSymbol(),
                message.getCommandId(),
                CommandResponseResult.fromProtoResult(message.getSendingResult()).getLabel(),
                properties.getTopic(), sendResult.getMsgId());
    }
}
