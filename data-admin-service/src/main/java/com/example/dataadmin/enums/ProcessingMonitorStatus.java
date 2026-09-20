package com.example.dataadmin.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/** 数据处理实时监控状态。 */
public enum ProcessingMonitorStatus {

    NORMAL("正常"),
    WARNING("关注"),
    ERROR("异常");

    private final String label;

    ProcessingMonitorStatus(String label) {
        this.label = label;
    }

    /** 接口序列化时直接返回中文状态。 */
    @JsonValue
    public String getLabel() {
        return label;
    }
}
