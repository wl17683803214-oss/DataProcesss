package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/** 统一绑定结果线程池和按采集接口重排消息的配置。 */
@Component
@Validated
@ConfigurationProperties(prefix = "data-processing.result")
public class ResultDispatchProperties {

    /** 入库线程数沿用已有配置。 */
    @Min(value = 1, message = "结果入库线程数必须大于零")
    private int storageThreadCount = 4;
    /** 入库队列容量沿用已有配置。 */
    @Min(value = 1, message = "结果入库队列容量必须大于零")
    private int storageQueueCapacity = 2048;
    /** 全部接口共用固定数量的消息调度线程。 */
    @Min(value = 1, message = "结果消息线程数必须大于零")
    private int messageThreadCount = 4;
    /** 投递中、重排中及失败重试中的消息共用此总条数额度。 */
    @Min(value = 1, message = "结果消息队列容量必须大于零")
    private int messageQueueCapacity = 2048;
    /** 入库和消息发送在关闭时共用的等待秒数。 */
    @Min(value = 1, message = "结果关闭等待时间必须大于零")
    private int shutdownWaitSeconds = 30;
    /** 连续重排的时间、接口额度及发送参数。 */
    @Valid
    private final MessageOrder messageOrder = new MessageOrder();

    /** 读取入库线程数。 */
    public int getStorageThreadCount() { return storageThreadCount; }

    /** 绑定入库线程数。 */
    public void setStorageThreadCount(int value) { storageThreadCount = value; }

    /** 读取入库队列容量。 */
    public int getStorageQueueCapacity() { return storageQueueCapacity; }

    /** 绑定入库队列容量。 */
    public void setStorageQueueCapacity(int value) { storageQueueCapacity = value; }

    /** 读取共享消息线程数。 */
    public int getMessageThreadCount() { return messageThreadCount; }

    /** 绑定共享消息线程数。 */
    public void setMessageThreadCount(int value) { messageThreadCount = value; }

    /** 读取全局消息条数上限。 */
    public int getMessageQueueCapacity() { return messageQueueCapacity; }

    /** 绑定全局消息条数上限。 */
    public void setMessageQueueCapacity(int value) { messageQueueCapacity = value; }

    /** 读取关闭等待秒数。 */
    public int getShutdownWaitSeconds() { return shutdownWaitSeconds; }

    /** 绑定关闭等待秒数。 */
    public void setShutdownWaitSeconds(int value) { shutdownWaitSeconds = value; }

    /** 读取连续重排配置。 */
    public MessageOrder getMessageOrder() { return messageOrder; }

    /** 消息到期只推进到自身序号，较大序号仍保留在接口队列。 */
    public static class MessageOrder {

        /** 关闭时保留消息重排，到期后跳过实际发送。 */
        private boolean sendEnabled = true;
        /** 从消息进入缓冲区开始计算的到期时间，零表示不额外等待。 */
        @Min(value = 0, message = "消息重排等待时间不能小于零")
        @Max(value = 600000, message = "消息重排等待时间不能超过十分钟")
        private long waitMillis = 1000;
        /** 周期检查间隔，不重置任意消息的入队时间。 */
        @Min(value = 1, message = "消息重排检查间隔必须大于零")
        private long scanIntervalMillis = 10;
        /** 单接口消息额度，按业务限制单个接口的积压量。 */
        @Min(value = 1, message = "单接口待发送消息上限必须大于零")
        private int maxPendingPerInterface = 2048;
        /** 按协议缓冲区长度加记录开销估算的全局缓冲字节上限。 */
        @Min(value = 1, message = "消息缓冲字节上限必须大于零")
        private long maxPendingBytes = 268435456L;
        /** 失败后保留消息和发送边界，等待此间隔再尝试。 */
        @Min(value = 1, message = "消息发送重试间隔必须大于零")
        private long retryIntervalMillis = 1000;
        /** 每次调度最多发送的条数，让其他接口也有机会取得线程。 */
        @Min(value = 1, message = "单次消息发送条数必须大于零")
        private int maxSendsPerRun = 256;

        /** 读取到期消息的发送开关。 */
        public boolean isSendEnabled() { return sendEnabled; }

        /** 绑定到期消息的发送开关。 */
        public void setSendEnabled(boolean value) { sendEnabled = value; }

        /** 读取重排等待毫秒数。 */
        public long getWaitMillis() { return waitMillis; }

        /** 绑定重排等待毫秒数。 */
        public void setWaitMillis(long value) { waitMillis = value; }

        /** 读取检查间隔。 */
        public long getScanIntervalMillis() { return scanIntervalMillis; }

        /** 绑定检查间隔。 */
        public void setScanIntervalMillis(long value) { scanIntervalMillis = value; }

        /** 读取单接口消息额度。 */
        public int getMaxPendingPerInterface() { return maxPendingPerInterface; }

        /** 绑定单接口消息额度。 */
        public void setMaxPendingPerInterface(int value) { maxPendingPerInterface = value; }

        /** 读取全局缓冲字节额度。 */
        public long getMaxPendingBytes() { return maxPendingBytes; }

        /** 绑定全局缓冲字节额度。 */
        public void setMaxPendingBytes(long value) { maxPendingBytes = value; }

        /** 读取失败重试间隔。 */
        public long getRetryIntervalMillis() { return retryIntervalMillis; }

        /** 绑定失败重试间隔。 */
        public void setRetryIntervalMillis(long value) { retryIntervalMillis = value; }

        /** 读取单次调度发送上限。 */
        public int getMaxSendsPerRun() { return maxSendsPerRun; }

        /** 绑定单次调度发送上限。 */
        public void setMaxSendsPerRun(int value) { maxSendsPerRun = value; }
    }
}
