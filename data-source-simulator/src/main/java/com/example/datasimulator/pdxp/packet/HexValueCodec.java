package com.example.datasimulator.pdxp.packet;

/** 用户十六进制配置值的校验和转换工具。 */
public final class HexValueCodec {

    /** 工具类不允许实例化。 */
    private HexValueCodec() {
    }

    /** 将按大端阅读填写的十六进制值转换为小端字节。 */
    public static byte[] decodeLittleEndian(
            String hexValue,
            int expectedLength,
            String fieldName) {
        // 第一步：去除常见分隔符，允许配置值按连续或空格分隔形式填写。
        String normalized = normalize(hexValue);
        int expectedCharacters = expectedLength * 2;
        if (normalized.length() != expectedCharacters) {
            throw new IllegalArgumentException(
                    fieldName + "必须填写" + expectedLength
                            + "字节十六进制值，当前字符数："
                            + normalized.length());
        }

        // 第二步：按用户的大端阅读顺序转换为字节。
        byte[] bigEndian = new byte[expectedLength];
        for (int index = 0; index < expectedLength; index++) {
            int start = index * 2;
            try {
                bigEndian[index] = (byte) Integer.parseInt(
                        normalized.substring(start, start + 2), 16);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        fieldName + "包含非十六进制字符：" + hexValue,
                        exception);
            }
        }

        // 第三步：整体反转当前字段，使低位字节先发送。
        byte[] littleEndian = new byte[expectedLength];
        for (int index = 0; index < expectedLength; index++) {
            littleEndian[index] = bigEndian[expectedLength - 1 - index];
        }
        return littleEndian;
    }

    /** 将十六进制配置转换为无符号长整数。 */
    public static long decodeUnsignedLong(
            String hexValue,
            int expectedLength,
            String fieldName) {
        // 第一步：复用统一格式清理和长度校验。
        String normalized = normalize(hexValue);
        if (normalized.length() != expectedLength * 2) {
            throw new IllegalArgumentException(
                    fieldName + "必须填写" + expectedLength
                            + "字节十六进制值");
        }
        // 第二步：四字节以内的配置可以安全保存到Java长整数。
        try {
            return Long.parseLong(normalized, 16);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    fieldName + "包含非十六进制字符：" + hexValue,
                    exception);
        }
    }

    /** 清理十六进制前缀、空格和常用分隔符。 */
    private static String normalize(String hexValue) {
        // 空配置无法构造报文，直接给出明确提示。
        if (hexValue == null) {
            throw new IllegalArgumentException("十六进制配置不能为空");
        }
        return hexValue.trim()
                .replace("0x", "")
                .replace("0X", "")
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }
}
