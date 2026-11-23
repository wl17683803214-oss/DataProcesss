package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.service.TelemetryMessageService;
import com.example.dataprocess.logging.PdxpFrameLogContext;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 遥测消息处理服务实现。
 */
@Service
public class TelemetryMessageServiceImpl implements TelemetryMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            TelemetryMessageServiceImpl.class);

    /** 成功日志与批量入库日志采用相同的五秒采样间隔。 */
    private static final long SUCCESS_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);
    /** 当前服务实例的所有接口共用采样窗口，首条成功消息可以立即输出。 */
    private final AtomicLong nextSuccessLogNanos = new AtomicLong(System.nanoTime());

    /** RocketMQ消息发送模板。 */
    private final RocketMQTemplate rocketMQTemplate;

    /** 当前遥测处理主流程的结果主题。 */
    private final String resultTopic;

    /** 当前遥测处理主流程的结果标签。 */
    private final String resultTag;

    public TelemetryMessageServiceImpl(
            RocketMQTemplate rocketMQTemplate,
            @Value("${data-processing.mq.result-topic}")
            String resultTopic,
            @Value("${data-processing.mq.result-tag}")
            String resultTag) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.resultTopic = resultTopic;
        this.resultTag = resultTag;
    }

    @Override
    public TelemetryMessage parse(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("单条遥测数据不能为空");
        }

        try {
            // 使用生成类自带的方法解析单条Protobuf遥测数据。
            return TelemetryMessage.parseFrom(data);
        } catch (InvalidProtocolBufferException exception) {
            throw new IllegalArgumentException("收到的数据不是有效的遥测消息", exception);
        }
    }

    @Override
    public void send(byte[] protobufData) {
        if (protobufData == null || protobufData.length == 0) {
            throw new IllegalArgumentException("待发送的遥测Proto消息体不能为空");
        }

        // 第一步：固定发送计时起点，组合主题和可选标签。
        long started = System.nanoTime();
        String destination = resultTag == null || resultTag.trim().isEmpty()
                ? resultTopic : resultTopic + ":" + resultTag;

        // 第二步：每条消息都正常发送，按采集接口主键选择固定消息队列。
        Long interfaceId = PdxpFrameLogContext.currentInterfaceId();
        SendResult sendResult = interfaceId == null
                ? rocketMQTemplate.syncSend(destination, protobufData)
                : rocketMQTemplate.syncSendOrderly(destination, protobufData, interfaceId.toString());
        // 有序链路只有得到成功确认才推进包序号，其他状态交给接口缓冲保留并重试。
        if (interfaceId != null && (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK)) {
            throw new IllegalStateException("遥测消息发送未获得成功确认");
        }

        // 第三步：确认成功后才竞争日志窗口，未采样的消息已经完成发送。
        long finished = System.nanoTime();
        long next = nextSuccessLogNanos.get();
        if (finished - next < 0L || !nextSuccessLogNanos.compareAndSet(
                next, finished + SUCCESS_LOG_INTERVAL_NANOS)) {
            return;
        }
        // 第四步：只格式化本次采样详情，发送耗时不包含排序缓冲等待和日志输出。
        LOGGER.info(
                "遥测消息发送成功，接口编号：{}，发送耗时：{}毫秒，名称服务：{}，目标主题：{}，目标标签：{}，"
                        + "消息标识：{}，偏移消息标识：{}，代理：{}，队列编号：{}，"
                        + "队列位点：{}，发送长度：{}字节",
                interfaceId,
                TimeUnit.NANOSECONDS.toMillis(finished - started),
                rocketMQTemplate.getProducer().getNamesrvAddr(),
                resultTopic,
                resultTag,
                sendResult.getMsgId(),
                sendResult.getOffsetMsgId(),
                sendResult.getMessageQueue().getBrokerName(),
                sendResult.getMessageQueue().getQueueId(),
                sendResult.getQueueOffset(),
                protobufData.length);
    }

}
