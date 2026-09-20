package com.example.dataadmin.vo.processing;

import java.time.LocalDateTime;

/** IoTDB 中的处理后遥测参数。 */
public class ProcessedTelemetryVO {
    /** 采集接口主键。 */
    private Long interfaceId;
    private String satelliteId;
    private String parameter;
    private Object parameterValue;
    private String status;
    /** 状态范围中从零开始的状态索引。 */
    private Integer stateIndex;
    private String deduplication;
    private LocalDateTime processTime;

    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }
    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }
    public String getParameter() { return parameter; }
    public void setParameter(String parameter) { this.parameter = parameter; }
    public Object getParameterValue() { return parameterValue; }
    public void setParameterValue(Object parameterValue) { this.parameterValue = parameterValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getStateIndex() { return stateIndex; }
    public void setStateIndex(Integer stateIndex) { this.stateIndex = stateIndex; }
    public String getDeduplication() { return deduplication; }
    public void setDeduplication(String deduplication) { this.deduplication = deduplication; }
    public LocalDateTime getProcessTime() { return processTime; }
    public void setProcessTime(LocalDateTime processTime) { this.processTime = processTime; }
}
