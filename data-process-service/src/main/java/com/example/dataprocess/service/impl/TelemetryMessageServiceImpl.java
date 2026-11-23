package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.service.TelemetryMessageService;
import com.example.dataprocess.logging.PdxpFrameLogContext;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.apache.rocketmq.common.message.MessageQueue;
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
    /** 每条原生批次记录的固定字段、标签及客户端标识所占字节数。 */
    private final int batchEntryOverhead;

    public TelemetryMessageServiceImpl(
            RocketMQTemplate rocketMQTemplate,
            @Value("${data-processing.mq.result-topic}")
            String resultTopic,
            @Value("${data-processing.mq.result-tag}")
            String resultTag) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.resultTopic = resultTopic;
        this.resultTag = resultTag;
        // 使用客户端编码计算空消息的开销，无需在排序扫描中重复序列化。
        Message sample = new Message(resultTopic, resultTag, new byte[0]);
        MessageClientIDSetter.setUniqID(sample);
        this.batchEntryOverhead = MessageDecoder.encodeMessage(sample).length;
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
        // 单条与批量成功共用采样窗口。
        logSuccess(interfaceId, started, System.nanoTime(), sendResult, 1, protobufData.length);
    }

    /** 按编码后的记录计算批次长度，保留原始消息体的所有字节。 */
    @Override
    public long encodedBatchEntrySize(int payloadBytes) {
        return (long) payloadBytes + batchEntryOverhead;
    }

    /** 复用当前生产者真实生效的消息体限制。 */
    @Override
    public int producerMaxMessageBytes() {
        return rocketMQTemplate.getProducer().getMaxMessageSize();
    }

    /** 按原接口路由发送原生批次，由回调通知确认结果。 */
    @Override
    public CompletableFuture<Void> sendOrderedAsync(Long interfaceId, List<TelemetryMessage> messages) {
        CompletableFuture<Void> confirmation = new CompletableFuture<>();
        long started = System.nanoTime();
        try {
            // 第一步：只在发送线程将本批消息序列化，各条消息继续保留独立的消息体和标签。
            List<Message> parts = new ArrayList<>(messages.size());
            for (TelemetryMessage telemetry : messages) {
                Message part = new Message(resultTopic, resultTag, telemetry.toByteArray());
                MessageClientIDSetter.setUniqID(part);
                parts.add(part);
            }
            // 第二步：复用客户端批次编码，以及单条顺序发送使用的队列选择器。
            Message request = parts.get(0);
            if (parts.size() > 1) {
                MessageBatch batch = MessageBatch.generateFromList(parts);
                MessageClientIDSetter.setUniqID(batch);
                batch.setBody(batch.encode());
                request = batch;
            }
            int sentBytes = request.getBody().length;
            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            producer.send(request, rocketMQTemplate.getMessageQueueSelector(), interfaceId.toString(),
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult result) {
                            // 第三步：只有确认成功才允许接口队列推进，其他状态进入原批重试。
                            long finished = System.nanoTime();
                            if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                                confirmation.completeExceptionally(new IllegalStateException("遥测消息批次未获得成功确认"));
                            } else if (confirmation.complete(null)) {
                                logSuccess(interfaceId, started, finished, result, parts.size(), sentBytes);
                            }
                        }

                        @Override
                        public void onException(Throwable exception) {
                            confirmation.completeExceptionally(exception);
                        }
                    }, producer.getSendMsgTimeout());
        } catch (Exception exception) {
            // 提交阶段立即失败也走同一个回调结果，避免遗漏本批。
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            confirmation.completeExceptionally(exception);
        }
        return confirmation;
    }

    /** 每五秒最多采样一次确认耗时，批次额外带上本次确认消息数。 */
    private void logSuccess(Long interfaceId, long started, long finished, SendResult sendResult,
            int count, int sentBytes) {
        if (sendResult == null) {
            return;
        }
        long next = nextSuccessLogNanos.get();
        if (finished - next < 0L || !nextSuccessLogNanos.compareAndSet(
                next, finished + SUCCESS_LOG_INTERVAL_NANOS)) {
            return;
        }
        // 发送耗时从序列化或发送调用开始计算，排序缓冲等待不计入。
        MessageQueue queue = sendResult.getMessageQueue();
        LOGGER.info(
                "遥测消息发送成功，接口编号：{}，发送耗时：{}毫秒，名称服务：{}，目标主题：{}，目标标签：{}，"
                        + "消息标识：{}，偏移消息标识：{}，代理：{}，队列编号：{}，"
                        + "队列位点：{}，发送长度：{}字节，消息条数：{}",
                interfaceId,
                TimeUnit.NANOSECONDS.toMillis(finished - started),
                rocketMQTemplate.getProducer().getNamesrvAddr(),
                resultTopic,
                resultTag,
                sendResult.getMsgId(),
                sendResult.getOffsetMsgId(),
                queue == null ? null : queue.getBrokerName(),
                queue == null ? null : queue.getQueueId(),
                sendResult.getQueueOffset(),
                sentBytes,
                count);
    }

}
