package com.example.simulator.enums;

/** UDP发送模式使用英文值持久化，并提供中文展示名称。 */
public enum SendMode {
    UNICAST("UNICAST", "单播"),
    MULTICAST("MULTICAST", "组播");

    public final String value;
    public final String label;

    SendMode(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 解析传入值；未指定时沿用现有单播行为。 */
    public static SendMode of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNICAST;
        }
        for (SendMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("发送模式只能选择UNICAST或MULTICAST");
    }
}
