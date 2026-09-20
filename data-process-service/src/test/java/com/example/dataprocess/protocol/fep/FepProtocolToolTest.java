package com.example.dataprocess.protocol.fep;

import com.example.common.protocol.fep.FepProtocolTool;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** FEP协议编解码工具测试。 */
class FepProtocolToolTest {

    /** 验证发送请求使用固定文件名字段和小端文件长度。 */
    @Test
    void shouldBuildAndParseSendRequest() {
        // 构造包含中文文件名的发送请求包。
        byte[] packetData = FepProtocolTool.buildSendRequest(
                "遥测数据.dat", 0x01020304);

        // 核对固定包长、类型以及小端文件长度字节。
        assertEquals(69, packetData.length);
        assertEquals(0x01, packetData[0] & 0xFF);
        assertArrayEquals(
                new byte[]{0x04, 0x03, 0x02, 0x01},
                new byte[]{packetData[65], packetData[66],
                        packetData[67], packetData[68]});

        // 解析请求并核对UTF-8文件名和文件长度可以完整恢复。
        FepProtocolTool.SendRequest request =
                FepProtocolTool.parseSendRequest(packetData);
        assertEquals("遥测数据.dat", request.getFileName());
        assertEquals(0x01020304, request.getFileLength());
    }

    /** 验证数据包字段顺序为Num、ID、Data且全部整数采用小端。 */
    @Test
    void shouldBuildAndParseDataPacket() {
        // 构造第258个数据单元、文件标识4660和三个数据字节。
        byte[] sourceData = "ABC".getBytes(StandardCharsets.UTF_8);
        byte[] packetData = FepProtocolTool.buildDataPacket(
                258, 0x1234, sourceData);

        // 核对类型、Num小端字节、ID小端字节及数据内容位置。
        assertArrayEquals(
                new byte[]{0x04, 0x02, 0x01, 0x00, 0x00,
                        0x34, 0x12, 0x41, 0x42, 0x43},
                packetData);

        // 解析数据包并核对所有字段。
        FepProtocolTool.DataPacket dataPacket =
                FepProtocolTool.parseDataPacket(packetData);
        assertEquals(258, dataPacket.getUnitNumber());
        assertEquals(0x1234, dataPacket.getFileId());
        assertArrayEquals(sourceData, dataPacket.getData());
    }
}
