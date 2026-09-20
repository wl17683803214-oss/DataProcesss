package com.example.dataprocess.tool;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PDXP 协议解析工具测试。 */
class PdxpParserTest {

    /** 验证协议样例中的四个连续数据包。 */
    @Test
    void shouldParseHexTextSample() throws Exception {
        // 准备与协议样例一致的空白分隔十六进制数据。
        String sample = "80 01 00 01 00 00 10 02 00 00 20 03 00 00 30 64 "
                + "00 00 00 5A 00 00 00 00 C7 25 00 E2 02 22 0C 00 "
                + "48 65 6C 6C 6F 2C 20 50 44 58 50 21 "
                + "80 FF 00 11 11 11 11 22 22 22 22 01 00 00 00 01 "
                + "00 00 00 C0 00 00 00 00 C7 25 80 E4 02 24 04 00 50 49 4E 47 "
                + "80 00 10 88 88 88 88 99 99 99 99 02 00 00 00 05 "
                + "00 00 00 4F 00 00 00 00 C7 25 80 E4 02 24 02 00 AA BB "
                + "80 05 00 33 33 33 33 44 44 44 44 FF 00 00 00 00 "
                + "01 00 00 90 00 00 00 00 C7 25 80 E4 02 24 08 00 "
                + "01 02 03 04 05 06 07 08";

        // 使用自动模式解析十六进制文本。
        List<PdxpParser.PdxpPacket> packets = PdxpParser.parseHexText(sample);

        // 核对数据包数量和每个数据域长度。
        assertEquals(4, packets.size());
        assertEquals(12, packets.get(0).getDataLength());
        assertEquals(4, packets.get(1).getDataLength());
        assertEquals(2, packets.get(2).getDataLength());
        assertEquals(8, packets.get(3).getDataLength());

        // 核对首包的全部关键字段及数据域。
        PdxpParser.PdxpPacket first = packets.get(0);
        assertEquals(2, first.getVersion());
        assertEquals(1, first.getMissionId());
        assertEquals(0x10000001L, first.getSourceId());
        assertEquals(0x20000002L, first.getDestinationId());
        assertEquals(0x30000003L, first.getDataId());
        assertEquals(100L, first.getSequenceNumber());
        assertEquals("Hello, PDXP!", new String(first.getData(), StandardCharsets.US_ASCII));

        // 核对处理标志每个码位的拆解结果。
        assertEquals(1, first.getProcessingFlag().getSaveFlag());
        assertEquals(3, first.getProcessingFlag().getEncryptionFlag());
        assertFalse(first.getProcessingFlag().isForceTransfer());
        assertTrue(first.getProcessingFlag().isAnswerRequired());
        assertFalse(first.getProcessingFlag().isSimulationTarget());
    }

    /** 验证 TCP 定界与转义数据可以恢复。 */
    @Test
    void shouldParseEscapedTcpFrame() throws Exception {
        // 构造数据域包含 7E 和 7D 的合法 PDXP 原始包。
        byte[] header = new byte[PdxpParser.HEADER_LENGTH];
        header[0] = (byte) 0x80;
        header[24] = 0x01;
        header[30] = 0x02;
        byte[] tcpFrame = new byte[PdxpParser.HEADER_LENGTH + 6];
        tcpFrame[0] = 0x7E;
        System.arraycopy(header, 0, tcpFrame, 1, header.length);
        tcpFrame[33] = 0x7D;
        tcpFrame[34] = 0x5E;
        tcpFrame[35] = 0x7D;
        tcpFrame[36] = 0x5D;
        tcpFrame[37] = 0x7E;

        // 按 TCP 格式执行定界和反转义解析。
        List<PdxpParser.PdxpPacket> packets = PdxpParser.parseTcp(tcpFrame);

        // 核对反转义后的两个数据字节。
        assertEquals(1, packets.size());
        assertArrayEquals(new byte[]{0x7E, 0x7D}, packets.get(0).getData());
    }

    /** 验证 TCP 接收入口可以跳过帧前杂项数据并读取一个完整帧。 */
    @Test
    void shouldReceiveOneTcpFrameFromStream() throws Exception {
        // 构造数据域为空的最小 PDXP 数据包，并添加 TCP 首尾定界符。
        byte[] tcpData = new byte[PdxpParser.HEADER_LENGTH + 4];
        tcpData[0] = 0x11;
        tcpData[1] = 0x22;
        tcpData[2] = 0x7E;
        tcpData[3] = (byte) 0x80;
        tcpData[27] = 0x01;
        tcpData[tcpData.length - 1] = 0x7E;

        // 从已有 TCP 输入流读取并解析一个完整的定界帧。
        List<PdxpParser.PdxpPacket> packets = PdxpParser.parseTcp(
                Arrays.copyOfRange(tcpData, 0, tcpData.length));

        // 核对帧前杂项数据已跳过，完整数据包成功返回。
        assertEquals(1, packets.size());
        assertEquals(0, packets.get(0).getDataLength());
    }

    /** 验证 UDP 残缺数据会被跳过。 */
    @Test
    void shouldSkipTruncatedUdpPacket() throws Exception {
        // 准备不足一个完整包头的原始二进制数据。
        byte[] truncated = new byte[]{(byte) 0x80, 0x01};

        // 确认解析器跳过残片且不会返回半成品。
        assertTrue(PdxpParser.parseUdp(truncated).isEmpty());
    }

    /** 验证 UDP 公开入口可以直接解析原始 PDXP 报文。 */
    @Test
    void shouldParseUdpPacket() throws Exception {
        // 构造一个数据域为空的最小 UDP PDXP 报文。
        byte[] udpData = new byte[PdxpParser.HEADER_LENGTH];
        udpData[0] = (byte) 0x80;
        udpData[24] = 0x01;

        // 使用明确标注的 UDP 入口解析报文。
        List<PdxpParser.PdxpPacket> packets = PdxpParser.parseUdp(udpData);

        // 核对最小报文能够正常解析且数据域为空。
        assertEquals(1, packets.size());
        assertEquals(0, packets.get(0).getDataLength());
        // 完整原始包必须保留，供IoTDB原始帧和RPC待确认data字段使用。
        assertArrayEquals(udpData, packets.get(0).getRawPacket());
    }

    /** 验证积日为零的 UDP 报文会被跳过。 */
    @Test
    void shouldSkipUdpPacketWithInvalidDate() throws Exception {
        // 构造版本和长度正确、但发送日期积日为零的异常报文。
        byte[] udpData = new byte[PdxpParser.HEADER_LENGTH];
        udpData[0] = (byte) 0x80;

        // UDP 工具应丢弃整个异常报文，不能生成 1999 年的伪数据。
        assertTrue(PdxpParser.parseUdp(udpData).isEmpty());
    }

    /** 验证 UDP 异常报文会整条跳过，不会在内部猜测包头。 */
    @Test
    void shouldSkipWholeInvalidUdpDatagram() throws Exception {
        // 构造两个可以独立解析的最小 PDXP 数据包。
        byte[] firstPacket = new byte[PdxpParser.HEADER_LENGTH];
        firstPacket[0] = (byte) 0x80;
        firstPacket[15] = 0x01;
        firstPacket[24] = 0x01;
        byte[] secondPacket = new byte[PdxpParser.HEADER_LENGTH];
        secondPacket[0] = (byte) 0x80;
        secondPacket[15] = 0x02;
        secondPacket[24] = 0x01;

        // 在两个完整包之间插入无法构成 PDXP 包头的异常字节。
        byte[] udpData = new byte[firstPacket.length + 3 + secondPacket.length + 2];
        System.arraycopy(firstPacket, 0, udpData, 0, firstPacket.length);
        udpData[firstPacket.length] = 0x11;
        udpData[firstPacket.length + 1] = 0x22;
        udpData[firstPacket.length + 2] = 0x33;
        System.arraycopy(
                secondPacket,
                0,
                udpData,
                firstPacket.length + 3,
                secondPacket.length);
        udpData[udpData.length - 2] = (byte) 0x80;
        udpData[udpData.length - 1] = 0x01;

        // 严格调用公共 PDXP 解析器，不允许在报文内部逐字节寻找包头。
        assertThrows(
                java.io.IOException.class,
                () -> PdxpParser.parseBinary(udpData));
    }

    /** 验证生成的 PDXP 样例文件可以完整解析。 */
    @Test
    void shouldParseGeneratedSampleFile() throws Exception {
        // 从项目文档目录读取新生成的五条十六进制样例数据。
        List<PdxpParser.PdxpPacket> packets = PdxpParser.parse(
                Paths.get("..", "docs", "pdxp-generated-samples.dat"));

        // 核对数据包数量以及各包的数据域长度。
        assertEquals(5, packets.size());
        assertEquals(5, packets.get(0).getDataLength());
        assertEquals(4, packets.get(1).getDataLength());
        assertEquals(4, packets.get(2).getDataLength());
        assertEquals(8, packets.get(3).getDataLength());
        assertEquals(6, packets.get(4).getDataLength());

        // 核对包含特殊字节和中文内容的两个数据域。
        assertArrayEquals(
                new byte[]{0x7E, 0x7D, 0x00, (byte) 0xFF},
                packets.get(2).getData());
        assertEquals(
                "测试",
                new String(packets.get(4).getData(), StandardCharsets.UTF_8));
    }

}
