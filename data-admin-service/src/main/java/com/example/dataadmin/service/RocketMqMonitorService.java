package com.example.dataadmin.service;

import com.example.dataadmin.config.RocketMqMonitorProperties;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Map;

/** 查询RocketMQ指定消费者组的数据处理结果积压量。 */
@Service
public class RocketMqMonitorService {

    /** RocketMQ监控日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RocketMqMonitorService.class);
    /** RocketMQ监控配置。 */
    private final RocketMqMonitorProperties properties;
    /** 延迟创建并复用的RocketMQ管理客户端。 */
    private volatile DefaultMQAdminExt adminClient;

    public RocketMqMonitorService(RocketMqMonitorProperties properties) {
        this.properties = properties;
    }

    /** 查询当前结果主题相对于下游消费者组的总积压量。 */
    public Long readBacklog() {
        // 未配置主题或消费者组时无法计算消费位点差值。
        if (isBlank(properties.getTopic())
                || isBlank(properties.getConsumerGroup())) {
            return null;
        }

        try {
            // 获取已启动的管理客户端并查询指定主题的消费统计。
            ConsumeStats consumeStats = getOrStartAdminClient()
                    .examineConsumeStats(
                            properties.getConsumerGroup(),
                            properties.getTopic());
            long backlog = 0L;

            // 汇总主题下每个消息队列的Broker位点与消费位点差值。
            for (Map.Entry<MessageQueue, OffsetWrapper> entry
                    : consumeStats.getOffsetTable().entrySet()) {
                OffsetWrapper offset = entry.getValue();
                backlog += Math.max(
                        0L,
                        offset.getBrokerOffset() - offset.getConsumerOffset());
            }
            return backlog;
        } catch (Exception exception) {
            // 远程RocketMQ不可用时监控值留空，不影响Admin其他业务。
            LOGGER.warn("RocketMQ消息积压量查询失败，主题：{}，消费者组：{}",
                    properties.getTopic(),
                    properties.getConsumerGroup(),
                    exception);
            resetAdminClient();
            return null;
        }
    }

    /** 获取已启动的管理客户端，连接失败时允许下次查询重新创建。 */
    private synchronized DefaultMQAdminExt getOrStartAdminClient()
            throws Exception {
        // 已成功启动的客户端直接复用，避免每五秒重复创建网络资源。
        if (adminClient != null) {
            return adminClient;
        }

        // 根据是否配置访问密钥选择普通连接或ACL连接。
        DefaultMQAdminExt newClient;
        if (isBlank(properties.getAccessKey())
                || isBlank(properties.getSecretKey())) {
            newClient = new DefaultMQAdminExt();
        } else {
            SessionCredentials credentials = new SessionCredentials(
                    properties.getAccessKey(), properties.getSecretKey());
            newClient = new DefaultMQAdminExt(
                    new AclClientRPCHook(credentials));
        }

        // 设置唯一管理组和远程名称服务地址后启动客户端。
        newClient.setAdminExtGroup("data-admin-rocketmq-monitor");
        newClient.setInstanceName("data-admin-rocketmq-monitor");
        newClient.setNamesrvAddr(properties.getNameServer());
        newClient.start();
        adminClient = newClient;
        return adminClient;
    }

    /** 关闭失效客户端，使下次定时采集能够重新建立连接。 */
    private synchronized void resetAdminClient() {
        // 已创建的客户端需要先释放网络线程和连接资源。
        if (adminClient != null) {
            adminClient.shutdown();
            adminClient = null;
        }
    }

    /** 应用停止时释放RocketMQ管理客户端。 */
    @PreDestroy
    public void shutdown() {
        // 复用统一重置逻辑完成资源关闭。
        resetAdminClient();
    }

    /** 判断配置文本是否没有有效内容。 */
    private boolean isBlank(String value) {
        // 空值和只包含空白字符的值都视为未配置。
        return value == null || value.trim().isEmpty();
    }
}
