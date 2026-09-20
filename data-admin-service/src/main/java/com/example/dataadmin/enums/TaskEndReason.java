package com.example.dataadmin.enums;

/** 试验任务结束原因。 */
public enum TaskEndReason implements LabeledEnum {

    /** 任务自然完成。 */
    COMPLETED(ExperimentTaskStatus.COMPLETED, "自然完成"),
    /** 任务被人工停止。 */
    CANCELED(ExperimentTaskStatus.CANCELED, "人工停止");

    private final ExperimentTaskStatus taskStatus;
    private final String label;

    TaskEndReason(ExperimentTaskStatus taskStatus, String label) {
        this.taskStatus = taskStatus;
        this.label = label;
    }

    public ExperimentTaskStatus getTaskStatus() { return taskStatus; }
    @Override
    public Object getValue() { return name(); }
    @Override
    public String getLabel() { return label; }

    /** 校验并返回结束原因。 */
    public static TaskEndReason require(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("任务结束原因不能为空");
        }
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的任务结束原因：" + value);
        }
    }

    /** 根据结束原因编码返回中文名称。 */
    public static String labelOf(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            return valueOf(value.trim()).label;
        } catch (IllegalArgumentException exception) {
            return "未知";
        }
    }
}
