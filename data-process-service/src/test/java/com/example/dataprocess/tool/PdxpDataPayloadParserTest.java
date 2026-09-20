package com.example.dataprocess.tool;

import com.example.dataprocess.protocol.rpc.PdxpDataPayload;
import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** PDXP数据域动态长度拆分测试。 */
class PdxpDataPayloadParserTest {

    /** 验证中间遥测数据长度由实际数据域长度动态计算。 */
    @Test
    void shouldSplitPayloadByActualLength() {
        // 准备五字节自定义数据、六字节遥测数据和四字节遥测帧头。
        byte[] payload = new byte[]{
                1, 2, 3, 4, 5,
                11, 12, 13, 14, 15, 16,
                21, 22, 23, 24};

        // 执行按实际数据域长度的拆分。
        PdxpDataPayload result = PdxpDataPayloadParser.parse(payload);

        // 验证三段数据的边界和内容均符合协议定义。
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, result.getCustomData());
        assertArrayEquals(new byte[]{11, 12, 13, 14, 15, 16}, result.getData());
        assertArrayEquals(new byte[]{21, 22, 23, 24}, result.getTelemetryHeader());
    }

    /** 验证不足九字节的数据域不能完成三段拆分。 */
    @Test
    void shouldRejectPayloadShorterThanFixedSections() {
        // 准备只包含固定分段之前的不足长度数据。
        byte[] payload = new byte[8];

        // 验证解析器明确拒绝无法包含两段固定区域的数据域。
        assertThrows(IllegalArgumentException.class,
                () -> PdxpDataPayloadParser.parse(payload));
    }
}
