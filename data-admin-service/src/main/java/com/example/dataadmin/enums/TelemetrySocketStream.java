package com.example.dataadmin.enums;

/** 遥测连接可订阅的页面数据类型。 */
public enum TelemetrySocketStream {
    PROCESSED("processed"),
    CURVE("curve"),
    NORMAL("normal"),
    ABNORMAL("abnormal");

    private final String value;

    TelemetrySocketStream(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 只接受协议定义的订阅类型。 */
    public static TelemetrySocketStream require(String value) {
        for (TelemetrySocketStream stream : values()) {
            if (stream.value.equals(value)) {
                return stream;
            }
        }
        throw new IllegalArgumentException("遥测订阅类别无效");
    }
}
