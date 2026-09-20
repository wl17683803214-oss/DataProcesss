package com.example.dataprocess.entity;

/** 数据处理服务使用的校准通道运行配置。 */
public class CalibrationChannelRuntimeConfig {
    /** 通道主键。 */ private Long id;
    /** 任务主键。 */ private String taskId;
    /** 校准公式。 */ private String calibrationFormula;
    /** 通道名称。 */ private String channelName;
    /** 正常范围。 */ private String normalRange;
    /** 检测方法。 */ private Integer detectMethod;
    /** 莱特准则倍数。 */ private Double sigmaValue;
    /** 阈值浮动百分比。 */ private Double fluctuationRate;
    /** 肖维涅系数。 */ private Double chauvenetCoef;
    /** 最少样本数。 */ private Integer minSampleCount;
    /** 迭代次数。 */ private Integer iterateCount;
    /** 最近样本数。 */ private Integer sampleWindow;
    /** 是否动态更新。 */ private Integer dynamicUpdate;
    /** 是否自动清洗。 */ private Integer autoClean;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getCalibrationFormula() { return calibrationFormula; }
    public void setCalibrationFormula(String calibrationFormula) { this.calibrationFormula = calibrationFormula; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getNormalRange() { return normalRange; }
    public void setNormalRange(String normalRange) { this.normalRange = normalRange; }
    public Integer getDetectMethod() { return detectMethod; }
    public void setDetectMethod(Integer detectMethod) { this.detectMethod = detectMethod; }
    public Double getSigmaValue() { return sigmaValue; }
    public void setSigmaValue(Double sigmaValue) { this.sigmaValue = sigmaValue; }
    public Double getFluctuationRate() { return fluctuationRate; }
    public void setFluctuationRate(Double fluctuationRate) { this.fluctuationRate = fluctuationRate; }
    public Double getChauvenetCoef() { return chauvenetCoef; }
    public void setChauvenetCoef(Double chauvenetCoef) { this.chauvenetCoef = chauvenetCoef; }
    public Integer getMinSampleCount() { return minSampleCount; }
    public void setMinSampleCount(Integer minSampleCount) { this.minSampleCount = minSampleCount; }
    public Integer getIterateCount() { return iterateCount; }
    public void setIterateCount(Integer iterateCount) { this.iterateCount = iterateCount; }
    public Integer getSampleWindow() { return sampleWindow; }
    public void setSampleWindow(Integer sampleWindow) { this.sampleWindow = sampleWindow; }
    public Integer getDynamicUpdate() { return dynamicUpdate; }
    public void setDynamicUpdate(Integer dynamicUpdate) { this.dynamicUpdate = dynamicUpdate; }
    public Integer getAutoClean() { return autoClean; }
    public void setAutoClean(Integer autoClean) { this.autoClean = autoClean; }
}

