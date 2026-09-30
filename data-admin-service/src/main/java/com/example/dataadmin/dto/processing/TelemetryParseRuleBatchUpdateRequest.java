package com.example.dataadmin.dto.processing;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/** 一次提交当前任务下多条遥测参数的完整修改内容。 */
public class TelemetryParseRuleBatchUpdateRequest {
    /** 所有待修改参数所属的试验任务。 */
    @NotBlank(message = "试验任务编号不能为空")
    private String taskId;
    /** 每条参数可以有不同的修改值，单次最多处理二百条。 */
    @Valid
    @NotEmpty(message = "请选择要修改的遥测参数")
    @Size(max = 200, message = "单次最多修改200条遥测参数")
    private List<Item> items;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { this.taskId = value; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> value) { this.items = value; }

    /** 单条参数的可编辑字段；系统名称由系统主键计算。 */
    public static class Item {
        /** 参数解析配置主键。 */
        @NotNull(message = "参数解析配置主键不能为空")
        private Long id;
        /** 原设备卫星主键，仅用于校验归属。 */
        @NotNull(message = "设备卫星主键不能为空")
        private Long deviceSatelliteId;
        /** 所属系统主键，空值表示直属设备。 */
        private Long systemId;
        /** 大表序号。 */
        @NotBlank(message = "大表序号不能为空")
        private String tableIndex;
        /** 位宽。 */
        @NotBlank(message = "位宽不能为空")
        private String bitWidth;
        /** 遥测参数名称。 */
        @NotBlank(message = "遥测名称不能为空")
        private String telemetryName;
        /** 遥测代号。 */
        @NotBlank(message = "遥测代号不能为空")
        private String telemetryCode;
        /** 公式类型。 */
        @NotBlank(message = "公式类型不能为空")
        private String formulaType;
        /** 处理公式。 */
        private String formulaDesc;
        /** 处理参数。 */
        private String processParam;
        /** 小数位数。 */
        private String decimalPlaces;
        /** 是否报警：0否、1是。 */
        private String alarmFlag;
        /** 正常值范围。 */
        private String normalValue;
        /** 预警值范围。 */
        private String warningValue;
        /** 状态跳变信息。 */
        private String stateChangeInfo;
        /** 相关命令。 */
        private String commandCode;
        /** 控制波道。 */
        private String controlChannel;
        /** 合并波道。 */
        private String mergeChannelCount;
        /** 延时波道。 */
        private String delayChannel;
        /** 存储遥测：0否、1是。 */
        private String storeFlag;
        /** 校准公式。 */
        private String calibrationFormula;

        public Long getId() { return id; }
        public void setId(Long value) { this.id = value; }
        public Long getDeviceSatelliteId() { return deviceSatelliteId; }
        public void setDeviceSatelliteId(Long value) { this.deviceSatelliteId = value; }
        public Long getSystemId() { return systemId; }
        public void setSystemId(Long value) { this.systemId = value; }
        public String getTableIndex() { return tableIndex; }
        public void setTableIndex(String value) { this.tableIndex = value; }
        public String getBitWidth() { return bitWidth; }
        public void setBitWidth(String value) { this.bitWidth = value; }
        public String getTelemetryName() { return telemetryName; }
        public void setTelemetryName(String value) { this.telemetryName = value; }
        public String getTelemetryCode() { return telemetryCode; }
        public void setTelemetryCode(String value) { this.telemetryCode = value; }
        public String getFormulaType() { return formulaType; }
        public void setFormulaType(String value) { this.formulaType = value; }
        public String getFormulaDesc() { return formulaDesc; }
        public void setFormulaDesc(String value) { this.formulaDesc = value; }
        public String getProcessParam() { return processParam; }
        public void setProcessParam(String value) { this.processParam = value; }
        public String getDecimalPlaces() { return decimalPlaces; }
        public void setDecimalPlaces(String value) { this.decimalPlaces = value; }
        public String getAlarmFlag() { return alarmFlag; }
        public void setAlarmFlag(String value) { this.alarmFlag = value; }
        public String getNormalValue() { return normalValue; }
        public void setNormalValue(String value) { this.normalValue = value; }
        public String getWarningValue() { return warningValue; }
        public void setWarningValue(String value) { this.warningValue = value; }
        public String getStateChangeInfo() { return stateChangeInfo; }
        public void setStateChangeInfo(String value) { this.stateChangeInfo = value; }
        public String getCommandCode() { return commandCode; }
        public void setCommandCode(String value) { this.commandCode = value; }
        public String getControlChannel() { return controlChannel; }
        public void setControlChannel(String value) { this.controlChannel = value; }
        public String getMergeChannelCount() { return mergeChannelCount; }
        public void setMergeChannelCount(String value) { this.mergeChannelCount = value; }
        public String getDelayChannel() { return delayChannel; }
        public void setDelayChannel(String value) { this.delayChannel = value; }
        public String getStoreFlag() { return storeFlag; }
        public void setStoreFlag(String value) { this.storeFlag = value; }
        public String getCalibrationFormula() { return calibrationFormula; }
        public void setCalibrationFormula(String value) { this.calibrationFormula = value; }
    }
}
