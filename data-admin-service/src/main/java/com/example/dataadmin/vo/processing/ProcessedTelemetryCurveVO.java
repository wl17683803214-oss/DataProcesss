package com.example.dataadmin.vo.processing;

import java.util.ArrayList;
import java.util.List;

/** 单个已勾选遥测参数的最近曲线点。 */
public class ProcessedTelemetryCurveVO {
    /** 设备卫星主键。 */
    private Long deviceSatelliteId;
    /** 遥测代号。 */
    private String telemetryCode;
    /** 遥测参数名称。 */
    private String parameter;
    /** 横轴时间，使用Unix毫秒时间戳。 */
    private List<Long> times = new ArrayList<>();
    /** 与横轴时间逐项对应的Proto数值。 */
    private List<Double> values = new ArrayList<>();

    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long value) { this.deviceSatelliteId = value; }
    public String getTelemetryCode() { return telemetryCode; }
    public void setTelemetryCode(String value) { this.telemetryCode = value; }
    public String getParameter() { return parameter; }
    public void setParameter(String value) { this.parameter = value; }
    public List<Long> getTimes() { return times; }
    public void setTimes(List<Long> value) { this.times = value; }
    public List<Double> getValues() { return values; }
    public void setValues(List<Double> value) { this.values = value; }
}
