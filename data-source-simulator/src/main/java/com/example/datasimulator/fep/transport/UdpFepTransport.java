package com.example.datasimulator.fep.transport;

import com.example.common.protocol.fep.FepPacketType;
import com.example.common.protocol.fep.FepProtocolTool;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/** 基于数据报的文件交换传输实现。 */
public final class UdpFepTransport implements FepTransport {

    /** 单个数据报允许的最大字节数。 */
    private static final int MAX_DATAGRAM_LENGTH = 65_507;
    /** 数据报套接字。 */
    private final DatagramSocket socket;
    /** 接收端地址。 */
    private final InetAddress targetAddress;
    /** 接收端端口。 */
    private final int targetPort;

    /** 创建数据报套接字并设置应答超时时间。 */
    public UdpFepTransport(
            String targetHost,
            int targetPort,
            int responseTimeoutMillis) throws IOException {
        // 第一步：校验连接配置，避免无效目标和无限等待。
        if (targetHost == null || targetHost.trim().isEmpty()) {
            throw new IllegalArgumentException("接收端地址不能为空");
        }
        if (targetPort < 1 || targetPort > 65_535) {
            throw new IllegalArgumentException("接收端端口必须在1到65535之间");
        }
        if (responseTimeoutMillis <= 0) {
            throw new IllegalArgumentException("应答超时时间必须大于0");
        }

        // 第二步：创建套接字并保存目标网络信息。
        targetAddress = InetAddress.getByName(targetHost);
        this.targetPort = targetPort;
        socket = new DatagramSocket();
        socket.setSoTimeout(responseTimeoutMillis);
    }

    @Override
    public void send(byte[] packetData) throws IOException {
        // 第一步：空协议包不能发送，超长内容也不能放入单个数据报。
        if (packetData == null || packetData.length == 0) {
            throw new IllegalArgumentException("待发送协议包不能为空");
        }
        if (packetData.length > MAX_DATAGRAM_LENGTH) {
            throw new IllegalArgumentException("待发送协议包超过单个数据报长度限制");
        }

        // 第二步：把当前完整协议包作为一个独立数据报发送。
        DatagramPacket packet = new DatagramPacket(
                packetData,
                packetData.length,
                targetAddress,
                targetPort);
        socket.send(packet);
    }

    @Override
    public byte[] receive(FepPacketType expectedType) throws IOException {
        // 第一步：使用最大缓存接收完整数据报，避免无法发现意外的超长应答。
        byte[] buffer = new byte[MAX_DATAGRAM_LENGTH];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        byte[] packetData = Arrays.copyOfRange(
                packet.getData(),
                packet.getOffset(),
                packet.getOffset() + packet.getLength());

        // 第二步：同时核对来源、包长和包类型。
        if (!packet.getAddress().equals(targetAddress)
                || packet.getPort() != targetPort) {
            throw new IOException("应答数据报不是来自配置的接收端");
        }
        int expectedLength = FepTransport.fixedPacketLength(expectedType);
        if (packetData.length != expectedLength) {
            throw new IOException(
                    "应答包长度不正确，期望：" + expectedLength
                            + "，实际：" + packetData.length);
        }
        FepPacketType actualType = FepProtocolTool.readPacketType(packetData);
        if (actualType != expectedType) {
            throw new IOException(
                    "应答包类型不正确，期望：" + expectedType.getName()
                            + "，实际：" + actualType.getName());
        }
        return packetData;
    }

    @Override
    public void close() {
        socket.close();
    }
}
