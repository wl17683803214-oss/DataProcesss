package com.example.dataprocess.protocol.rpc;

import java.util.Arrays;

/** 按实际长度拆分PDXP数据域。 */
public final class PdxpDataPayloadParser {

    /** 自定义数据长度。 */
    public static final int CUSTOM_DATA_LENGTH = 5;
    /** 遥测帧头长度。 */
    public static final int TELEMETRY_HEADER_LENGTH = 4;

    /** 工具类不允许实例化。 */
    private PdxpDataPayloadParser() {
    }

    /** 按五字节、中间遥测数据和四字节拆分PDXP数据域。 */
    public static PdxpDataPayload parse(byte[] payload) {
        // 第一步：数据域至少需要包含五字节自定义数据和四字节遥测帧头。
        int minimumPayloadLength = CUSTOM_DATA_LENGTH
                + TELEMETRY_HEADER_LENGTH;
        if (payload == null || payload.length < minimumPayloadLength) {
            throw new IllegalArgumentException(
                    "PDXP数据域长度不能少于" + minimumPayloadLength
                            + "字节，实际长度："
                            + (payload == null ? 0 : payload.length));
        }

        // 第二步：前五字节是用户填写的自定义数据。
        byte[] customData = Arrays.copyOfRange(
                payload, 0, CUSTOM_DATA_LENGTH);
        // 第三步：根据实际数据域长度计算中间遥测数据长度。
        int telemetryDataEnd = payload.length - TELEMETRY_HEADER_LENGTH;
        // 第四步：中间全部数据作为遥测原始数据。
        byte[] data = Arrays.copyOfRange(
                payload,
                CUSTOM_DATA_LENGTH,
                telemetryDataEnd);
        // 第五步：末尾四字节是从原文件开头移动过来的遥测帧头。
        byte[] telemetryHeader = Arrays.copyOfRange(
                payload,
                telemetryDataEnd,
                payload.length);
        return new PdxpDataPayload(
                customData, data, telemetryHeader);
    }
}
