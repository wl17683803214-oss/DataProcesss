package com.example.dataprocess.enums;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;

/** 本地处理公式类型与消息字段类型的对应关系。 */
public enum LocalFormulaType {
    /** 单精度物理量。 */
    FLOAT("300", ValueType.VALUE_TYPE_FLOAT),
    /** 双精度物理量。 */
    DOUBLE("301", ValueType.VALUE_TYPE_DOUBLE),
    /** 文本参数。 */
    TEXT("302", ValueType.VALUE_TYPE_TEXT),
    /** 以十分之一毫秒为单位的时间参数。 */
    TIME("303", ValueType.VALUE_TYPE_TIME),
    /** 未约定类型沿用原码处理，兼容已有协议。 */
    RAW("", ValueType.VALUE_TYPE_RAW);

    /** 协议配置中的类型编号。 */
    private final String code;
    /** 消息中的字段类型。 */
    private final ValueType valueType;

    /** 保存类型映射。 */
    LocalFormulaType(String code, ValueType valueType) {
        this.code = code;
        this.valueType = valueType;
    }

    /** 获取消息字段类型。 */
    public ValueType getValueType() {
        return valueType;
    }

    /** 按配置编号查找类型，空值及旧类型保留原码语义。 */
    public static LocalFormulaType fromCode(String code) {
        String normalized = code == null ? "" : code.trim();
        for (LocalFormulaType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return RAW;
    }
}
