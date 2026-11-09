package com.example.common.greptime;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** 统一生成对象分表名称，设备使用主键，卫星使用原始代号的无损编码。 */
public final class GreptimeTableRoute {
    /** 参数表固定前缀。 */
    private static final String PARAMETER_PREFIX = "telemetry_parameter_";
    /** 整帧表固定前缀。 */
    private static final String FRAME_PREFIX = "telemetry_frame_";
    /** 只接受由本组件生成的设备或卫星分表名。 */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "telemetry_(?:parameter|frame)_(?:device_[1-9][0-9]*|sat_(?:[0-9a-f]{2})+)");
    /** 十六进制字符表，避免大小写和特殊字符在数据库标识符中产生歧义。 */
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    /** 同一对象两张表共用的固定后缀。 */
    private final String suffix;

    /** 保存已经校验的对象后缀。 */
    private GreptimeTableRoute(String suffix) {
        this.suffix = suffix;
    }

    /** 两个服务使用同一规则，卫星原始代号仍保存在记录身份字段中。 */
    public static GreptimeTableRoute of(String sourceType, String sourceIdentity) {
        // 第一步：本地身份必须是有效主键，并统一转换为十进制文本。
        if (TelemetrySourceType.LOCAL.getValue().equals(sourceType)) {
            try {
                return new GreptimeTableRoute("device_" + TelemetryIdentity.local(Long.valueOf(sourceIdentity)));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("设备分表缺少有效的设备卫星主键", exception);
            }
        }
        // 第二步：远程代号去掉两端空白后按原始字节编码，保留大小写差异。
        if (TelemetrySourceType.RPC.getValue().equals(sourceType)) {
            byte[] bytes = TelemetryIdentity.rpc(sourceIdentity).getBytes(StandardCharsets.UTF_8);
            StringBuilder encoded = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                encoded.append(HEX[(value & 0xff) >>> 4]);
                encoded.append(HEX[value & 0x0f]);
            }
            return new GreptimeTableRoute("sat_" + encoded);
        }
        throw new IllegalArgumentException("遥测分表的数据来源不正确");
    }

    /** 获取对象参数表名。 */
    public String parameterTable() {
        return PARAMETER_PREFIX + suffix;
    }

    /** 获取对象整帧表名。 */
    public String frameTable() {
        return FRAME_PREFIX + suffix;
    }

    /** 目录和执行层共用严格校验，禁止将前端文本作为表名执行。 */
    public static String requireTable(String name) {
        if (name == null || !TABLE_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("遥测分表名称不合法");
        }
        return name;
    }

    /** 清理时只处理本系统的对象分表，不包含旧公共表或目录表。 */
    public static boolean isShardTable(String name) {
        return name != null && TABLE_PATTERN.matcher(name).matches();
    }
}
