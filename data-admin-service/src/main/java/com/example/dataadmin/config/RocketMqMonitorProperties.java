package com.example.dataadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** RocketMQ数据处理结果积压监控配置。 */
@Component
@ConfigurationProperties(prefix = "rocketmq-monitor")
public class RocketMqMonitorProperties {

    /** RocketMQ名称服务地址。 */
    private String nameServer = "127.0.0.1:9876";
    /** 当前数据处理结果主题。 */
    private String topic = "telemetry-framed-send";
    /** 消费数据处理结果的下游消费者组。 */
    private String consumerGroup;
    /** RocketMQ访问密钥。 */
    private String accessKey;
    /** RocketMQ访问密钥对应的私钥。 */
    private String secretKey;

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
