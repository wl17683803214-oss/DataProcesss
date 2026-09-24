package com.example.dataadmin.enums;

/** 交互系统心跳状态。 */
public enum InteractionHeartbeatStatus {

    /** 系统运行正常。 */
    NORMAL("0", "正常"),

    /** 系统运行异常。 */
    ABNORMAL("1", "异常");

    /** 交互系统要求的状态编码。 */
    private final String code;

    /** 状态中文说明。 */
    private final String label;

    InteractionHeartbeatStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
