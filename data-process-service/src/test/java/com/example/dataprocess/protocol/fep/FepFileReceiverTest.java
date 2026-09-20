package com.example.dataprocess.protocol.fep;

import com.example.common.protocol.fep.FepProtocolTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FEP文件接收工具测试。 */
class FepFileReceiverTest {

    /** 单元测试使用的临时接收目录。 */
    @TempDir
    Path temporaryDirectory;

    /** 验证非整倍数文件通过最后一个短数据单元完成接收。 */
    @Test
    void shouldReceiveFileWithShortLastUnit() throws Exception {
        // 准备5000字节源文件内容和文件接收工具。
        byte[] sourceData = createSequentialData(5_000);
        FepFileReceiver receiver = new FepFileReceiver(temporaryDirectory);

        // 发送01请求并取得接收方分配的文件标识和起始单元号。
        FepFileReceiver.RequestAcceptance acceptance = receiver.acceptRequest(
                FepProtocolTool.buildSendRequest("sample.bin", sourceData.length));
        assertEquals(0, acceptance.getResumeUnitNumber());

        // 发送第0个完整4096字节数据单元，此时文件尚未结束。
        FepFileReceiver.DataReceiveResult firstResult = receiver.receiveData(
                FepProtocolTool.buildDataPacket(
                        0,
                        acceptance.getFileId(),
                        Arrays.copyOfRange(sourceData, 0, 4_096)));
        assertFalse(firstResult.isCompleted());

        // 发送第1个904字节短数据单元，接收方应完成文件并生成03确认包。
        FepFileReceiver.DataReceiveResult finalResult = receiver.receiveData(
                FepProtocolTool.buildDataPacket(
                        1,
                        acceptance.getFileId(),
                        Arrays.copyOfRange(sourceData, 4_096, sourceData.length)));
        assertTrue(finalResult.isCompleted());
        assertEquals(0x03, finalResult.getConfirmationPacket()[0] & 0xFF);
        assertArrayEquals(
                sourceData,
                Files.readAllBytes(temporaryDirectory.resolve("sample.bin")));
    }

    /** 验证整倍数文件必须通过额外空数据单元结束。 */
    @Test
    void shouldRequireEmptyPacketForExactUnitFile() throws Exception {
        // 准备一个恰好4096字节的文件内容。
        byte[] sourceData = createSequentialData(4_096);
        FepFileReceiver receiver = new FepFileReceiver(temporaryDirectory);
        FepFileReceiver.RequestAcceptance acceptance = receiver.acceptRequest(
                FepProtocolTool.buildSendRequest("exact.bin", sourceData.length));

        // 发送完整数据单元后，按照协议不能立即判定文件结束。
        FepFileReceiver.DataReceiveResult firstResult = receiver.receiveData(
                FepProtocolTool.buildDataPacket(
                        0, acceptance.getFileId(), sourceData));
        assertFalse(firstResult.isCompleted());

        // 补发第1个零长度数据单元后完成接收。
        FepFileReceiver.DataReceiveResult finalResult = receiver.receiveData(
                FepProtocolTool.buildDataPacket(
                        1, acceptance.getFileId(), new byte[0]));
        assertTrue(finalResult.isCompleted());
        assertArrayEquals(
                sourceData,
                Files.readAllBytes(temporaryDirectory.resolve("exact.bin")));
    }

    /** 创建内容可重复核对的测试数据。 */
    private byte[] createSequentialData(int length) {
        // 每个字节使用自身序号低八位，便于发现写入偏移错误。
        byte[] data = new byte[length];
        for (int index = 0; index < data.length; index++) {
            data[index] = (byte) index;
        }
        return data;
    }
}
