package com.example.dataprocess.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/** 按包序号采样各阶段，使用同一标识关联处理和入库日志。 */
public final class PdxpTiming {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdxpTiming.class);
    private static final int SAMPLE_INTERVAL = 20;

    private PdxpTiming() {
    }

    /** 临时压测期间每二十个包序号采样一次。 */
    public static boolean sample(long sequence) {
        return sequence % SAMPLE_INTERVAL == 0;
    }

    /** 使用单调时钟记录耗时，统一输出毫秒。 */
    public static void log(Long interfaceId, long sequence, String stage, long nanos) {
        LOGGER.info("PDXP压测计时，接口编号：{}，包序号：{}，阶段：{}，耗时：{}毫秒",
                interfaceId, sequence, stage, String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0));
    }
}
