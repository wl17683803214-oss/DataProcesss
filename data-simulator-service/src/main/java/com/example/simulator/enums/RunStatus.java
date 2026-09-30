package com.example.simulator.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/** 模拟源专用执行状态，现有采集在线状态不能表达完成和停止过程。 */
public enum RunStatus {
    STARTING(1, "启动中"), RUNNING(2, "运行中"), STOPPING(3, "停止中"),
    STOPPED(4, "已停止"), COMPLETED(5, "已完成"), FAILED(6, "异常");

    public final int code;
    public final String label;

    RunStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 对外统一返回中文。 */
    @JsonValue
    public String label() {
        return label;
    }

    /** 将持久化编码转换为执行状态。 */
    public static RunStatus of(int code) {
        for (RunStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知模拟源运行状态");
    }
}

