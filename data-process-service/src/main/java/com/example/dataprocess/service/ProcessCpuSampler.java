package com.example.dataprocess.service;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

/** 定时采集当前处理服务的进程 CPU 使用率。 */
@Component
public class ProcessCpuSampler {

    private static final long MAX_SAMPLE_AGE_NANOS = TimeUnit.SECONDS.toNanos(5);

    /** 保存上一次采样的进程累计 CPU 时间和单调时钟。 */
    private long previousCpuNanos = -1L;
    private long previousSampleNanos;
    /** 采样线程发布完整结果，请求线程只读取最新快照。 */
    private volatile Sample latestSample;

    @Scheduled(fixedDelay = 1000)
    public void sampleCpuUsage() {
        // 读取当前 JVM 累计使用的 CPU 时间。首次采样只用于建立基线。
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        long cpuNanos = bean instanceof OperatingSystemMXBean
                ? ((OperatingSystemMXBean) bean).getProcessCpuTime() : -1L;
        long sampleNanos = System.nanoTime();
        long elapsedNanos = sampleNanos - previousSampleNanos;
        Sample sample = null;
        if (previousCpuNanos >= 0 && cpuNanos >= previousCpuNanos && elapsedNanos > 0) {
            // 按实际采样间隔和 JVM 识别的处理器数计算百分比，保留超过 100% 的读数。
            int processors = Runtime.getRuntime().availableProcessors();
            double percent = (cpuNanos - previousCpuNanos) * 100.0 / elapsedNanos / processors;
            sample = new Sample(sampleNanos, Math.round(percent * 100.0) / 100.0);
        }

        // 更新下轮基线，并一次性发布本轮结果。
        previousCpuNanos = cpuNanos;
        previousSampleNanos = sampleNanos;
        latestSample = sample;
    }

    /** 返回最近一次有效采样，初始化期间或采样过期时返回空值。 */
    public Double getCpuUsagePercent() {
        Sample sample = latestSample;
        return sample == null || System.nanoTime() - sample.sampleNanos > MAX_SAMPLE_AGE_NANOS
                ? null : sample.percent;
    }

    /** CPU 读数和单调时钟时间戳一同保存。 */
    private static final class Sample {
        private final long sampleNanos;
        private final double percent;

        private Sample(long sampleNanos, double percent) {
            this.sampleNanos = sampleNanos;
            this.percent = percent;
        }
    }
}
