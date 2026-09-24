package com.example.dataadmin.enums;

/** 遥测参数筛选树节点类型。 */
public enum TelemetryFilterNodeType {
    /** 设备或卫星工作表。 */
    DEVICE("device", "设备卫星"),
    /** 所属系统。 */
    SYSTEM("system", "所属系统"),
    /** 可勾选遥测参数。 */
    PARAMETER("parameter", "遥测参数");

    private final String code;
    private final String label;

    TelemetryFilterNodeType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
