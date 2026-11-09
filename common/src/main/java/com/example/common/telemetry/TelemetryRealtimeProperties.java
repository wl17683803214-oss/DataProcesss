package com.example.common.telemetry;

/** 遥测实时快照配置，两个服务必须使用相同的缓存前缀和保留时间。 */
public class TelemetryRealtimeProperties {
    /** 是否启用实时缓存，关闭后实时接口返回空快照。 */
    private boolean enabled = true;
    /** 与统计、认证及校准缓存隔离的键前缀。 */
    private String keyPrefix = "telemetry:live:v2:";
    /** 帧与单个最新参数共用的保留秒数，默认三天。 */
    private long retentionSeconds = 259200;
    /** 每任务、每接口、每种帧状态的序列化记录字节上限。 */
    private int frameCapacityBytes = 4194304;
    /** 异步队列最多保留的消息数量。 */
    private int queueCapacity = 256;
    /** 排队与正在合并的参数点总量上限，避免大消息撑满内存。 */
    private int maxPendingParameters = 3000000;
    /** 异步队列允许保留的完整帧字节总数。 */
    private int maxPendingFrameBytes = 16777216;
    /** 工作线程合并快照的周期，单位毫秒。 */
    private long flushIntervalMillis = 50;
    /** 实时转发队列的消息上限。 */
    private int pushQueueCapacity = 64;

    /** 启动时检查配置，避免错误配置形成无界缓存。 */
    public void validate() {
        // 缓存窗口和队列都必须有明确的正数上限。
        if (keyPrefix == null || keyPrefix.trim().isEmpty() || keyPrefix.contains("{")
                || keyPrefix.contains("}") || retentionSeconds <= 0 || retentionSeconds > Integer.MAX_VALUE
                || frameCapacityBytes <= 0 || pushQueueCapacity <= 0
                || queueCapacity <= 0 || maxPendingParameters <= 0 || maxPendingFrameBytes <= 0
                || flushIntervalMillis <= 0) {
            throw new IllegalArgumentException("遥测实时缓存配置必须使用有效前缀和正数容量");
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String value) { keyPrefix = value; }
    public long getRetentionSeconds() { return retentionSeconds; }
    public void setRetentionSeconds(long value) { retentionSeconds = value; }
    public int getFrameCapacityBytes() { return frameCapacityBytes; }
    public void setFrameCapacityBytes(int value) { frameCapacityBytes = value; }
    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int value) { queueCapacity = value; }
    public int getMaxPendingParameters() { return maxPendingParameters; }
    public void setMaxPendingParameters(int value) { maxPendingParameters = value; }
    public int getMaxPendingFrameBytes() { return maxPendingFrameBytes; }
    public void setMaxPendingFrameBytes(int value) { maxPendingFrameBytes = value; }
    public long getFlushIntervalMillis() { return flushIntervalMillis; }
    public void setFlushIntervalMillis(long value) { flushIntervalMillis = value; }
    public int getPushQueueCapacity() { return pushQueueCapacity; }
    public void setPushQueueCapacity(int value) { pushQueueCapacity = value; }
}
