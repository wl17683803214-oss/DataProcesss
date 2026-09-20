package com.example.dataadmin.enums;

/** 试验任务生命周期事件类型。 */
public enum TaskLifecycleEventType {

    /** 任务开始事件。 */
    STARTED,
    /** 任务结束事件。 */
    ENDED;

    /** 校验并返回生命周期事件类型。 */
    public static TaskLifecycleEventType require(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("任务生命周期事件类型不能为空");
        }
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的任务生命周期事件类型：" + value);
        }
    }
}
