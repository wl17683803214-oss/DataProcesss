package com.example.dataadmin.entity;

/** 遥测代号搜索使用的参数、所属设备及系统信息。 */
public class TelemetryFilterSearchRecord {
    /** 参数解析配置主键。 */
    private Long id;
    /** 设备卫星主键。 */
    private Long deviceSatelliteId;
    /** 设备卫星名称。 */
    private String deviceSatelliteName;
    /** 导入文件中的所属系统完整路径。 */
    private String systemName;
    /** 遥测参数名称。 */
    private String telemetryName;
    /** 遥测代号。 */
    private String telemetryCode;
    /** 当前是否已勾选。 */
    private Boolean checked;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long value) { this.deviceSatelliteId = value; }
    public String getDeviceSatelliteName() { return deviceSatelliteName; }
    public void setDeviceSatelliteName(String value) { this.deviceSatelliteName = value; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String value) { this.systemName = value; }
    public String getTelemetryName() { return telemetryName; }
    public void setTelemetryName(String value) { this.telemetryName = value; }
    public String getTelemetryCode() { return telemetryCode; }
    public void setTelemetryCode(String value) { this.telemetryCode = value; }
    public Boolean getChecked() { return checked; }
    public void setChecked(Boolean value) { this.checked = value; }
}
