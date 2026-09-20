package com.example.datasimulator.fep.transport;

import com.example.common.protocol.fep.FepPacketType;
import com.example.common.protocol.fep.FepProtocolTool;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** 基于可靠字节流的文件交换传输实现。 */
public final class TcpFepTransport implements FepTransport {

    /** 当前传输连接。 */
    private final Socket socket;
    /** 连接输入流。 */
    private final InputStream inputStream;
    /** 连接输出流。 */
    private final OutputStream outputStream;

    /** 创建连接并设置连接及应答超时时间。 */
    public TcpFepTransport(
            String targetHost,
            int targetPort,
            int connectTimeoutMillis,
            int responseTimeoutMillis) throws IOException {
        // 第一步：校验目标端口和超时时间，避免无效连接参数。
        validateNetworkConfig(
                targetHost,
                targetPort,
                connectTimeoutMillis,
                responseTimeoutMillis);

        // 第二步：建立连接并取得后续复用的输入输出流。
        socket = new Socket();
        socket.connect(
                new InetSocketAddress(targetHost, targetPort),
                connectTimeoutMillis);
        socket.setSoTimeout(responseTimeoutMillis);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    @Override
    public void send(byte[] packetData) throws IOException {
        // 第一步：空协议包不能发送。
        validatePacketData(packetData);

        // 第二步：写入完整协议包并立即刷新到网络连接。
        outputStream.write(packetData);
        outputStream.flush();
    }

    @Override
    public byte[] receive(FepPacketType expectedType) throws IOException {
        // 第一步：根据应答类型确定本次必须读取的固定字节数。
        int expectedLength = FepTransport.fixedPacketLength(expectedType);
        byte[] packetData = new byte[expectedLength];

        // 第二步：循环读取直到取得完整应答，不能假设一次读取就是一个完整包。
        int totalRead = 0;
        while (totalRead < expectedLength) {
            int readCount = inputStream.read(
                    packetData, totalRead, expectedLength - totalRead);
            if (readCount == -1) {
                throw new EOFException("接收完整应答前连接已经关闭");
            }
            totalRead += readCount;
        }

        // 第三步：核对应答类型，防止连接中的其他数据被错误解析。
        validatePacketType(packetData, expectedType);
        return packetData;
    }

    @Override
    public void close() throws IOException {
        // 关闭连接会同时释放关联的输入输出流。
        socket.close();
    }

    /** 校验连接配置。 */
    private void validateNetworkConfig(
            String targetHost,
            int targetPort,
            int connectTimeoutMillis,
            int responseTimeoutMillis) {
        // 地址不能为空，端口和超时时间也必须处于有效范围。
        if (targetHost == null || targetHost.trim().isEmpty()) {
            throw new IllegalArgumentException("接收端地址不能为空");
        }
        if (targetPort < 1 || targetPort > 65_535) {
            throw new IllegalArgumentException("接收端端口必须在1到65535之间");
        }
        if (connectTimeoutMillis <= 0 || responseTimeoutMillis <= 0) {
            throw new IllegalArgumentException("连接和应答超时时间必须大于0");
        }
    }

    /** 校验待发送协议包。 */
    private void validatePacketData(byte[] packetData) {
        // 空数组不构成有效协议包。
        if (packetData == null || packetData.length == 0) {
            throw new IllegalArgumentException("待发送协议包不能为空");
        }
    }

    /** 校验接收协议包类型。 */
    private void validatePacketType(
            byte[] packetData,
            FepPacketType expectedType) throws IOException {
        // 实际类型必须和当前发送流程等待的类型一致。
        FepPacketType actualType = FepProtocolTool.readPacketType(packetData);
        if (actualType != expectedType) {
            throw new IOException(
                    "应答包类型不正确，期望：" + expectedType.getName()
                            + "，实际：" + actualType.getName());
        }
    }
}
