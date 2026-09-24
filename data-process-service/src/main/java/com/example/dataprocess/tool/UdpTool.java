package com.example.dataprocess.tool;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/** UDP 通用持续收发工具，支持单播和组播。 */
public final class UdpTool {

    /** UDP 报文允许接收的最大数据长度。 */
    private static final int MAX_DATAGRAM_LENGTH = 65_507;

    /** 工具类不允许实例化。 */
    private UdpTool() {
    }

    /**
     * 创建可以连续发送和接收单播报文的 UDP 端点。
     *
     * @param localAddress 本机绑定地址，例如192.168.1.10；为空时绑定全部本机网卡
     * @param localPort 本机绑定端口，取值范围为1到65535
     * @param timeoutMillis 接收超时时间，单位毫秒，0表示一直等待
     * @return 可重复调用发送和接收方法的 UDP 单播端点
     * @throws IOException 本机地址解析或端口绑定失败
     */
    public static UnicastEndpoint openUnicast(
            String localAddress,
            int localPort,
            int timeoutMillis) throws IOException {
        // 校验本机端口和接收超时时间。
        validatePort(localPort, "UDP 本机端口");
        validateTimeout(timeoutMillis);

        // 根据参数确定绑定全部网卡或指定本机地址。
        InetSocketAddress bindAddress = resolveLocalAddress(localAddress, localPort);

        // 创建并配置可以长期复用的单播套接字。
        DatagramSocket socket = new DatagramSocket(null);
        try {
            socket.setReuseAddress(true);
            socket.bind(bindAddress);
            socket.setSoTimeout(timeoutMillis);

            // 返回持有同一套接字的单播端点，供调用方连续收发。
            return new UnicastEndpoint(socket);
        } catch (IOException exception) {
            // 初始化失败时关闭套接字，避免占用端口资源。
            socket.close();
            throw exception;
        }
    }

    /**
     * 创建只负责连续发送组播报文的发送端。
     *
     * @param localAddress 本机发送网卡地址，例如192.168.1.10；为空时使用默认网卡
     * @return 不加入任何组播、只负责发送的组播发送端
     * @throws IOException 本机网卡解析失败
     */
    public static MulticastSender openMulticastSender(String localAddress) throws IOException {
        // 根据本机地址选择用于发送组播的网络接口。
        NetworkInterface networkInterface = resolveNetworkInterface(localAddress);

        // 创建不绑定组播接收端口、也不加入组播的发送套接字。
        MulticastSocket socket = new MulticastSocket();
        try {
            // 指定网卡时明确设置组播数据的出口网卡。
            if (networkInterface != null) {
                socket.setNetworkInterface(networkInterface);
            }
            return new MulticastSender(socket);
        } catch (IOException exception) {
            // 初始化失败时关闭套接字，避免资源泄漏。
            socket.close();
            throw exception;
        }
    }

    /**
     * 创建负责加入组播并连续接收报文的接收端。
     *
     * @param multicastAddress 需要加入的组播地址，例如239.1.1.1
     * @param port 组播接收端口，取值范围为1到65535
     * @param localAddress 本机接收网卡地址，例如192.168.1.10；为空时使用默认网卡
     * @param timeoutMillis 接收超时时间，单位毫秒，0表示一直等待
     * @return 已加入指定组播、只负责接收的组播接收端
     * @throws IOException 端口绑定、网卡解析或加入组播失败
     */
    public static MulticastReceiver openMulticastReceiver(
            String multicastAddress,
            int port,
            String localAddress,
            int timeoutMillis) throws IOException {
        // 校验组播地址、端口、网卡和接收超时时间。
        InetAddress groupAddress = resolveMulticastAddress(multicastAddress, port);
        NetworkInterface networkInterface = resolveNetworkInterface(localAddress);
        validateTimeout(timeoutMillis);

        // 创建并绑定负责接收组播数据的长期套接字。
        MulticastSocket socket = new MulticastSocket(null);
        try {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            socket.setSoTimeout(timeoutMillis);

            // 只有接收端执行加入组播操作。
            InetSocketAddress groupSocketAddress = new InetSocketAddress(groupAddress, port);
            if (networkInterface == null) {
                socket.joinGroup(groupAddress);
            } else {
                socket.joinGroup(groupSocketAddress, networkInterface);
            }

            // 返回只负责接收和维护组播成员关系的接收端。
            return new MulticastReceiver(
                    socket,
                    groupAddress,
                    groupSocketAddress,
                    networkInterface);
        } catch (IOException exception) {
            // 初始化失败时关闭套接字，避免占用端口资源。
            socket.close();
            throw exception;
        }
    }

    /** 从同一个 UDP 套接字接收一个完整报文。 */
    private static byte[] receive(DatagramSocket socket) throws IOException {
        // 普通调用只关心报文内容，复用带来源信息的统一接收入口。
        return receivePacket(socket).getData();
    }

    /** 从同一个UDP套接字接收报文及其来源地址。 */
    private static ReceivedPacket receivePacket(DatagramSocket socket) throws IOException {
        // 每次接收都创建独立缓冲区，避免后续报文覆盖前一次结果。
        byte[] buffer = new byte[MAX_DATAGRAM_LENGTH];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // UDP 保留报文边界，一次调用只取得一个完整报文。
        socket.receive(packet);

        // 复制实际收到的范围，并保留来源地址供FEP等应答协议使用。
        byte[] receivedData = Arrays.copyOfRange(
                packet.getData(),
                packet.getOffset(),
                packet.getOffset() + packet.getLength());
        return new ReceivedPacket(
                receivedData,
                packet.getAddress().getHostAddress(),
                packet.getPort());
    }

    /** 使用同一个 UDP 套接字发送一个完整报文。 */
    private static void send(
            DatagramSocket socket,
            String targetAddress,
            int targetPort,
            byte[] data) throws IOException {
        // 校验目标地址、目标端口和待发送数据。
        validateAddress(targetAddress, "UDP 目标地址");
        validatePort(targetPort, "UDP 目标端口");
        validateData(data);

        // 将全部字节作为一个独立 UDP 报文发送。
        InetAddress address = InetAddress.getByName(targetAddress.trim());
        DatagramPacket packet = new DatagramPacket(data, data.length, address, targetPort);
        socket.send(packet);
    }

    /** 解析本机绑定地址。 */
    private static InetSocketAddress resolveLocalAddress(
            String localAddress,
            int localPort) throws IOException {
        // 空地址表示监听全部本机网卡。
        if (localAddress == null || localAddress.trim().isEmpty()) {
            return new InetSocketAddress(localPort);
        }

        // 指定地址必须属于当前计算机的某个网络接口。
        InetAddress address = InetAddress.getByName(localAddress.trim());
        if (NetworkInterface.getByInetAddress(address) == null) {
            throw new IOException("UDP 绑定地址不是本机地址：" + localAddress);
        }
        return new InetSocketAddress(address, localPort);
    }

    /** 解析并校验组播地址。 */
    private static InetAddress resolveMulticastAddress(
            String multicastAddress,
            int port) throws IOException {
        // 组播地址不能为空，端口必须有效。
        validateAddress(multicastAddress, "UDP 组播地址");
        validatePort(port, "UDP 组播端口");

        // 地址必须属于 IP 组播地址范围。
        InetAddress groupAddress = InetAddress.getByName(multicastAddress.trim());
        if (!groupAddress.isMulticastAddress()) {
            throw new IllegalArgumentException("UDP 组播地址不合法：" + multicastAddress);
        }
        return groupAddress;
    }

    /** 根据本机地址查找对应网络接口。 */
    private static NetworkInterface resolveNetworkInterface(String localAddress) throws IOException {
        // 空地址表示使用操作系统选择的默认网卡。
        if (localAddress == null || localAddress.trim().isEmpty()) {
            return null;
        }

        // 指定地址必须属于本机已经配置的网络接口。
        InetAddress address = InetAddress.getByName(localAddress.trim());
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
        if (networkInterface == null) {
            throw new IOException("未找到本机网卡地址：" + localAddress);
        }
        return networkInterface;
    }

    /** 校验网络地址。 */
    private static void validateAddress(String address, String name) {
        // 网络地址不能为空或只包含空白字符。
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }

    /** 校验网络端口。 */
    private static void validatePort(int port, String name) {
        // 端口必须处于网络协议允许的有效范围。
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException(name + "必须在1到65535之间");
        }
    }

    /** 校验接收超时时间。 */
    private static void validateTimeout(int timeoutMillis) {
        // 零表示一直等待，负数没有合法含义。
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("UDP 接收超时时间不能小于0");
        }
    }

    /** 校验待发送数据。 */
    private static void validateData(byte[] data) {
        // UDP 报文不能为空且不能超过协议允许的最大长度。
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("UDP 发送数据不能为空");
        }
        if (data.length > MAX_DATAGRAM_LENGTH) {
            throw new IllegalArgumentException("UDP 发送数据不能超过65507字节");
        }
    }

    /** UDP接收任务统一使用的收发端点。 */
    public interface DatagramEndpoint extends AutoCloseable {

        /** 使用当前套接字向指定来源或目标发送一个UDP报文。 */
        void send(String targetAddress, int targetPort, byte[] data)
                throws IOException;

        /** 接收下一条UDP报文并保留发送方地址和端口。 */
        ReceivedPacket receivePacket() throws IOException;

        /** @return 当前UDP端点是否仍可使用 */
        boolean isOpen();

        /** 关闭当前UDP端点并释放监听资源。 */
        @Override
        void close() throws IOException;
    }

    /** 可以连续发送和接收单播报文的 UDP 端点。 */
    public static final class UnicastEndpoint implements DatagramEndpoint {

        /** 当前单播端点长期复用的套接字。 */
        private final DatagramSocket socket;

        /** 保存已经完成绑定的单播套接字。 */
        private UnicastEndpoint(DatagramSocket socket) {
            this.socket = socket;
        }

        /**
         * 使用当前端点发送一个 UDP 单播报文。
         *
         * @param targetAddress 目标主机地址，例如192.168.1.20
         * @param targetPort 目标端口，取值范围为1到65535
         * @param data 本次发送的原始字节，不能为空且不能超过65507字节
         * @throws IOException 地址解析或网络发送失败
         */
        @Override
        public void send(String targetAddress, int targetPort, byte[] data) throws IOException {
            // 复用当前端点的同一个套接字发送单播报文。
            UdpTool.send(socket, targetAddress, targetPort, data);
        }

        /**
         * 使用当前端点接收一个 UDP 单播报文。
         *
         * @return 一个具有完整边界的 UDP 原始报文
         * @throws IOException 网络接收失败或等待超时
         */
        public byte[] receive() throws IOException {
            // 复用当前端点的同一个套接字接收下一条报文。
            return UdpTool.receive(socket);
        }

        /** 接收下一条UDP报文，并返回发送方地址和端口。 */
        @Override
        public ReceivedPacket receivePacket() throws IOException {
            // FEP应答必须发回当前报文来源，因此保留来源网络信息。
            return UdpTool.receivePacket(socket);
        }

        /** @return 当前单播端点是否仍可使用 */
        @Override
        public boolean isOpen() {
            // 套接字未关闭时可以继续发送和接收。
            return !socket.isClosed();
        }

        /** 关闭当前单播端点并释放绑定端口。 */
        @Override
        public void close() {
            // 关闭套接字，结束后续发送和接收。
            socket.close();
        }
    }

    /** 一条包含来源网络信息的UDP报文。 */
    public static final class ReceivedPacket {

        /** 报文内容副本。 */
        private final byte[] data;
        /** 发送方地址。 */
        private final String sourceAddress;
        /** 发送方端口。 */
        private final int sourcePort;

        private ReceivedPacket(
                byte[] data,
                String sourceAddress,
                int sourcePort) {
            this.data = Arrays.copyOf(data, data.length);
            this.sourceAddress = sourceAddress;
            this.sourcePort = sourcePort;
        }

        public byte[] getData() {
            return Arrays.copyOf(data, data.length);
        }

        public String getSourceAddress() {
            return sourceAddress;
        }

        public int getSourcePort() {
            return sourcePort;
        }
    }

    /** 不加入组播、只负责连续发送组播报文的发送端。 */
    public static final class MulticastSender implements AutoCloseable {

        /** 当前组播发送端长期复用的套接字。 */
        private final MulticastSocket socket;

        /** 保存已经选择发送网卡的组播套接字。 */
        private MulticastSender(MulticastSocket socket) {
            this.socket = socket;
        }

        /**
         * 向指定组播地址和端口发送一个报文。
         *
         * @param multicastAddress 目标组播地址，例如239.1.1.1
         * @param port 目标组播端口，取值范围为1到65535
         * @param data 本次发送的原始字节，不能为空且不能超过65507字节
         * @throws IOException 网络发送失败
         */
        public void send(String multicastAddress, int port, byte[] data) throws IOException {
            // 校验目标确实属于组播地址范围。
            InetAddress groupAddress = resolveMulticastAddress(multicastAddress, port);

            // 复用当前发送套接字发送数据，但不加入目标组播。
            UdpTool.send(socket, groupAddress.getHostAddress(), port, data);
        }

        /** @return 当前组播发送端是否仍可使用 */
        public boolean isOpen() {
            // 套接字未关闭时可以继续发送组播报文。
            return !socket.isClosed();
        }

        /** 关闭当前组播发送端。 */
        @Override
        public void close() {
            // 发送端没有加入组播，直接关闭套接字即可。
            socket.close();
        }
    }

    /** 加入组播并负责连续接收报文的接收端。 */
    public static final class MulticastReceiver implements DatagramEndpoint {

        /** 当前组播接收端长期复用的套接字。 */
        private final MulticastSocket socket;

        /** 当前接收端加入的组播地址。 */
        private final InetAddress groupAddress;

        /** 当前加入组播时使用的套接字地址。 */
        private final InetSocketAddress groupSocketAddress;

        /** 当前加入组播时使用的本机网卡。 */
        private final NetworkInterface networkInterface;

        /** 保存已加入组播的套接字及退出组播所需信息。 */
        private MulticastReceiver(
                MulticastSocket socket,
                InetAddress groupAddress,
                InetSocketAddress groupSocketAddress,
                NetworkInterface networkInterface) {
            this.socket = socket;
            this.groupAddress = groupAddress;
            this.groupSocketAddress = groupSocketAddress;
            this.networkInterface = networkInterface;
        }

        /** 接收下一条具有完整边界的 UDP 组播报文。 */
        public byte[] receive() throws IOException {
            // 复用已经加入组播的套接字接收下一条报文。
            return UdpTool.receive(socket);
        }

        /** 接收下一条组播报文，并保留实际发送方地址和端口。 */
        @Override
        public ReceivedPacket receivePacket() throws IOException {
            // FEP等需要应答的协议使用来源信息把响应发送给实际发送方。
            return UdpTool.receivePacket(socket);
        }

        /** 使用当前组播套接字向指定目标发送一个UDP应答报文。 */
        @Override
        public void send(
                String targetAddress,
                int targetPort,
                byte[] data) throws IOException {
            // 组播接收后的协议应答仍发送到实际报文来源地址。
            UdpTool.send(socket, targetAddress, targetPort, data);
        }

        /** @return 当前组播接收端是否仍可使用 */
        @Override
        public boolean isOpen() {
            // 套接字未关闭时可以继续接收组播报文。
            return !socket.isClosed();
        }

        /** 退出组播并关闭当前组播接收端。 */
        @Override
        public void close() throws IOException {
            try {
                // 使用与加入时相同的网卡和地址退出组播。
                if (networkInterface == null) {
                    socket.leaveGroup(groupAddress);
                } else {
                    socket.leaveGroup(groupSocketAddress, networkInterface);
                }
            } finally {
                // 无论退出是否成功，都关闭套接字释放接收端口。
                socket.close();
            }
        }
    }
//
//    /**
//     * UDP 单播运行入口。
//     *
//     * <p>运行单播时保留此方法，并保持下方组播 main 方法处于注释状态。</p>
//     */
//    public static void main(String[] args) throws IOException {
//        // Java程序绑定本机回环地址，与本机网络调试工具通信。
//        String localAddress = "127.0.0.1";
//
//        // Java程序使用的本机端口，不能与网络调试工具端口相同。
//        int localPort = 9001;
//
//        // 网络调试工具绑定的本机地址。
//        String targetAddress = "127.0.0.1";
//
//        // 网络调试工具绑定的UDP端口。
//        int targetPort = 9002;
//
//        // 创建一次单播端点，并通过控制台连续输入需要发送的内容。
//        try (Scanner scanner = new Scanner(System.in, "UTF-8");
//             UnicastEndpoint endpoint = openUnicast(localAddress, localPort, 0)) {
//            // 输出本机和目标地址信息，便于核对网络调试工具配置。
//            System.out.println("UDP单播已启动，本机端口：" + localPort
//                    + "，目标端口：" + targetPort);
//            System.out.println("请输入发送内容；输入“退出”结束程序");
//
//            // 创建独立接收线程，持续接收网络调试工具发到本机端口的数据。
//            Thread receiveThread = new Thread(() -> {
//                // 当前单播端点可用时持续接收，不影响主线程发送数据。
//                while (endpoint.isOpen()) {
//                    try {
//                        // 接收下一条具有完整边界的UDP单播报文。
//                        byte[] receivedData = endpoint.receive();
//
//                        // 按UTF-8文本和十六进制两种形式输出收到的数据。
//                        System.out.println("收到单播数据："
//                                + new String(receivedData, StandardCharsets.UTF_8));
//                        System.out.println("收到单播字节：" + PdxpParser.toHex(receivedData));
//                    } catch (IOException exception) {
//                        // 端点关闭或接收失败时结束后台接收线程。
//                        if (endpoint.isOpen()) {
//                            System.out.println("UDP单播接收结束：" + exception.getMessage());
//                        }
//                        break;
//                    }
//                }
//            }, "UDP单播接收线程");
//
//            // 设置为后台线程，主程序退出时无需额外等待该线程。
//            receiveThread.setDaemon(true);
//            receiveThread.start();
//
//            // 同一个单播端点可以连续发送多条 UDP 报文。
//            while (endpoint.isOpen() && scanner.hasNextLine()) {
//                // 读取本次准备发送的一行文本。
//                String message = scanner.nextLine();
//
//                // 输入指定结束词时结束循环并关闭单播端点。
//                if ("退出".equals(message)) {
//                    break;
//                }
//
//                // 将文本转换为UTF-8字节，并发送给网络调试工具。
//                byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
//                endpoint.send(targetAddress, targetPort, sendData);
//
//                // 输出本次发送结果，不强制等待网络调试工具回复。
//                System.out.println("发送完成，字节数量：" + sendData.length);
//            }
//        }
//    }
//
//    /**
//     * UDP 组播接收运行入口。
//     *
//     * <p>此入口只加入组播并接收，不创建发送端，因此不会自发自收。</p>
//     */
//    public static void main(String[] args) throws IOException {
//        // Java需要加入并监听的组播地址。
//        String multicastAddress = "239.1.1.19";
//
//        // Java需要监听的组播端口，调试助手必须发送到该端口。
//        int port = 9003;
//
//        // Java通过本机WLAN网卡加入组播。
//        String localAddress = "10.206.90.38";
//
//        // 只创建组播接收端，不创建组播发送端。
//        try (MulticastReceiver receiver = openMulticastReceiver(
//                     multicastAddress,
//                     port,
//                     localAddress,
//                     0)) {
//            // 输出当前接收端加入的组播地址和端口。
//            System.out.println("UDP组播接收已启动，组播地址：" + multicastAddress
//                    + "，组播端口：" + port);
//
//            // 当前接收端保持开启时持续等待外部程序发送组播数据。
//            while (receiver.isOpen()) {
//                // 接收调试助手发送到当前组播的下一条完整报文。
//                byte[] receivedData = receiver.receive();
//
//                // 按UTF-8文本和十六进制两种形式输出收到的数据。
//                System.out.println("收到组播数据："
//                        + new String(receivedData, StandardCharsets.UTF_8));
//                System.out.println("收到组播字节：" + PdxpParser.toHex(receivedData));
//            }
//        }
//    }

    /*
     * UDP 组播发送运行入口。
     *
     * 使用说明：
     * 1. 运行发送端前，先注释上方组播接收 main 方法。
     * 2. 再取消下面组播发送 main 方法的注释。
     * 3. 该入口只发送，不调用joinGroup，也不会监听组播。
     *

     */
    public static void main(String[] args) throws IOException {
        // 需要发送到的目标组播地址。
        String multicastAddress = "239.1.1.19";

        // 需要发送到的目标组播端口。
        int port = 9003;

        // 本机发送组播时使用的WLAN网卡地址。
        String localAddress = "10.206.90.38";

        // 创建只负责发送的组播发送端，不加入任何组播。
        try (Scanner scanner = new Scanner(System.in, "UTF-8");
             MulticastSender sender = openMulticastSender(localAddress)) {
            // 提示控制台输入规则。
            System.out.println("UDP组播发送端已启动，请输入发送内容；输入“退出”结束程序");

            // 使用同一个发送端连续发送多条组播报文。
            while (sender.isOpen() && scanner.hasNextLine()) {
                // 读取本次准备发送的一行文本。
                String message = scanner.nextLine();
                if ("退出".equals(message)) {
                    break;
                }

                // 将文本转换为UTF-8字节并发送到目标组播。
                byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
                sender.send(multicastAddress, port, sendData);
                System.out.println("组播发送完成，字节数量：" + sendData.length);
            }
        }
    }


}
