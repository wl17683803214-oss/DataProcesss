package com.example.simulator.protocol;

import com.example.simulator.config.SimulatorProperties;
import com.example.simulator.model.SourceConfig;
import com.example.simulator.enums.ProtocolProfile;
import com.example.simulator.enums.SendMode;
import com.example.simulator.runtime.ExecutionContext;
import com.example.common.protocol.fep.FepProtocolTool;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.nio.file.*;

/** 独立模拟源发送实现，FEP编解码复用公共模块。 */
public class SimulatorSender {
    private final SimulatorProperties properties;

    public SimulatorSender(SimulatorProperties properties) {
        this.properties = properties;
    }

    /** 根据协议分派，保持每次执行资源隔离。 */
    public void send(SourceConfig config, ExecutionContext context) throws Exception {
        ProtocolProfile profile = ProtocolProfile.of(config.transferProtocol);
        if (profile == ProtocolProfile.JSON) {
            sendHttp(config, context);
        } else if (profile == ProtocolProfile.PDXP) {
            sendPdxp(config, context);
        } else {
            sendFep(config, context);
        }
    }

    /** 按本实例的帧长读取并发送，每轮重开文件但不重置包序号。 */
    private void sendPdxp(SourceConfig config, ExecutionContext context) throws Exception {
        PdxpEncoder encoder = new PdxpEncoder(config.pdxp);
        int frameLength = config.pdxp.inputFrameLength;
        InetAddress target = InetAddress.getByName(config.targetHost);
        DatagramSocket socket = createUdpSocket(config);
        try (DatagramSocket closeableSocket = socket) {
            context.bind(closeableSocket);
            do {
                context.check();
                try (RandomAccessFile input = new RandomAccessFile(config.filePath, "r")) {
                    long length = input.length();
                    if (length == 0 || length % frameLength != 0) {
                        throw new IllegalArgumentException("PDXP文件必须非空且大小为配置帧长度的整数倍");
                    }
                    byte[] frame = new byte[frameLength];
                    for (long offset = 0; offset < length; offset += frameLength) {
                        context.check();
                        input.readFully(frame);
                        byte[] packet = encoder.encode(frame);
                        closeableSocket.send(new DatagramPacket(packet, packet.length, target, config.targetPort));
                        context.sent(packet.length, true);
                        context.pause(config.sendIntervalMillis);
                    }
                }
                context.completeRound();
            } while (config.loopEnabled == 1);
        }
    }

    /** 按发送模式创建单播或组播UDP套接字。 */
    private DatagramSocket createUdpSocket(SourceConfig config) throws Exception {
        if (SendMode.of(config.sendMode) == SendMode.UNICAST) {
            return new DatagramSocket();
        }
        MulticastSocket socket = new MulticastSocket();
        try {
            // 组播默认只允许本地网段转发，防止无意跨路由扩散。
            socket.setTimeToLive(1);
            if (config.multicastInterface != null) {
                InetAddress localAddress = InetAddress.getByName(config.multicastInterface);
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localAddress);
                if (networkInterface == null || !networkInterface.isUp()) {
                    throw new IllegalArgumentException("找不到可用的组播出口网卡");
                }
                socket.setNetworkInterface(networkInterface);
            }
            return socket;
        } catch (Exception exception) {
            socket.close();
            throw exception;
        }
    }

    /** 将配置的JSON正文按HTTP POST重复发送，并沿用现有循环与统计控制。 */
    private void sendHttp(SourceConfig config, ExecutionContext context) throws Exception {
        byte[] body = config.messageContent.getBytes(StandardCharsets.UTF_8);
        do {
            context.check();
            URL target = new URI("http", null, config.targetHost, config.targetPort,
                    config.targetPath, null, null).toURL();
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(properties.getConnectTimeoutMillis());
            connection.setReadTimeout(properties.getResponseTimeoutMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            // 将连接登记到运行上下文，停止操作可立即断开当前请求。
            context.bind(new Closeable() {
                @Override
                public void close() {
                    connection.disconnect();
                }
            });
            try {
                // 发送本次完整JSON请求体。
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                    output.flush();
                }
                int responseCode = connection.getResponseCode();
                InputStream response = responseCode >= 400
                        ? connection.getErrorStream() : connection.getInputStream();
                if (response != null) {
                    try (InputStream input = response) {
                        byte[] buffer = new byte[1024];
                        while (input.read(buffer) != -1) {
                            context.check();
                        }
                    }
                }
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException("HTTP接收端返回失败状态码：" + responseCode);
                }
                context.sent(body.length, true);
                context.completeRound();
            } finally {
                connection.disconnect();
            }
            if (config.loopEnabled == 1) {
                context.pause(Math.max(1, config.sendIntervalMillis));
            }
        } while (config.loopEnabled == 1);
    }

    /** 每轮建立独立连接，兼容接收端确认后主动断开的行为。 */
    private void sendFep(SourceConfig config, ExecutionContext context) throws Exception {
        do {
            context.check();
            sendFepRound(config, context);
            context.completeRound();
            if (config.loopEnabled == 1) {
                context.pause(Math.max(1, config.sendIntervalMillis));
            }
        } while (config.loopEnabled == 1);
    }

    /** 执行请求、续传、文件单元发送与结束确认。 */
    private void sendFepRound(SourceConfig config, ExecutionContext context) throws Exception {
        Path path = Paths.get(config.filePath);
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
             Socket socket = new Socket()) {
            // 读取本次文件长度并利用公共编码器校验文件名。
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("FEP文件超过协议长度上限");
            }
            String fileName = path.getFileName().toString();
            byte[] request = FepProtocolTool.buildSendRequest(fileName, (int) length);
            context.bind(socket);
            socket.connect(new InetSocketAddress(config.targetHost, config.targetPort),
                    properties.getConnectTimeoutMillis());
            socket.setSoTimeout(properties.getResponseTimeoutMillis());
            OutputStream output = socket.getOutputStream();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            write(output, request, false, context);
            // 固定长度完整读取，公共解析器负责验证应答类型。
            byte[] responseBytes = new byte[71];
            input.readFully(responseBytes);
            FepProtocolTool.RequestResponse response = FepProtocolTool.parseRequestResponse(responseBytes);
            if (!fileName.equals(response.getFileName())) {
                throw new IllegalStateException("应答文件名与请求不一致");
            }
            int unit = response.getUnitNumber();
            if (unit == FepProtocolTool.FILE_ALREADY_COMPLETE) {
                return;
            }
            int unitLength = properties.getDataUnitLength();
            long offset = (long) unit * unitLength;
            if (unit < 0 || offset > length) {
                throw new IllegalStateException("接收端拒绝文件或返回无效续传位置");
            }
            // 从接收端指定位置续传，并保留每个实例自己的单元编号。
            file.seek(offset);
            long remaining = length - offset;
            while (remaining > 0) {
                context.check();
                int count = (int) Math.min(unitLength, remaining);
                byte[] data = new byte[count];
                file.readFully(data);
                write(output, FepProtocolTool.buildDataPacket(unit++, response.getFileId(), data), true, context);
                remaining -= count;
                context.pause(config.sendIntervalMillis);
            }
            // 整数倍文件及空文件需要发送空结束单元。
            if (length % unitLength == 0) {
                write(output, FepProtocolTool.buildDataPacket(unit, response.getFileId(), new byte[0]), true, context);
            }
            byte[] finish = new byte[3];
            input.readFully(finish);
            if (FepProtocolTool.parseFinishConfirmation(finish).getFileId() != response.getFileId()) {
                throw new IllegalStateException("结束确认文件标识不一致");
            }
        }
    }

    /** 统一检查停止、发送和计数，控制包不增加数据单元数。 */
    private void write(OutputStream output, byte[] packet, boolean dataUnit,
                       ExecutionContext context) throws Exception {
        context.check();
        output.write(packet);
        output.flush();
        context.sent(packet.length, dataUnit);
    }
}

