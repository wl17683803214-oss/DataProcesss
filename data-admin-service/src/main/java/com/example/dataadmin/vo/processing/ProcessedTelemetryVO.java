package com.example.dataadmin.vo.processing;

import java.time.LocalDateTime;

/** GreptimeDB处理后参数的页面返回结构。 */
public class ProcessedTelemetryVO {
    /** 已保存勾选记录所属的设备卫星主键。 */
    private Long deviceSatelliteId;
    /** 参数表中的遥测代号。 */
    private String telemetryCode;
    /** Proto参数名称。 */
    private String parameter;
    /** 文本类型返回字符串，其他类型返回数值；未采集时为空。 */
    private Object parameterValue;
    /** Proto状态中文名称。 */
    private String status;
    /** 实际处理时间。 */
    private LocalDateTime processTime;

    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long value) { this.deviceSatelliteId = value; }
    public String getTelemetryCode() { return telemetryCode; }
    public void setTelemetryCode(String value) { this.telemetryCode = value; }
    public String getParameter() { return parameter; }
    public void setParameter(String value) { this.parameter = value; }
    /** 返回按遥测值类型选择的文本或数值。 */
    public Object getParameterValue() { return parameterValue; }
    /** 保存按遥测值类型选择的文本或数值。 */
    public void setParameterValue(Object value) { this.parameterValue = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public LocalDateTime getProcessTime() { return processTime; }
    public void setProcessTime(LocalDateTime value) { this.processTime = value; }
}
