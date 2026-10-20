package com.example.common.enums;

/** 整帧检查结果，数据库使用英文编码，页面使用中文名称。 */
public enum TelemetryFrameStatus {
    /** 整帧处理或检查正常。 */
    NORMAL("normal", "正常"),
    /** 远程帧检查失败或本地参数解析失败。 */
    ABNORMAL("abnormal", "异常");

    /** 数据库保存的英文编码。 */
    private final String value;

    /** 帧检查结果中文名称。 */
    private final String label;

    TelemetryFrameStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 返回数据库编码。 */
    public String getValue() {
        return value;
    }

    /** 将数据库编码转换为中文显示名称。 */
    public static String labelOf(String value) {
        for (TelemetryFrameStatus status : values()) {
            if (status.value.equals(value)) {
                return status.label;
            }
        }
        return "未知";
    }

    /** 返回页面使用的中文检查结果。 */
    public String getLabel() {
        return label;
    }
}
