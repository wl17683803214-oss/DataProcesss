package com.example.dataprocess.entity;

import java.time.LocalDateTime;

/** 数据处理服务批量写入的校准野值详情。 */
public class CalibrationRecordDetail {
    /** 试验任务ID。 */ private String taskId;
    /** 校准通道ID。 */ private Long channelId;
    /** 采集接口ID。 */ private Long interfaceId;
    /** 遥测帧通道名称。 */ private String channelName;
    /** 遥测参数名称。 */ private String parameterName;
    /** 遥测代号。 */ private String tmSymbol;
    /** 触发野值时的遥测值。 */ private Double telemetryValue;
    /** 判定为野值的详细原因。 */ private String detail;
    /** 野值发生时间。 */ private LocalDateTime createTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public String getTmSymbol() { return tmSymbol; }
    public void setTmSymbol(String tmSymbol) { this.tmSymbol = tmSymbol; }
    public Double getTelemetryValue() { return telemetryValue; }
    public void setTelemetryValue(Double telemetryValue) {
        this.telemetryValue = telemetryValue;
    }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
