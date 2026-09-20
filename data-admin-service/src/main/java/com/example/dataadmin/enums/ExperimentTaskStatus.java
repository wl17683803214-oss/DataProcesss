package com.example.dataadmin.enums;

/** 调度系统试验任务生命周期状态。 */
public enum ExperimentTaskStatus implements LabeledEnum {

    /** 任务正在运行。 */
    RUNNING(1, "运行中"),
    /** 任务自然完成。 */
    COMPLETED(2, "已完成"),
    /** 任务被人工停止。 */
    CANCELED(3, "已取消");

    private final int code;
    private final String label;

    ExperimentTaskStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    @Override
    public Object getValue() { return code; }
    @Override
    public String getLabel() { return label; }

    /** 根据状态编码返回中文名称。 */
    public static String labelOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (ExperimentTaskStatus status : values()) {
            if (status.code == code) {
                return status.label;
            }
        }
        return "未知";
    }
}
