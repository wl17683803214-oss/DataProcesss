package com.example.dataprocess.entity;

/** RPC遥测代号索引与遥测代号的轻量映射。 */
public class TelemetryCodeMapping {

    /** RPC返回结果使用的遥测代号索引。 */
    private Integer tableIndex;
    /** TelemetryMessage映射使用的遥测代号。 */
    private String telemetryCode;

    public Integer getTableIndex() {
        return tableIndex;
    }

    public void setTableIndex(Integer tableIndex) {
        this.tableIndex = tableIndex;
    }

    public String getTelemetryCode() {
        return telemetryCode;
    }

    public void setTelemetryCode(String telemetryCode) {
        this.telemetryCode = telemetryCode;
    }
}
