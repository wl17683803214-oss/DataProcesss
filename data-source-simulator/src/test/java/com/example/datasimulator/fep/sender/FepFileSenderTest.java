package com.example.datasimulator.fep.sender;

import com.example.common.protocol.fep.FepPacketType;
import com.example.common.protocol.fep.FepProtocolTool;
import com.example.datasimulator.fep.transport.FepTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 文件交换模拟发送流程测试。 */
class FepFileSenderTest {

    /** 测试文件临时目录。 */
    @TempDir
    Path temporaryDirectory;

    /** 验证请求、分块数据和结束确认按照协议顺序完成。 */
    @Test
    void shouldSendRequestAndAllDataUnits() throws Exception {
        // 第一步：创建包含一个完整单元和一个短单元的测试文件。
        byte[] sourceData = new byte[5_000];
        for (int index = 0; index < sourceData.length; index++) {
            sourceData[index] = (byte) index;
        }
        Path filePath = temporaryDirectory.resolve("sample.bin");
        Files.write(filePath, sourceData);

        // 第二步：模拟接收方从第零单元接收并最终返回结束确认。
        int fileId = 0x1234;
        RecordingTransport transport = new RecordingTransport(
                FepProtocolTool.buildRequestResponse(
                        "sample.bin", 0, fileId),
                FepProtocolTool.buildFinishConfirmation(fileId));
        FepFileSender sender = new FepFileSender(transport, 4_096, 0);

        // 第三步：执行完整文件发送流程。
        sender.send(filePath);

        // 第四步：核对发送顺序为请求包、完整数据单元和末尾短数据单元。
        assertEquals(3, transport.sentPackets.size());
        assertEquals(
                FepPacketType.SEND_REQUEST,
                FepProtocolTool.readPacketType(transport.sentPackets.get(0)));
        FepProtocolTool.DataPacket firstData =
                FepProtocolTool.parseDataPacket(transport.sentPackets.get(1));
        FepProtocolTool.DataPacket secondData =
                FepProtocolTool.parseDataPacket(transport.sentPackets.get(2));
        assertEquals(0, firstData.getUnitNumber());
        assertEquals(1, secondData.getUnitNumber());
        assertArrayEquals(
                Arrays.copyOfRange(sourceData, 0, 4_096),
                firstData.getData());
        assertArrayEquals(
                Arrays.copyOfRange(sourceData, 4_096, 5_000),
                secondData.getData());
    }

    /** 记录发送内容并按顺序返回预设应答的测试传输实现。 */
    private static final class RecordingTransport implements FepTransport {

        /** 模拟接收端依次返回的应答。 */
        private final List<byte[]> responses;
        /** 发送方已经发出的全部协议包。 */
        private final List<byte[]> sentPackets = new ArrayList<byte[]>();
        /** 下一条待返回应答的位置。 */
        private int responseIndex;

        private RecordingTransport(byte[]... responses) {
            // 复制应答数组，避免测试外部修改正在使用的内容。
            this.responses = Arrays.asList(responses);
        }

        @Override
        public void send(byte[] packetData) {
            // 保存独立副本，保证后续断言不受发送方数组复用影响。
            sentPackets.add(Arrays.copyOf(packetData, packetData.length));
        }

        @Override
        public byte[] receive(FepPacketType expectedType) {
            // 依次返回请求应答和结束确认，并核对测试数据类型。
            byte[] response = responses.get(responseIndex++);
            assertEquals(expectedType, FepProtocolTool.readPacketType(response));
            return Arrays.copyOf(response, response.length);
        }

        @Override
        public void close() {
            // 内存测试传输没有需要释放的外部资源。
        }
    }
}
