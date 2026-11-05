package com.example.common.greptime;

/** 批量查询结果，复用遥测存储字段并携带内部勾选位置。 */
public class TelemetryParameterBatchRecord extends TelemetryParameterRecord {
    /** 对应请求中勾选列表的位置，不作为接口出参。 */
    private int selectionIndex;

    /** 获取结果对应的勾选位置。 */
    public int getSelectionIndex() {
        return selectionIndex;
    }

    /** 由映射组件填写结果对应的勾选位置。 */
    public void setSelectionIndex(int selectionIndex) {
        this.selectionIndex = selectionIndex;
    }
}
