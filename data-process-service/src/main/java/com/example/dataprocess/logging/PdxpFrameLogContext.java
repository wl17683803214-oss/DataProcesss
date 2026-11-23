package com.example.dataprocess.logging;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;

/** 仅在当前消息发送任务内传递接口上下文，不改变消息服务入参。 */
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
            // 防止线程池下一条消息误用其他采集接口的上下文。
            if (previous == null) {
                CURRENT_INTERFACE.remove();
            } else {
                CURRENT_INTERFACE.set(previous);
            }
        }
    }

    /** 复用已有线程上下文取得顺序发送分组，不向下游消息增加接口字段。 */
    public static Long currentInterfaceId() {
        CollectInterfaceRuntimeConfig config = CURRENT_INTERFACE.get();
        return config == null ? null : config.getInterfaceId();
    }
}
