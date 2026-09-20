package com.example.common.calibration;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 校准实时缓存和定时落库共用的样本结构。 */
public class CalibrationSample {

    /** 样本唯一标识，用于避免同一毫秒内的数据相互覆盖。 */
    private String sampleId;
    /** 试验任务主键。 */
    private String taskId;
    /** 校准通道主键。 */
    private Long channelId;
    /** 校准公式；兼容读取改名前缓存中的通道编号字段。 */
    @JsonAlias("channelCode")
    private String calibrationFormula;
    /** 遥测参数代号。 */
    private String telemetryCode;
    /** 采样时间毫秒值。 */
    private Long sampleTime;
    /** 原始参数值。 */
    private Double value;
    /** 是否为野值：0否，1是。 */
    private Integer outlier;
    /** 是否已自动清洗：0否，1是。 */
    private Integer cleaned;

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getCalibrationFormula() { return calibrationFormula; }
    public void setCalibrationFormula(String calibrationFormula) {
        this.calibrationFormula = calibrationFormula;
    }
    public String getTelemetryCode() { return telemetryCode; }
    public void setTelemetryCode(String telemetryCode) { this.telemetryCode = telemetryCode; }
    public Long getSampleTime() { return sampleTime; }
    public void setSampleTime(Long sampleTime) { this.sampleTime = sampleTime; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public Integer getOutlier() { return outlier; }
    public void setOutlier(Integer outlier) { this.outlier = outlier; }
    public Integer getCleaned() { return cleaned; }
    public void setCleaned(Integer cleaned) { this.cleaned = cleaned; }
}
