package com.example.common.greptime;

/** 两张遥测表共用的无损身份规则。 */
public final class TelemetryIdentity {
    private TelemetryIdentity() {
    }

    /** 本地身份使用十进制主键，查询时可以直接还原。 */
    public static String local(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("本地遥测缺少有效设备卫星主键");
        }
        return String.valueOf(deviceId);
    }

    /** 远程身份以编码长度分隔，保留特殊字符并避免组合冲突。 */
    public static String rpc(String satelliteCode, String channelCode) {
        String satellite = satelliteCode == null ? "" : satelliteCode.trim();
        String channel = channelCode == null ? "" : channelCode.trim();
        if (satellite.isEmpty() || channel.isEmpty()) {
            throw new IllegalArgumentException("远程遥测缺少卫星代号或通道代号");
        }
        return satellite.length() + ":" + satellite + channel.length() + ":" + channel;
    }

    /** 远程帧保持没有本地主键的原有接口行为。 */
    public static Long deviceId(String sourceType, String identity) {
        if (!TelemetrySourceType.LOCAL.getValue().equals(sourceType)) {
            return null;
        }
        return Long.valueOf(identity);
    }
}
