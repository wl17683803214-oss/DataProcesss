package com.example.dataadmin.enums;

/** 遥测帧检查结果枚举。 */
public enum TelemetryFrameCheckResult {

    /** 未获得明确的帧检查结果。 */
    UNKNOWN(0, "未知"),

    /** 遥测帧检查通过。 */
    CORRECT(1, "正确"),

    /** 遥测帧检查失败。 */
    ERROR(2, "错误");

    /** IoTDB中保存的检查结果编码。 */
    private final int code;

    /** 接口返回的检查结果中文名称。 */
    private final String description;

    TelemetryFrameCheckResult(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据IoTDB中的检查结果编码获取对应枚举。 */
    public static TelemetryFrameCheckResult fromCode(int code) {
        for (TelemetryFrameCheckResult result : values()) {
            if (result.code == code) {
                return result;
            }
        }
        return UNKNOWN;
    }
}
