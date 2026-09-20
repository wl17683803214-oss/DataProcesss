package com.example.datasimulator.fep.sender;

import com.example.common.protocol.fep.FepPacketType;
import com.example.common.protocol.fep.FepProtocolTool;
import com.example.datasimulator.fep.transport.FepTransport;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/** 按文件交换协议发送一个完整文件。 */
public final class FepFileSender {

    /** 实际网络传输实现。 */
    private final FepTransport transport;
    /** 每个文件数据单元的最大字节数。 */
    private final int dataUnitLength;
    /** 相邻数据单元的发送间隔。 */
    private final long sendIntervalMillis;

    public FepFileSender(
            FepTransport transport,
            int dataUnitLength,
            long sendIntervalMillis) {
        // 第一步：检查发送流程依赖和数据单元配置。
        if (transport == null) {
            throw new IllegalArgumentException("网络传输实现不能为空");
        }
        if (dataUnitLength < 1
                || dataUnitLength > FepProtocolTool.DEFAULT_DATA_UNIT_LENGTH) {
            throw new IllegalArgumentException("数据单元长度必须在1到4096之间");
        }
        if (sendIntervalMillis < 0) {
            throw new IllegalArgumentException("发送间隔不能小于0");
        }

        // 第二步：保存经过校验的固定发送参数。
        this.transport = transport;
        this.dataUnitLength = dataUnitLength;
        this.sendIntervalMillis = sendIntervalMillis;
    }

    /** 完成请求、续传、数据发送和结束确认流程。 */
    public void send(Path filePath) throws Exception {
        // 第一步：检查源文件并取得协议允许的文件名和文件长度。
        int fileLength = validateAndReadFileLength(filePath);
        String fileName = filePath.getFileName().toString();

        // 第二步：发送开始请求并等待接收方返回续传位置和文件标识。
        byte[] requestPacket = FepProtocolTool.buildSendRequest(
                fileName, fileLength);
        transport.send(requestPacket);
        System.out.println("第一步：已发送01发送请求包，文件名："
                + fileName + "，文件长度：" + fileLength
                + "，协议包：" + toHex(requestPacket));
        byte[] responsePacket = transport.receive(
                FepPacketType.REQUEST_RESPONSE);
        FepProtocolTool.RequestResponse response =
                FepProtocolTool.parseRequestResponse(
                        responsePacket);
        validateResponse(fileName, fileLength, response);
        System.out.println("第二步：已收到02请求应答包，文件名："
                + response.getFileName() + "，续传单元号："
                + response.getUnitNumber() + "，文件标识："
                + response.getFileId() + "，协议包："
                + toHex(responsePacket));

        // 第三步：接收方已有完整文件时无需再次传输数据。
        if (response.getUnitNumber()
                == FepProtocolTool.FILE_ALREADY_COMPLETE) {
            System.out.println("接收方已经存在完整文件，无需重复发送：" + fileName);
            return;
        }

        // 第四步：接收方不可用时停止传输并保留明确原因。
        if (response.getUnitNumber()
                == FepProtocolTool.RECEIVER_UNAVAILABLE) {
            throw new IllegalStateException("接收方当前不能接收该文件");
        }

        // 第五步：从接收方指定的数据单元开始继续发送文件内容。
        sendDataUnits(
                filePath,
                fileLength,
                response.getUnitNumber(),
                response.getFileId());

        // 第六步：等待结束确认并核对文件标识，确认本次传输完整结束。
        byte[] confirmationPacket = transport.receive(
                FepPacketType.FINISH_CONFIRMATION);
        FepProtocolTool.FinishConfirmation confirmation =
                FepProtocolTool.parseFinishConfirmation(confirmationPacket);
        if (confirmation.getFileId() != response.getFileId()) {
            throw new IllegalStateException(
                    "结束确认中的文件标识与当前文件不一致");
        }
        System.out.println("第四步：已收到03结束确认包，文件标识："
                + confirmation.getFileId() + "，协议包："
                + toHex(confirmationPacket));
        System.out.println("文件发送完成，文件名：" + fileName
                + "，文件长度：" + fileLength
                + "，文件标识：" + response.getFileId());
    }

    /** 从指定数据单元开始发送文件内容。 */
    private void sendDataUnits(
            Path filePath,
            int fileLength,
            int startUnitNumber,
            int fileId) throws Exception {
        // 第一步：根据数据单元号计算续传字节位置并检查边界。
        long startOffset = (long) startUnitNumber * dataUnitLength;
        if (startUnitNumber < 0 || startOffset > fileLength) {
            throw new IllegalStateException("接收方返回的续传位置超出文件范围");
        }

        // 第二步：定位到续传位置，按固定大小依次发送剩余文件内容。
        int unitNumber = startUnitNumber;
        try (RandomAccessFile sourceFile = new RandomAccessFile(
                filePath.toFile(), "r")) {
            sourceFile.seek(startOffset);
            long remainingLength = fileLength - startOffset;
            while (remainingLength > 0) {
                int currentLength = (int) Math.min(
                        remainingLength, dataUnitLength);
                byte[] unitData = new byte[currentLength];
                sourceFile.readFully(unitData);
                byte[] dataPacket = FepProtocolTool.buildDataPacket(
                        unitNumber, fileId, unitData);
                transport.send(dataPacket);
                System.out.println("第三步：已发送04文件数据包，单元号："
                        + unitNumber + "，文件标识：" + fileId
                        + "，数据长度：" + currentLength
                        + "，协议包：" + toHex(dataPacket));
                unitNumber++;
                remainingLength -= currentLength;
                waitBeforeNextUnit();
            }
        }

        // 第三步：文件长度为数据单元整数倍时，补发空单元作为结束标志。
        if (fileLength % dataUnitLength == 0) {
            byte[] dataPacket = FepProtocolTool.buildDataPacket(
                    unitNumber, fileId, new byte[0]);
            transport.send(dataPacket);
            System.out.println("第三步：已发送04文件结束空数据包，单元号："
                    + unitNumber + "，文件标识：" + fileId
                    + "，数据长度：0，协议包：" + toHex(dataPacket));
        }
    }

    /** 检查文件并读取协议使用的四字节长度。 */
    private int validateAndReadFileLength(Path filePath) throws Exception {
        // 第一步：文件必须存在并且是普通文件。
        if (filePath == null || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("待发送文件不存在：" + filePath);
        }

        // 第二步：当前协议长度使用有符号四字节，拒绝超出范围的文件。
        long fileLength = Files.size(filePath);
        if (fileLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("待发送文件长度超过协议当前支持范围");
        }
        return (int) fileLength;
    }

    /** 检查请求应答中的文件信息和续传位置。 */
    private void validateResponse(
            String expectedFileName,
            int fileLength,
            FepProtocolTool.RequestResponse response) {
        // 第一步：应答文件名必须对应本次发送请求。
        if (!expectedFileName.equals(response.getFileName())) {
            throw new IllegalStateException("请求应答中的文件名与当前文件不一致");
        }

        // 第二步：正常续传位置不能超过文件允许的数据单元范围。
        int unitNumber = response.getUnitNumber();
        if (unitNumber >= 0
                && (long) unitNumber * dataUnitLength > fileLength) {
            throw new IllegalStateException("请求应答中的续传位置超出文件范围");
        }
        if (unitNumber < FepProtocolTool.RECEIVER_UNAVAILABLE) {
            throw new IllegalStateException("请求应答包含未知的文件接收状态");
        }
    }

    /** 按配置等待下一数据单元。 */
    private void waitBeforeNextUnit() throws InterruptedException {
        // 配置为零时连续发送，不进行无意义等待。
        if (sendIntervalMillis > 0) {
            Thread.sleep(sendIntervalMillis);
        }
    }

    /** 将协议包转换为连续的大写十六进制文本。 */
    private String toHex(byte[] packetData) {
        // 第一步：按照一个字节两个字符预分配文本容量。
        StringBuilder builder = new StringBuilder(packetData.length * 2);

        // 第二步：逐字节转换并保留不足两位时的前导零。
        for (byte value : packetData) {
            builder.append(String.format("%02X", value & 0xFF));
        }
        return builder.toString();
    }
}
