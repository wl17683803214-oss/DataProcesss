package com.example.common.greptime;

/** 单个已勾选参数的内部查询条件，不作为接口入参。 */
public final class TelemetryParameterQuery {
    /** 勾选列表中的位置，用于区分不同设备卫星的同名参数并恢复返回顺序。 */
    private final int selectionIndex;
    /** 来源类型，复用公共来源枚举的数据库编码。 */
    private final String sourceType;
    /** 本地设备身份；远程查询为空，沿用任务内全部远程身份的范围。 */
    private final String sourceIdentity;
    /** 完整遥测代号。 */
    private final String telemetryCode;

    /** 固定一次请求中的参数身份，避免批量查询期间改变筛选条件。 */
    public TelemetryParameterQuery(int selectionIndex, String sourceType,
            String sourceIdentity, String telemetryCode) {
        this.selectionIndex = selectionIndex;
        this.sourceType = sourceType;
        this.sourceIdentity = sourceIdentity;
        this.telemetryCode = telemetryCode;
    }

    /** 获取勾选列表位置。 */
    public int getSelectionIndex() {
        return selectionIndex;
    }

    /** 获取来源类型编码。 */
    public String getSourceType() {
        return sourceType;
    }

    /** 获取可选的本地设备身份。 */
    public String getSourceIdentity() {
        return sourceIdentity;
    }

    /** 获取完整遥测代号。 */
    public String getTelemetryCode() {
        return telemetryCode;
    }
}
