package com.example.common.enums;

/** Lua参数声明的英文标识与中文含义，不根据配置内容猜测参数类型。 */
public enum FormulaParameterType {
    /** 保持原有数值常量及默认参数行为。 */
    NUMBER("number", "数值常量"),
    /** 使用同帧遥测的小端无符号原码整数。 */
    TELEMETRY_RAW("telemetry_raw", "遥测原码");

    /** 脚本中的英文类型标识。 */
    private final String value;
    /** 用于中文说明和错误信息。 */
    private final String label;

    /** 初始化已有类型定义，不暴露其他Java能力。 */
    FormulaParameterType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 读取脚本类型标识。 */
    public String getValue() { return value; }
    /** 读取中文类型名称。 */
    public String getLabel() { return label; }

    /** 只接受明确支持的声明，错误声明不能降级为数值参数。 */
    public static FormulaParameterType require(String value) {
        for (FormulaParameterType type : values()) {
            if (type.value.equals(value)) { return type; }
        }
        throw new IllegalArgumentException("不支持的公式参数类型：" + value);
    }
}
