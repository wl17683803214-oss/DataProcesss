package com.example.dataadmin.entity;

/** 当前任务共享的已勾选遥测参数。 */
public class ProcessedTelemetryFilterSelection {
    /** 当前试验任务编号。 */
    private String taskId;
    /** 设备卫星主键。 */
    private Long deviceSatelliteId;
    /** 遥测代号。 */
    private String telemetryCode;
    /** 当前有效解析配置中的遥测名称。 */
    private String telemetryName;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { this.taskId = value; }
    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long value) { this.deviceSatelliteId = value; }
    public String getTelemetryCode() { return telemetryCode; }
    public void setTelemetryCode(String value) { this.telemetryCode = value; }
    public String getTelemetryName() { return telemetryName; }
    public void setTelemetryName(String value) { this.telemetryName = value; }
}
