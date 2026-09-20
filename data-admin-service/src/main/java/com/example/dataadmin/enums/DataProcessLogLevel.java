package com.example.dataadmin.enums;

/** 数据处理日志级别枚举。 */
public enum DataProcessLogLevel implements LabeledEnum {

    /** 普通处理信息。 */
    INFO(1, "信息"),

    /** 不影响本批任务继续执行的警告。 */
    WARNING(2, "警告"),

    /** 数据处理失败或处理结果异常。 */
    ERROR(3, "错误");

    /** 保存到数据库中的级别编码。 */
    private final int code;

    /** 前端展示使用的中文说明。 */
    private final String description;

    DataProcessLogLevel(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    @Override
    public Object getValue() {
        // 枚举查询接口沿用数据库保存的数字编码。
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getLabel() {
        // 枚举查询接口沿用原有中文说明。
        return description;
    }

    /** 判断请求中的日志级别编码是否有效。 */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (DataProcessLogLevel level : values()) {
            if (level.code == code) {
                return true;
            }
        }
        return false;
    }

    /** 根据枚举编码返回中文说明，未知或空编码返回null。 */
    public static String descriptionOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataProcessLogLevel level : values()) {
            if (level.code == code) {
                return level.description;
            }
        }
        return null;
    }
}
