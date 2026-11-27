package com.example.dataadmin.enums;

import com.example.common.appstore.AppStoreLogLevel;

/**
 * 系统运行日志级别枚举。
 *
 * 枚举编码直接保存到sys_runtime_log.log_level，并作为接口查询参数返回。
 */
public enum SystemLogLevel implements LabeledEnum {

    /** 普通运行信息。 */
    INFO(AppStoreLogLevel.INFO),

    /** 不影响系统继续运行的警告。 */
    WARN(AppStoreLogLevel.WARN),

    /** 系统功能执行失败或运行异常。 */
    ERROR(AppStoreLogLevel.ERROR);

    /** 复用相同的编码和中文名称，系统日志仍只录入这三个级别。 */
    private final AppStoreLogLevel reportLevel;

    SystemLogLevel(AppStoreLogLevel reportLevel) {
        this.reportLevel = reportLevel;
    }

    public String getCode() {
        return reportLevel.name();
    }

    @Override
    public Object getValue() {
        // 枚举查询接口沿用数据库保存的字符串编码。
        return getCode();
    }

    public String getDescription() {
        return reportLevel.getLabel();
    }

    @Override
    public String getLabel() {
        // 枚举查询接口沿用原有中文说明。
        return getDescription();
    }

    /**
     * 校验并统一日志级别编码。
     *
     * @param code 请求或待写入的日志级别
     * @return 大写的标准枚举编码；未传时返回null
     */
    public static String normalize(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (SystemLogLevel level : values()) {
            if (level.getCode().equals(normalized)) {
                return normalized;
            }
        }
        throw new IllegalArgumentException(
                "日志级别只能为INFO、WARN或ERROR");
    }

    /** 根据枚举编码返回中文说明，未知或空编码返回null。 */
    public static String descriptionOf(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (SystemLogLevel level : values()) {
            if (level.getCode().equals(normalized)) {
                return level.getLabel();
            }
        }
        return null;
    }

    /** 根据已校验的数据库级别取得对应的上报级别。 */
    public static AppStoreLogLevel reportLevelOf(String code) {
        for (SystemLogLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level.reportLevel;
            }
        }
        return null;
    }
}
