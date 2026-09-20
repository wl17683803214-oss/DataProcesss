package com.example.datasimulator.pdxp.sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/** 向指定目标发送模拟UDP报文。 */
public final class UdpSimulatorSender implements AutoCloseable {

    /** UDP发送套接字。 */
    private final DatagramSocket socket;
    /** UDP目标地址。 */
    private final InetAddress targetAddress;
    /** UDP目标端口。 */
    private final int targetPort;

    /** 创建可复用的UDP发送套接字。 */
    public UdpSimulatorSender(String targetHost, int targetPort)
            throws IOException {
        // 第一步：端口必须处于UDP有效范围。
        if (targetPort <= 0 || targetPort > 65535) {
            throw new IllegalArgumentException("UDP目标端口必须在1到65535之间");
        }
        // 第二步：解析目标地址并创建发送套接字。
        this.targetAddress = InetAddress.getByName(targetHost);
        this.targetPort = targetPort;
        this.socket = new DatagramSocket();
    }

    /** 发送一个完整的模拟报文。 */
    public void send(byte[] packetData) throws IOException {
        // 第一步：空报文没有发送意义，直接拒绝。
        if (packetData == null || packetData.length == 0) {
            throw new IllegalArgumentException("UDP发送数据不能为空");
        }
        // 第二步：使用同一个套接字发送当前完整报文。
        DatagramPacket packet = new DatagramPacket(
                packetData,
                packetData.length,
                targetAddress,
                targetPort);
        socket.send(packet);
    }

    /** 关闭UDP发送套接字。 */
    @Override
    public void close() {
        socket.close();
    }
}
