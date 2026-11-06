package com.example.common.greptime;

/** 两张遥测表共用的来源身份规则。 */
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

    /** 远程身份只保存卫星代号，不再按通道区分。 */
    public static String rpc(String satelliteCode) {
        // 第一步：去掉卫星代号两端空白，保持身份格式一致。
        String satellite = satelliteCode == null ? "" : satelliteCode.trim();
        // 第二步：缺少卫星代号时拒绝写入，避免空身份数据混在一起。
        if (satellite.isEmpty()) {
            throw new IllegalArgumentException("远程遥测缺少卫星代号");
        }
        // 第三步：直接使用卫星代号作为整帧和参数的远程身份。
        return satellite;
    }

    /** 远程帧保持没有本地主键的原有接口行为。 */
    public static Long deviceId(String sourceType, String identity) {
        if (!TelemetrySourceType.LOCAL.getValue().equals(sourceType)) {
            return null;
        }
        return Long.valueOf(identity);
    }
}
