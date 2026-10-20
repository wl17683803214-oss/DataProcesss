package com.example.common.greptime;

/** 遥测数据来源，数据库保存英文编码。 */
public enum TelemetrySourceType {
    /** 本地参数计算来源。 */
    LOCAL("local", "本地"),
    /** 远程处理结果来源。 */
    RPC("rpc", "远程");

    /** 持久化编码。 */
    private final String value;
    /** 中文展示名称。 */
    private final String label;

    TelemetrySourceType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 获取数据库编码。 */
    public String getValue() {
        return value;
    }

    /** 获取页面展示名称。 */
    public String getLabel() {
        return label;
    }
}
