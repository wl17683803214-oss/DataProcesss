package com.example.dataprocess.protocol.rpc;

/** PDXP协议配置中的参数解析字段，业务属性与参数解析配置保持一致。 */
public class PdxpProtocolField {

    /** 参数解析配置主键。 */
    private Long id;
    /** 序号。 */
    private Integer tableIndex;
    /** 位宽。 */
    private Integer bitWidth;
    /** 遥测名称。 */
    private String telemetryName;
    /** 遥测代号。 */
    private String telemetryCode;
    /** 公式类型。 */
    private String formulaType;
    /** 处理公式。 */
    private String formulaDesc;
    /** 处理参数。 */
    private String processParam;
    /** 小数位数。 */
    private String decimalPlaces;
    /** 是否报警。 */
    private String alarmFlag;
    /** 正常值范围。 */
    private String normalValue;
    /** 预警值范围。 */
    private String warningValue;
    /** 状态跳变信息。 */
    private String stateChangeInfo;
    /** 相关命令。 */
    private String commandCode;
    /** 所属系统。 */
    private String systemName;
    /** 控制波道。 */
    private String controlChannel;
    /** 合并波道。 */
    private String mergeChannelCount;
    /** 延时波道。 */
    private String delayChannel;
    /** 存储遥测。 */
    private String storeFlag;
    /** 校准公式。 */
    private String calibrationFormula;

    /** 获取参数解析配置主键。 */
    public Long getId() { return id; }
    /** 设置参数解析配置主键。 */
    public void setId(Long id) { this.id = id; }
    /** 获取序号。 */
    public Integer getTableIndex() { return tableIndex; }
    /** 设置序号。 */
    public void setTableIndex(Integer tableIndex) { this.tableIndex = tableIndex; }
    /** 获取位宽。 */
    public Integer getBitWidth() { return bitWidth; }
    /** 设置位宽。 */
    public void setBitWidth(Integer bitWidth) { this.bitWidth = bitWidth; }
    /** 获取遥测名称。 */
    public String getTelemetryName() { return telemetryName; }
    /** 设置遥测名称。 */
    public void setTelemetryName(String telemetryName) { this.telemetryName = telemetryName; }
    /** 获取遥测代号。 */
    public String getTelemetryCode() { return telemetryCode; }
    /** 设置遥测代号。 */
    public void setTelemetryCode(String telemetryCode) { this.telemetryCode = telemetryCode; }
    /** 获取公式类型。 */
    public String getFormulaType() { return formulaType; }
    /** 设置公式类型。 */
    public void setFormulaType(String formulaType) { this.formulaType = formulaType; }
    /** 获取处理公式。 */
    public String getFormulaDesc() { return formulaDesc; }
    /** 设置处理公式。 */
    public void setFormulaDesc(String formulaDesc) { this.formulaDesc = formulaDesc; }
    /** 获取处理参数。 */
    public String getProcessParam() { return processParam; }
    /** 设置处理参数。 */
    public void setProcessParam(String processParam) { this.processParam = processParam; }
    /** 获取小数位数。 */
    public String getDecimalPlaces() { return decimalPlaces; }
    /** 设置小数位数。 */
    public void setDecimalPlaces(String decimalPlaces) { this.decimalPlaces = decimalPlaces; }
    /** 获取是否报警。 */
    public String getAlarmFlag() { return alarmFlag; }
    /** 设置是否报警。 */
    public void setAlarmFlag(String alarmFlag) { this.alarmFlag = alarmFlag; }
    /** 获取正常值范围。 */
    public String getNormalValue() { return normalValue; }
    /** 设置正常值范围。 */
    public void setNormalValue(String normalValue) { this.normalValue = normalValue; }
    /** 获取预警值范围。 */
    public String getWarningValue() { return warningValue; }
    /** 设置预警值范围。 */
    public void setWarningValue(String warningValue) { this.warningValue = warningValue; }
    /** 获取状态跳变信息。 */
    public String getStateChangeInfo() { return stateChangeInfo; }
    /** 设置状态跳变信息。 */
    public void setStateChangeInfo(String stateChangeInfo) { this.stateChangeInfo = stateChangeInfo; }
    /** 获取相关命令。 */
    public String getCommandCode() { return commandCode; }
    /** 设置相关命令。 */
    public void setCommandCode(String commandCode) { this.commandCode = commandCode; }
    /** 获取所属系统。 */
    public String getSystemName() { return systemName; }
    /** 设置所属系统。 */
    public void setSystemName(String systemName) { this.systemName = systemName; }
    /** 获取控制波道。 */
    public String getControlChannel() { return controlChannel; }
    /** 设置控制波道。 */
    public void setControlChannel(String controlChannel) { this.controlChannel = controlChannel; }
    /** 获取合并波道。 */
    public String getMergeChannelCount() { return mergeChannelCount; }
    /** 设置合并波道。 */
    public void setMergeChannelCount(String mergeChannelCount) { this.mergeChannelCount = mergeChannelCount; }
    /** 获取延时波道。 */
    public String getDelayChannel() { return delayChannel; }
    /** 设置延时波道。 */
    public void setDelayChannel(String delayChannel) { this.delayChannel = delayChannel; }
    /** 获取存储遥测。 */
    public String getStoreFlag() { return storeFlag; }
    /** 设置存储遥测。 */
    public void setStoreFlag(String storeFlag) { this.storeFlag = storeFlag; }
    /** 获取校准公式。 */
    public String getCalibrationFormula() { return calibrationFormula; }
    /** 设置校准公式。 */
    public void setCalibrationFormula(String calibrationFormula) {
        this.calibrationFormula = calibrationFormula;
    }
}
