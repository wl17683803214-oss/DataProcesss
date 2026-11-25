package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 处理服务共用的耗时日志开关与包序号采样间隔。 */
@Component
@ConfigurationProperties(prefix = "data-processing.timing-log")
public class TimingLogProperties {
    /** 默认保留处理计时、入库采样和遥测消息发送成功日志。 */
    private volatile boolean enabled = true;
    /** 包序号为该间隔的整数倍时记录阶段耗时。 */
    private volatile int sampleEveryPackets = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSampleEveryPackets() {
        return sampleEveryPackets;
    }

    public void setSampleEveryPackets(int sampleEveryPackets) {
        // 先校验再写入，确保处理线程不会读到无效间隔。
        if (sampleEveryPackets < 1) {
            throw new IllegalArgumentException("日志采样包间隔必须大于零");
        }
        this.sampleEveryPackets = sampleEveryPackets;
    }

    /** 关闭时直接跳过，开启时各阶段按同一个包序号采样。 */
    public boolean shouldSample(long sequence) {
        return enabled && sequence % sampleEveryPackets == 0;
    }
}
