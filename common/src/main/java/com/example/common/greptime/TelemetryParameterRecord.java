package com.example.common.greptime;

import java.sql.Timestamp;

/** 遥测参数，仅用于存储映射，不作为接口返回对象。 */
public class TelemetryParameterRecord {
    /** 源时间。 */
    private Timestamp sourceTime;
    /** 任务编号。 */
    private String taskId;
    /** 来源类型。 */
    private String sourceType;
    /** 设备或远程组合身份。 */
    private String sourceIdentity;
    /** 遥测类型。 */
    private String telemetryType;
    /** 遥测代号。 */
    private String telemetryCode;
    /** 参数名称。 */
    private String telemetryName;
    /** 遥测值类型编码。 */
    private String valueType;
    /** 遥测数值。 */
    private Double value;
    /** 遥测值文本原文。 */
    private String valueText;
    /** 参数原码。 */
    private byte[] raw;
    /** 参数状态名称原文。 */
    private String stateName;
    /** 参数状态类型编码。 */
    private String stateType;
    /** 参数状态索引。 */
    private Integer stateIndex;
    /** 处理时间。 */
    private Timestamp processedAt;

    /** 获取源时间。 */
    public Timestamp getSourceTime() {
        return sourceTime;
    }

    /** 设置源时间。 */
    public void setSourceTime(Timestamp sourceTime) {
        this.sourceTime = sourceTime;
    }

    /** 获取任务编号。 */
    public String getTaskId() {
        return taskId;
    }

    /** 设置任务编号。 */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /** 获取来源类型。 */
    public String getSourceType() {
        return sourceType;
    }

    /** 设置来源类型。 */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /** 获取设备或远程组合身份。 */
    public String getSourceIdentity() {
        return sourceIdentity;
    }

    /** 设置设备或远程组合身份。 */
    public void setSourceIdentity(String sourceIdentity) {
        this.sourceIdentity = sourceIdentity;
    }

    /** 获取遥测类型。 */
    public String getTelemetryType() {
        return telemetryType;
    }

    /** 设置遥测类型。 */
    public void setTelemetryType(String telemetryType) {
        this.telemetryType = telemetryType;
    }

    /** 获取遥测代号。 */
    public String getTelemetryCode() {
        return telemetryCode;
    }

    /** 设置遥测代号。 */
    public void setTelemetryCode(String telemetryCode) {
        this.telemetryCode = telemetryCode;
    }

    /** 获取参数名称。 */
    public String getTelemetryName() {
        return telemetryName;
    }

    /** 设置参数名称。 */
    public void setTelemetryName(String telemetryName) {
        this.telemetryName = telemetryName;
    }

    /** 获取遥测值类型编码。 */
    public String getValueType() {
        return valueType;
    }

    /** 设置遥测值类型编码。 */
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    /** 获取遥测数值。 */
    public Double getValue() {
        return value;
    }

    /** 设置遥测数值。 */
    public void setValue(Double value) {
        this.value = value;
    }

    /** 获取遥测值文本原文。 */
    public String getValueText() {
        return valueText;
    }

    /** 设置遥测值文本原文。 */
    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    /** 获取参数原码。 */
    public byte[] getRaw() {
        return raw;
    }

    /** 设置参数原码。 */
    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    /** 获取参数状态名称原文。 */
    public String getStateName() {
        return stateName;
    }

    /** 设置参数状态名称原文。 */
    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    /** 获取参数状态类型编码。 */
    public String getStateType() {
        return stateType;
    }

    /** 设置参数状态类型编码。 */
    public void setStateType(String stateType) {
        this.stateType = stateType;
    }

    /** 获取参数状态索引。 */
    public Integer getStateIndex() {
        return stateIndex;
    }

    /** 设置参数状态索引。 */
    public void setStateIndex(Integer stateIndex) {
        this.stateIndex = stateIndex;
    }

    /** 获取处理时间。 */
    public Timestamp getProcessedAt() {
        return processedAt;
    }

    /** 设置处理时间。 */
    public void setProcessedAt(Timestamp processedAt) {
        this.processedAt = processedAt;
    }
}
