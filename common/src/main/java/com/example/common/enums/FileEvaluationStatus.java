package com.example.common.enums;

/** 文件和图像共用的评价状态，数据库保存英文编码，查询返回中文名称。 */
public enum FileEvaluationStatus {
    /** 文件接收登记时的默认评价状态。 */
    COMPLETED("completed", "完成");

    /** 数据库保存的英文编码。 */
    private final String value;
    /** 页面展示的中文名称。 */
    private final String label;

    FileEvaluationStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 根据落库编码查找对应的中文名称。 */
    public static String labelOf(String value) {
        for (FileEvaluationStatus status : values()) {
            if (status.value.equals(value)) {
                return status.label;
            }
        }
        return "未知";
    }
}
