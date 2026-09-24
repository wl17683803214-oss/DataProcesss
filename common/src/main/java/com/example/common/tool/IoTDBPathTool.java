package com.example.common.tool;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** IoTDB设备路径节点转换工具。 */
public final class IoTDBPathTool {

    /** 工具类不允许创建实例。 */
    private IoTDBPathTool() {
    }

    /** 将业务值转换为可稳定复用的IoTDB路径节点。 */
    public static String pathNode(String value, String fallback) {
        String normalizedFallback = normalizeFallback(fallback);
        if (value == null || value.trim().isEmpty()) {
            return normalizedFallback;
        }
        String source = value.trim();
        String safe = source.replaceAll("[^A-Za-z0-9_]", "_");
        if (safe.matches("\\d+")) {
            return "`" + safe + "`";
        }
        if (safe.equals(source)) {
            return safe;
        }
        // 转义结果追加原值校验码，避免不同特殊字符被替换后发生路径冲突。
        CRC32 crc32 = new CRC32();
        crc32.update(source.getBytes(StandardCharsets.UTF_8));
        return safe + "_" + Long.toHexString(crc32.getValue());
    }

    /** 为任务和接口生成带固定业务前缀的路径节点。 */
    public static String prefixedPathNode(
            String prefix,
            String value,
            String fallback) {
        String normalizedPrefix = normalizeFallback(prefix);
        String normalizedValue = value == null || value.trim().isEmpty()
                ? normalizeFallback(fallback) : value.trim();
        return pathNode(
                normalizedPrefix + normalizedValue,
                normalizedPrefix + normalizeFallback(fallback));
    }

    /** 将通道编码转换为统一的通道路径节点。 */
    public static String channelNode(String channelCode) {
        String normalized = channelCode == null ? "" : channelCode.trim();
        if (normalized.matches("(?i)ch.+")) {
            return pathNode(normalized, "unknown_channel");
        }
        if (normalized.matches("\\d+")) {
            return "ch" + normalized;
        }
        return pathNode(normalized, "unknown_channel");
    }

    /** 保证回退节点始终是非空安全标识。 */
    private static String normalizeFallback(String fallback) {
        if (fallback == null || fallback.trim().isEmpty()) {
            return "unknown";
        }
        return fallback.trim().replaceAll("[^A-Za-z0-9_]", "_");
    }
}
