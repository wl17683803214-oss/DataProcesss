package com.example.datasimulator.pdxp.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 模拟源报文字节布局测试。 */
class SimulatorPacketBuilderTest {

    /** 验证512字节输入帧被正确重排成551字节UDP报文。 */
    @Test
    void shouldBuildExpectedUdpPacketLayout() {
        // 第一步：创建每个位置都可识别的512字节输入帧。
        byte[] inputFrame = new byte[512];
        for (int index = 0; index < inputFrame.length; index++) {
            inputFrame[index] = (byte) index;
        }
        SimulatorPacketBuilder packetBuilder = new SimulatorPacketBuilder(
                new PdxpPacketBuilder());

        // 第二步：构造第一帧和第二帧，用于同时验证布局和序号递增。
        byte[] firstPacket = packetBuilder.build(inputFrame);
        byte[] secondPacket = packetBuilder.build(inputFrame);

        // 第三步：传输头1234应按小端发送为3412。
        assertEquals(551, firstPacket.length);
        assertArrayEquals(new byte[]{0x34, 0x12},
                java.util.Arrays.copyOfRange(firstPacket, 0, 2));

        // 第四步：PDXP版本字段和517字节长度应位于固定偏移。
        assertEquals((byte) 0x80, firstPacket[2]);
        assertArrayEquals(new byte[]{0x05, 0x02},
                java.util.Arrays.copyOfRange(firstPacket, 32, 34));

        // 第五步：自定义数据0102030405应整体按小端发送。
        assertArrayEquals(
                new byte[]{0x05, 0x04, 0x03, 0x02, 0x01},
                java.util.Arrays.copyOfRange(firstPacket, 34, 39));

        // 第六步：原输入偏移4到511的数据应保持顺序移动到自定义数据之后。
        assertArrayEquals(
                java.util.Arrays.copyOfRange(inputFrame, 4, 512),
                java.util.Arrays.copyOfRange(firstPacket, 39, 547));

        // 第七步：原输入前四字节应保持顺序移动到数据域末尾。
        assertArrayEquals(
                java.util.Arrays.copyOfRange(inputFrame, 0, 4),
                java.util.Arrays.copyOfRange(firstPacket, 547, 551));

        // 第八步：默认序号从零开始，第二帧按小端写入数值一。
        assertArrayEquals(new byte[]{0, 0, 0, 0},
                java.util.Arrays.copyOfRange(firstPacket, 17, 21));
        assertArrayEquals(new byte[]{1, 0, 0, 0},
                java.util.Arrays.copyOfRange(secondPacket, 17, 21));
    }

    /** 验证用户填写的十六进制字段按字段独立转换为小端。 */
    @Test
    void shouldDecodeConfiguredHexAsLittleEndian() {
        // 两字节配置1234转换后必须低位字节在前。
        assertArrayEquals(
                new byte[]{0x34, 0x12},
                HexValueCodec.decodeLittleEndian("1234", 2, "测试字段"));

        // 四字节配置允许带0x前缀和空格，转换规则保持一致。
        assertArrayEquals(
                new byte[]{0x78, 0x56, 0x34, 0x12},
                HexValueCodec.decodeLittleEndian(
                        "0x12 34 56 78", 4, "测试字段"));
    }
}
