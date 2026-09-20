package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 文件交换接收配置。 */
@Component
@ConfigurationProperties(prefix = "data-processing.fep")
public class FepProperties {

    /** 接收中文件使用的本地临时目录。 */
    private String tempDirectory;
    /** 单个文件数据单元的最大字节数。 */
    private int dataUnitLength;
    /** 文件结果线程池固定线程数。 */
    private int resultThreadCount;
    /** 文件结果线程池等待队列容量。 */
    private int resultQueueCapacity;
    /** 待处理文件扫描间隔，单位为毫秒。 */
    private long retryIntervalMillis;
    /** 每次扫描提交的最大文件数量。 */
    private int retryBatchSize;

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public int getDataUnitLength() {
        return dataUnitLength;
    }

    public void setDataUnitLength(int dataUnitLength) {
        this.dataUnitLength = dataUnitLength;
    }

    public int getResultThreadCount() {
        return resultThreadCount;
    }

    public void setResultThreadCount(int resultThreadCount) {
        this.resultThreadCount = resultThreadCount;
    }

    public int getResultQueueCapacity() {
        return resultQueueCapacity;
    }

    public void setResultQueueCapacity(int resultQueueCapacity) {
        this.resultQueueCapacity = resultQueueCapacity;
    }

    public long getRetryIntervalMillis() {
        return retryIntervalMillis;
    }

    public void setRetryIntervalMillis(long retryIntervalMillis) {
        this.retryIntervalMillis = retryIntervalMillis;
    }

    public int getRetryBatchSize() {
        return retryBatchSize;
    }

    public void setRetryBatchSize(int retryBatchSize) {
        this.retryBatchSize = retryBatchSize;
    }
}
