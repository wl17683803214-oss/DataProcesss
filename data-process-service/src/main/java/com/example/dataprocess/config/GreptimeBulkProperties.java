package com.example.dataprocess.config;

/** 官方批量写入协议的独立配置，所有内存额度均以字节为单位。 */
public class GreptimeBulkProperties {
    /** 留空时从查询连接推导同主机的默认写入端口。 */
    private String[] endpoints = new String[0];
    /** 跨帧合并后的单批行数上限，同时受字节和时间条件控制。 */
    private int maxBatchRows = 100000;
    /** 单批字段内容的保守估算上限，超限时在行边界拆分。 */
    private long maxBatchBytes = 8 * 1024 * 1024L;
    /** 每个流的堆外分配上限，包含列缓冲及传输过程中的分配。 */
    private long allocatorMaxBytes = 32 * 1024 * 1024L;
    /** 按表复用的流数量上限，达到上限后淘汰空闲流。 */
    private int maxCachedStreams = 16;
    /** 同时执行组装和等待写入确认的任务上限。 */
    private int maxConcurrentWrites = 4;
    /** 单次服务端确认及流内部等待的超时毫秒数。 */
    private long timeoutMillis = 30000L;
    /** 从当前对象最早待写帧开始累计，持续收到新帧不会延后刷新。 */
    private long flushIntervalMillis = 1000L;
    /** 已接收记录的估算总额度，包含待合并、写入中及重试中的记录。 */
    private long maxPendingBytes = 256 * 1024 * 1024L;
    /** 正常关闭时立即刷新剩余记录，等待超过此上限后明确报告未完成量。 */
    private long shutdownWaitMillis = 60000L;

    /** 获取显式写入地址。 */
    public String[] getEndpoints() { return endpoints.clone(); }
    /** 复制配置地址，避免调用方在启动后修改。 */
    public void setEndpoints(String[] value) { endpoints = value == null ? new String[0] : value.clone(); }
    /** 获取单批行数。 */
    public int getMaxBatchRows() { return maxBatchRows; }
    /** 设置单批行数。 */
    public void setMaxBatchRows(int value) { maxBatchRows = value; }
    /** 获取单批字节额度。 */
    public long getMaxBatchBytes() { return maxBatchBytes; }
    /** 设置单批字节额度。 */
    public void setMaxBatchBytes(long value) { maxBatchBytes = value; }
    /** 获取单流堆外额度。 */
    public long getAllocatorMaxBytes() { return allocatorMaxBytes; }
    /** 设置单流堆外额度。 */
    public void setAllocatorMaxBytes(long value) { allocatorMaxBytes = value; }
    /** 获取缓存流数。 */
    public int getMaxCachedStreams() { return maxCachedStreams; }
    /** 设置缓存流数。 */
    public void setMaxCachedStreams(int value) { maxCachedStreams = value; }
    /** 获取并发写入上限。 */
    public int getMaxConcurrentWrites() { return maxConcurrentWrites; }
    /** 设置并发写入上限。 */
    public void setMaxConcurrentWrites(int value) { maxConcurrentWrites = value; }
    /** 获取写入确认超时。 */
    public long getTimeoutMillis() { return timeoutMillis; }
    /** 设置写入确认超时。 */
    public void setTimeoutMillis(long value) { timeoutMillis = value; }
    /** 获取合并等待间隔。 */
    public long getFlushIntervalMillis() { return flushIntervalMillis; }
    /** 设置合并等待间隔。 */
    public void setFlushIntervalMillis(long value) { flushIntervalMillis = value; }
    /** 获取待写记录估算额度。 */
    public long getMaxPendingBytes() { return maxPendingBytes; }
    /** 设置待写记录估算额度。 */
    public void setMaxPendingBytes(long value) { maxPendingBytes = value; }
    /** 获取关闭时的刷新等待上限。 */
    public long getShutdownWaitMillis() { return shutdownWaitMillis; }
    /** 设置关闭时的刷新等待上限。 */
    public void setShutdownWaitMillis(long value) { shutdownWaitMillis = value; }

    /** 先拒绝无效额度，避免运行到写入线程后才发现配置错误。 */
    public void validate() {
        if (maxBatchRows < 1 || maxBatchRows > 100000 || maxBatchBytes < 1024
                || maxBatchBytes > 128 * 1024 * 1024L || allocatorMaxBytes > 1024 * 1024 * 1024L
                || allocatorMaxBytes < maxBatchBytes * 2 || maxCachedStreams < 1
                || maxCachedStreams > 256 || maxConcurrentWrites < 1
                || maxConcurrentWrites > maxCachedStreams || timeoutMillis < 1000 || timeoutMillis > 300000) {
            throw new IllegalArgumentException("批量写入配置无效：行数为1至100000，单流内存至少为批次额度两倍，并发数不能超过缓存流数，超时为1至300秒");
        }
        for (String endpoint : endpoints) {
            if (endpoint == null || endpoint.trim().isEmpty()) {
                throw new IllegalArgumentException("批量写入地址不能为空");
            }
        }
        // 合并缓冲必须能容纳一个配置批次，关闭等待也必须有明确边界。
        if (flushIntervalMillis < 1 || flushIntervalMillis > 60000 || maxPendingBytes < maxBatchBytes
                || maxPendingBytes > 16L * 1024 * 1024 * 1024
                || shutdownWaitMillis < 1000 || shutdownWaitMillis > 300000) {
            throw new IllegalArgumentException("遥测合并配置无效：刷新间隔为1至60000毫秒，待写额度至少为单批字节额度且不超过16吉字节，关闭等待为1至300秒");
        }
    }
}
