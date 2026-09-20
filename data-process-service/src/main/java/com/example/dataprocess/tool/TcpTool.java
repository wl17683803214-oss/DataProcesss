package com.example.dataprocess.tool;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.List;

/** TCP 通用长连接收发工具。 */
public final class TcpTool {

    /** 默认单次读取缓冲区长度。 */
    private static final int BUFFER_LENGTH = 8_192;

    /** 工具类不允许实例化。 */
    private TcpTool() {
    }

    /**
     * 建立一个可以连续发送和接收数据的 TCP 长连接。
     *
     * @param host 远端服务地址，例如127.0.0.1或192.168.1.10
     * @param port 远端服务端口，取值范围为1到65535
     * @param connectTimeoutMillis 连接超时时间，单位毫秒，0表示使用系统默认值
     * @param readTimeoutMillis 接收超时时间，单位毫秒，0表示一直等待
     * @return 可重复调用发送和接收方法的 TCP 长连接
     * @throws IOException 建立连接失败
     */
    public static TcpConnection connect(
            String host,
            int port,
            int connectTimeoutMillis,
            int readTimeoutMillis) throws IOException {
        // 在建立连接前统一校验所有网络参数。
        validateConnectionArguments(host, port, connectTimeoutMillis, readTimeoutMillis);

        // 创建套接字并连接指定的远端服务。
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host.trim(), port), connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);

            // 返回持有同一套接字的长连接对象，供调用方连续收发。
            return new TcpConnection(socket);
        } catch (IOException exception) {
            // 连接失败时关闭已创建的套接字，避免资源泄漏。
            socket.close();
            throw exception;
        }
    }

    /**
     * 创建一个可以持续接受客户端连接的 TCP 服务端。
     *
     * @param localAddress 本机监听地址，例如127.0.0.1或0.0.0.0；为空时监听全部网卡
     * @param port 本机监听端口，取值范围为1到65535
     * @param acceptTimeoutMillis 等待客户端连接的超时时间，单位毫秒，0表示一直等待
     * @param clientReadTimeoutMillis 客户端连接接收超时时间，单位毫秒，0表示一直等待
     * @return 可以重复接受客户端连接的 TCP 服务端
     * @throws IOException 本机地址解析或端口绑定失败
     */
    public static TcpServer listen(
            String localAddress,
            int port,
            int acceptTimeoutMillis,
            int clientReadTimeoutMillis) throws IOException {
        // 校验监听端口和两个超时时间。
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("TCP 服务端端口必须在1到65535之间");
        }
        if (acceptTimeoutMillis < 0 || clientReadTimeoutMillis < 0) {
            throw new IllegalArgumentException("TCP 服务端超时时间不能小于0");
        }

        // 根据参数确定监听全部网卡或指定本机地址。
        InetSocketAddress bindAddress;
        if (localAddress == null || localAddress.trim().isEmpty()) {
            bindAddress = new InetSocketAddress(port);
        } else {
            bindAddress = new InetSocketAddress(localAddress.trim(), port);
        }

        // 创建服务端套接字并绑定监听地址。
        ServerSocket serverSocket = new ServerSocket();
        try {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(bindAddress);
            serverSocket.setSoTimeout(acceptTimeoutMillis);

            // 保存客户端读取超时，供后续每个新连接统一使用。
            return new TcpServer(serverSocket, clientReadTimeoutMillis);
        } catch (IOException exception) {
            // 初始化失败时关闭套接字，避免占用监听端口。
            serverSocket.close();
            throw exception;
        }
    }

    /** 从输入流完整读取指定长度的原始字节。 */
    private static byte[] readFully(InputStream inputStream, int expectedLength) throws IOException {
        // TCP 没有消息边界，调用方必须明确本次需要读取的字节数量。
        if (expectedLength <= 0) {
            throw new IllegalArgumentException("TCP 读取长度必须大于0");
        }

        // 根据期望长度准备输出空间和分批读取缓冲区。
        ByteArrayOutputStream output = new ByteArrayOutputStream(expectedLength);
        byte[] buffer = new byte[Math.min(BUFFER_LENGTH, expectedLength)];

        // 单次读取不保证填满缓冲区，因此循环读取到指定长度。
        while (output.size() < expectedLength) {
            int remaining = expectedLength - output.size();
            int readCount = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (readCount == -1) {
                throw new EOFException("TCP 连接提前结束，数据长度不足");
            }
            output.write(buffer, 0, readCount);
        }
        return output.toByteArray();
    }

    /** 校验 TCP 连接参数。 */
    private static void validateConnectionArguments(
            String host,
            int port,
            int connectTimeoutMillis,
            int readTimeoutMillis) {
        // 远端主机地址不能为空或只包含空白字符。
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("TCP 远端主机地址不能为空");
        }

        // TCP 端口必须处于网络协议允许的有效范围。
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("TCP 端口必须在1到65535之间");
        }

        // 零表示使用默认值或一直等待，负数没有合法含义。
        if (connectTimeoutMillis < 0 || readTimeoutMillis < 0) {
            throw new IllegalArgumentException("TCP 超时时间不能小于0");
        }
    }

    /** 校验待发送数据。 */
    private static void validateData(byte[] data) {
        // TCP 发送内容必须包含至少一个字节。
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("TCP 发送数据不能为空");
        }
    }

    /** 可以连续发送和接收数据的 TCP 长连接。 */
    public static final class TcpConnection implements AutoCloseable {

        /** 当前长连接使用的套接字。 */
        private final Socket socket;

        /** 当前长连接使用的输入流。 */
        private final InputStream inputStream;

        /** 当前长连接使用的输出流。 */
        private final OutputStream outputStream;

        /** 发送操作使用的独立锁，避免多线程发送内容交叉。 */
        private final Object sendLock = new Object();

        /** 接收操作使用的独立锁，保证同一时间只有一个线程读取。 */
        private final Object receiveLock = new Object();

        /** 保存已经建立连接的套接字及其输入输出流。 */
        private TcpConnection(Socket socket) throws IOException {
            // 保存套接字并取得可以重复使用的输入输出流。
            this.socket = socket;
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
        }

        /**
         * 在当前 TCP 长连接上发送一条原始数据。
         *
         * @param data 本次需要发送的原始字节，不能为空
         * @throws IOException 网络发送失败
         */
        public void send(byte[] data) throws IOException {
            // 每次发送前校验数据，防止发送无意义的空内容。
            validateData(data);

            // 发送和接收使用不同锁，允许TCP全双工同时进行。
            synchronized (sendLock) {
                // 向同一连接写入全部数据并刷新输出缓冲区。
                outputStream.write(data);
                outputStream.flush();
            }
        }

        /**
         * 从当前 TCP 长连接完整接收指定长度的数据。
         *
         * @param expectedLength 本次需要读取的字节数量，必须大于0
         * @return 本次完整读取的原始字节
         * @throws IOException 网络接收失败或连接提前关闭
         */
        public byte[] receive(int expectedLength) throws IOException {
            // 接收和发送使用不同锁，接收阻塞时不会影响发送操作。
            synchronized (receiveLock) {
                // 复用当前连接的输入流完整读取指定数量的字节。
                return readFully(inputStream, expectedLength);
            }
        }

        /**
         * 从当前 TCP 长连接接收一批可用数据。
         *
         * <p>该方法至少等待一个字节，收到数据后返回本次已经到达的全部字节。</p>
         *
         * @return 本次从网络读取的原始字节
         * @throws IOException 网络接收失败、等待超时或连接关闭
         */
        public byte[] receive() throws IOException {
            // 接收和发送使用不同锁，后台接收不会阻塞前台发送。
            synchronized (receiveLock) {
                // 首次读取会等待网络数据到达，返回值小于零表示对方已关闭连接。
                byte[] buffer = new byte[BUFFER_LENGTH];
                int firstReadCount = inputStream.read(buffer);
                if (firstReadCount == -1) {
                    throw new EOFException("TCP对端已关闭连接");
                }

                // 将首次数据写入结果缓冲区。
                ByteArrayOutputStream output = new ByteArrayOutputStream(firstReadCount);
                output.write(buffer, 0, firstReadCount);

                // 继续读取当前已经到达的数据，不额外等待下一批网络数据。
                while (inputStream.available() > 0) {
                    int readCount = inputStream.read(
                            buffer,
                            0,
                            Math.min(buffer.length, inputStream.available()));
                    if (readCount == -1) {
                        break;
                    }
                    output.write(buffer, 0, readCount);
                }
                return output.toByteArray();
            }
        }

        /**
         * 从当前TCP连接接收并解析一个完整的PDXP TCP帧。
         *
         * @return 当前TCP帧对应的PDXP数据包
         * @throws IOException TCP接收、定界、反转义或PDXP解析失败
         */
        public PdxpParser.PdxpPacket receivePdxpPacket() throws IOException {
            // 与普通接收共用同一把锁，避免多个线程同时读取同一输入流导致帧错乱。
            synchronized (receiveLock) {
                // 复用PDXP解析器现有的TCP流接收逻辑，统一处理7E定界和7D反转义。
                List<PdxpParser.PdxpPacket> packets = PdxpParser.receiveTcp(inputStream);
                if (packets.size() != 1) {
                    throw new IOException("一个TCP PDXP帧必须且只能解析出一个数据包");
                }

                return packets.get(0);
            }
        }

        /** @return 当前连接是否仍处于可用状态 */
        public boolean isOpen() {
            // 同时检查连接状态和输入输出关闭状态。
            return socket.isConnected()
                    && !socket.isClosed()
                    && !socket.isInputShutdown()
                    && !socket.isOutputShutdown();
        }

        /** 关闭当前 TCP 长连接及其输入输出流。 */
        @Override
        public void close() throws IOException {
            // 关闭套接字会同时关闭关联的输入流和输出流。
            socket.close();
        }
    }

    /** 可以持续接受客户端连接的 TCP 服务端。 */
    public static final class TcpServer implements AutoCloseable {

        /** 当前服务端使用的监听套接字。 */
        private final ServerSocket serverSocket;

        /** 每个客户端连接使用的读取超时时间。 */
        private final int clientReadTimeoutMillis;

        /** 保存已完成绑定的服务端套接字和客户端读取配置。 */
        private TcpServer(ServerSocket serverSocket, int clientReadTimeoutMillis) {
            this.serverSocket = serverSocket;
            this.clientReadTimeoutMillis = clientReadTimeoutMillis;
        }

        /**
         * 等待并接受一个新的 TCP 客户端连接。
         *
         * @return 可以连续发送和接收数据的客户端连接
         * @throws IOException 等待连接失败或等待超时
         */
        public TcpConnection accept() throws IOException {
            // 阻塞等待下一个客户端建立连接。
            Socket clientSocket = serverSocket.accept();
            try {
                // 为新客户端设置统一的读取超时时间。
                clientSocket.setSoTimeout(clientReadTimeoutMillis);

                // 将已连接套接字包装为通用长连接对象。
                return new TcpConnection(clientSocket);
            } catch (IOException exception) {
                // 客户端连接初始化失败时主动关闭套接字。
                clientSocket.close();
                throw exception;
            }
        }

        /** @return 当前 TCP 服务端是否仍在监听 */
        public boolean isOpen() {
            // 服务端套接字未关闭时可以继续接受客户端连接。
            return !serverSocket.isClosed();
        }

        /** 关闭 TCP 服务端并停止接受新连接。 */
        @Override
        public void close() throws IOException {
            // 关闭监听套接字并释放本机监听端口。
            serverSocket.close();
        }
    }

    /**
     * TCP 客户端运行入口。
     *
     * <p>运行客户端时保留此方法，并保持下方服务端 main 方法处于注释状态。</p>
     */
    public static void main(String[] args) throws IOException {
        // 截图中的网络调试工具运行在本机，因此连接本机地址。
        String host = "127.0.0.1";

        // 截图中的网络调试工具监听端口为8080。
        int port = 8080;

        // 建立一次长连接，并通过控制台连续输入需要发送的消息。
        try (Scanner scanner = new Scanner(System.in, "UTF-8");
             TcpConnection connection = connect(host, port, 5_000, 0)) {
            // 连接成功后提示控制台输入规则。
            System.out.println("TCP连接成功，请输入发送内容；输入“退出”结束程序");

            // 创建独立接收线程，持续读取网络调试助手主动发送的数据。
            Thread receiveThread = new Thread(() -> {
                // 当前连接可用时持续接收，不影响主线程发送数据。
                while (connection.isOpen()) {
                    try {
                        // 接收网络调试助手本次已经发送到达的数据。
                        byte[] receivedData = connection.receive();

                        // 按UTF-8文本和十六进制两种形式输出，便于核对数据。
                        System.out.println("收到数据："
                                + new String(receivedData, StandardCharsets.UTF_8));
                        System.out.println("收到字节：" + PdxpParser.toHex(receivedData));
                    } catch (IOException exception) {
                        // 对端断开或接收失败时结束后台接收线程。
                        if (connection.isOpen()) {
                            System.out.println("TCP接收结束：" + exception.getMessage());
                        }
                        break;
                    }
                }
            }, "TCP接收线程");

            // 设置为后台线程，主程序退出时无需额外等待该线程。
            receiveThread.setDaemon(true);
            receiveThread.start();

            // 持续读取控制台内容并复用当前连接发送。
            while (connection.isOpen() && scanner.hasNextLine()) {
                // 读取本次准备发送的一行文本。
                String message = scanner.nextLine();

                // 输入指定结束词时结束循环并关闭连接。
                if ("退出".equals(message)) {
                    break;
                }

                // 将文本转换为UTF-8字节，适配截图中的ASCII显示模式。
                byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
                connection.send(sendData);

                // 这里只验证发送，不等待固定1024字节响应，避免程序阻塞。
                System.out.println("发送完成，字节数量：" + sendData.length);
            }
        }
    }

    /*
     * TCP 服务端运行入口。
     *
     * 使用说明：
     * 1. 运行服务端前，先将上方客户端 main 方法整体注释掉。
     * 2. 再取消下面服务端 main 方法的注释。
     * 3. Java 同一个类不能同时存在两个参数完全相同的 main 方法。
     * 4. 服务端默认监听全部本机网卡的8080端口。
     * 5. 示例每次读取一个字节，正式业务应根据实际消息格式调整接收长度。
     *
    public static void main(String[] args) throws IOException {
        // 监听全部本机网卡；如果只允许本机连接，可以修改为127.0.0.1。
        String localAddress = "0.0.0.0";

        // 服务端监听端口，根据实际环境修改。
        int port = 8080;

        // 示例每次读取一个字节，正式业务应根据实际消息格式确定长度。
        int messageLength = 1;

        // 创建一次服务端监听，并在程序结束时自动关闭。
        try (TcpServer server = listen(localAddress, port, 0, 0)) {
            // 输出服务端启动信息，便于确认监听地址和端口。
            System.out.println("TCP服务端已启动，监听地址：" + localAddress + "，端口：" + port);

            // 服务端持续运行并接受新的客户端连接。
            while (server.isOpen()) {
                // 等待一个客户端建立连接，并在客户端断开后释放连接资源。
                try (TcpConnection connection = server.accept()) {
                    // 输出客户端连接状态。
                    System.out.println("客户端已连接");

                    // 当前客户端保持连接期间，可以连续接收和发送多条数据。
                    while (connection.isOpen()) {
                        try {
                            // 按示例约定长度接收下一条原始数据。
                            byte[] receivedData = connection.receive(messageLength);

                            // 输出本次收到的原始字节数量。
                            System.out.println("收到数据，字节数量：" + receivedData.length);

                            // 将收到的原始字节回复给当前客户端，便于联调验证。
                            connection.send(receivedData);
                        } catch (EOFException exception) {
                            // 客户端主动关闭连接时结束当前客户端处理循环。
                            System.out.println("客户端已断开");
                            break;
                        }
                    }
                }
            }
        }
    }
    */
}
