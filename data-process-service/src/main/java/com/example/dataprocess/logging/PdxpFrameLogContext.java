package com.example.dataprocess.logging;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;

/** 仅在当前消息发送任务内传递接口首帧日志状态，不改变消息服务入参。 */
public final class PdxpFrameLogContext {

    /** 结果线程可能复用，任务结束必须恢复原来的线程上下文。 */
    private static final ThreadLocal<CollectInterfaceRuntimeConfig> CURRENT_INTERFACE =
            new ThreadLocal<CollectInterfaceRuntimeConfig>();

    private PdxpFrameLogContext() {
    }

    /** 在接口自己的日志范围内执行发送，发送异常原样交给调度组件处理。 */
    public static void runWithInterface(CollectInterfaceRuntimeConfig config, Runnable action) {
        CollectInterfaceRuntimeConfig previous = CURRENT_INTERFACE.get();
        CURRENT_INTERFACE.set(config);
        try {
            action.run();
        } finally {
            // 防止线程池下一条消息误用其他接口的首帧标记。
            if (previous == null) {
                CURRENT_INTERFACE.remove();
            } else {
                CURRENT_INTERFACE.set(previous);
            }
        }
    }

    /** PDXP消息只记录本接口首次成功，其他直接发送入口沿用原来的日志行为。 */
    public static boolean shouldLogMessageSuccess() {
        CollectInterfaceRuntimeConfig config = CURRENT_INTERFACE.get();
        return config == null || config.markFirstPdxpMessageLog();
    }
}
