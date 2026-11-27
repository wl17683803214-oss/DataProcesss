package com.example.common.appstore;

/** 应用商店日志接口支持的等级，传输使用枚举名，展示使用中文名称。 */
public enum AppStoreLogLevel {
    DEBUG("调试"),
    INFO("信息"),
    WARN("警告"),
    ERROR("错误"),
    FATAL("致命");

    /** 日志等级的中文名称。 */
    private final String label;

    AppStoreLogLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
