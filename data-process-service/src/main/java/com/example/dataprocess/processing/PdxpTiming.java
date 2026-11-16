package com.example.dataprocess.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/** 按包序号采样各阶段，使用同一标识关联处理和入库日志。 */
public final class PdxpTiming {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdxpTiming.class);
    private static final int SAMPLE_INTERVAL = 20;
    /** 仅采样任务传递本帧估算值，线程池任务结束后必须清理，避免串帧。 */
    private static final ThreadLocal<Long> STORAGE_ESTIMATED_BYTES = new ThreadLocal<>();

    private PdxpTiming() {
    }

    /** 临时压测期间每二十个包序号采样一次。 */
    public static boolean sample(long sequence) {
        return sequence % SAMPLE_INTERVAL == 0;
    }

    /** 开始当前采样帧的入库记录估算，负值表示记录尚未组装完成。 */
    public static void beginStorageSample() {
        STORAGE_ESTIMATED_BYTES.set(-1L);
    }

    /** 复用缓冲已经计算的估算值，未采样帧不创建额外上下文。 */
    public static void recordStorageEstimate(long bytes) {
        if (STORAGE_ESTIMATED_BYTES.get() != null) {
            STORAGE_ESTIMATED_BYTES.set(bytes);
        }
    }

    /** 返回本帧估算并立即清理线程上下文，失败或停用存储时返回空值。 */
    public static Long endStorageSample() {
        Long bytes = STORAGE_ESTIMATED_BYTES.get();
        STORAGE_ESTIMATED_BYTES.remove();
        return bytes == null || bytes < 0 ? null : bytes;
    }

    /** 记录原排队耗时及完整入库记录的估算大小，不将未获取大小写成零。 */
    public static void logStorageQueue(Long interfaceId, long sequence, long nanos, Long bytes) {
        // 第一步：记录未组装成功或存储停用时，保留原有排队计时格式。
        if (bytes == null) {
            log(interfaceId, sequence, "入库组装排队", nanos);
            return;
        }
        // 第二步：同时展示字节数和二进制兆字节，数值与合并缓冲额度口径一致。
        LOGGER.info("PDXP压测计时，接口编号：{}，包序号：{}，阶段：入库组装排队，耗时：{}毫秒，"
                        + "本帧入库估算大小：{}字节（{}MiB）",
                interfaceId, sequence, String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0),
                bytes, String.format(Locale.ROOT, "%.2f", bytes / (1024.0 * 1024.0)));
    }

    /** 使用单调时钟记录耗时，统一输出毫秒。 */
    public static void log(Long interfaceId, long sequence, String stage, long nanos) {
        LOGGER.info("PDXP压测计时，接口编号：{}，包序号：{}，阶段：{}，耗时：{}毫秒",
                interfaceId, sequence, stage, String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0));
    }
}
