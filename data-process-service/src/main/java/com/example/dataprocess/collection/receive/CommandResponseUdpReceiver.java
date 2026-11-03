package com.example.dataprocess.collection.receive;

import SatDataCenter.DataExchange.TmTc.TelecommandMessages.CommandExecResultMsg;
import com.example.dataprocess.config.CommandResponseProperties;
import com.example.dataprocess.protocol.command.CommandResponseParser;
import com.example.dataprocess.protocol.command.CommandResponseResult;
import com.example.dataprocess.tool.PdxpParser;
import com.example.dataprocess.tool.UdpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** 独立接收采集指令响应，组装消息后逐条记录日志。 */
@Component
public class CommandResponseUdpReceiver implements SmartLifecycle {

    /** 指令响应接收日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandResponseUdpReceiver.class);
    /** 关闭时等待接收线程退出的最长毫秒数。 */
    private static final long STOP_WAIT_MILLIS = 5000L;
    /** 独立接收配置，不依赖数据库中的采集接口。 */
    private final CommandResponseProperties properties;
    /** 当前是否持续接收。 */
    private volatile boolean running;
    /** 当前已经绑定端口的单播接收端。 */
    private UdpTool.UnicastEndpoint endpoint;
    /** 单个阻塞接收线程。 */
    private Thread receiverThread;

    /** 注入独立接收配置。 */
    public CommandResponseUdpReceiver(CommandResponseProperties properties) {
        this.properties = properties;
    }

    /** 随应用启动绑定端口，配置无效或端口绑定失败时明确阻止启动。 */
    @Override
    public synchronized void start() {
        // 第一步：重复启动不重复占用端口，未启用时只输出一次状态。
        if (running) {
            return;
        }
        if (receiverThread != null && receiverThread.isAlive()) {
            throw new IllegalStateException("上一次采集指令响应接收线程尚未退出，不能重复启动");
        }
        if (!properties.isEnabled()) {
            LOGGER.info("采集指令响应接收未启用");
            return;
        }

        // 第二步：先绑定端口再启动线程，避免应用启动成功却未能监听。
        try {
            endpoint = UdpTool.openUnicast(properties.getHost(), properties.getPort(), 0);
        } catch (IOException exception) {
            throw new IllegalStateException("采集指令响应监听端口绑定失败", exception);
        }

        // 第三步：独立线程串行处理响应，不占用遥测采集和业务处理线程池。
        UdpTool.UnicastEndpoint receivingEndpoint = endpoint;
        running = true;
        receiverThread = new Thread(() -> receive(receivingEndpoint), "采集指令响应接收线程");
        receiverThread.setDaemon(true);
        try {
            receiverThread.start();
        } catch (RuntimeException exception) {
            running = false;
            receivingEndpoint.close();
            throw new IllegalStateException("采集指令响应接收线程启动失败", exception);
        }
        LOGGER.info("采集指令响应监听已启动，监听地址：{}，监听端口：{}，配置主题：{}",
                properties.getHost(), properties.getPort(), properties.getTopic());
    }

    /** 保持UDP报文边界，网络异常结束当前接收并释放资源。 */
    private void receive(UdpTool.UnicastEndpoint receivingEndpoint) {
        // 第一步：阻塞等待数据，每个报文交给独立的容错处理入口。
        try {
            while (running && receivingEndpoint.isOpen()) {
                handleDatagram(receivingEndpoint.receivePacket());
            }
        } catch (IOException exception) {
            if (running) {
                LOGGER.error("采集指令响应网络接收失败，监听端口：{}",
                        properties.getPort(), exception);
            }
        } finally {
            // 第二步：正常关闭和网络故障都释放端口，并更新实际运行状态。
            receivingEndpoint.close();
            running = false;
        }
    }

    /** 拆分单个UDP报文，坏包或单帧错误不影响后续响应接收。 */
    private void handleDatagram(UdpTool.ReceivedPacket datagram) {
        // 第一步：复用现有解析器，支持可选传输头以及一个报文中的多个PDXP包。
        List<PdxpParser.PdxpPacket> packets;
        try {
            packets = PdxpParser.parseTransportUdp(
                    datagram.getData(), properties.getTransportHeaderLength());
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("采集指令响应报文解析失败，来源地址：{}，来源端口：{}，原因：{}",
                    datagram.getSourceAddress(), datagram.getSourcePort(), exception.getMessage());
            return;
        }
        if (packets.isEmpty()) {
            LOGGER.warn("采集指令响应报文没有完整协议包，来源地址：{}，来源端口：{}",
                    datagram.getSourceAddress(), datagram.getSourcePort());
            return;
        }

        // 第二步：逐帧识别业务并组装，单帧异常不会跳过同一报文中的后续帧。
        for (PdxpParser.PdxpPacket packet : packets) {
            if (!CommandResponseParser.supports(packet)) {
                LOGGER.warn("采集指令响应监听收到其他业务报文，包序号：{}，业务标识：{}",
                        packet.getSequenceNumber(), packet.getDataId());
                continue;
            }
            try {
                CommandExecResultMsg message = CommandResponseParser.parse(packet);
                logResponse(datagram, packet, message);
            } catch (RuntimeException exception) {
                LOGGER.warn("采集指令响应组装失败，来源地址：{}，来源端口：{}，包序号：{}，原因：{}",
                        datagram.getSourceAddress(), datagram.getSourcePort(),
                        packet.getSequenceNumber(), exception.getMessage());
            }
        }
    }

    /** 每条合法响应打印五个消息字段，结果使用中文名称，主题仅用于日志定位。 */
    private void logResponse(UdpTool.ReceivedPacket datagram, PdxpParser.PdxpPacket packet,
            CommandExecResultMsg message) {
        // 将消息时间还原成时间点，完整保留秒和纳秒，不输出消息对象的英文字段名。
        Instant sendingTime = Instant.ofEpochSecond(
                message.getSendingTime().getSeconds(), message.getSendingTime().getNanos());
        LOGGER.info("收到采集指令响应，来源地址：{}，来源端口：{}，包序号：{}，卫星代号：{}，"
                        + "指令代号：{}，指令标识：{}，发送结果：{}，发送时间：{}，配置主题：{}",
                datagram.getSourceAddress(), datagram.getSourcePort(), packet.getSequenceNumber(),
                message.getSatCode(), message.getCmSymbol(), message.getCommandId(),
                CommandResponseResult.fromProtoResult(message.getSendingResult()).getLabel(),
                sendingTime, properties.getTopic());
    }

    /** 关闭套接字解除阻塞，并等待接收线程有界退出。 */
    @Override
    public synchronized void stop() {
        // 第一步：先关闭接收开关和套接字，避免继续等待新报文。
        running = false;
        if (endpoint != null) {
            endpoint.close();
        }

        // 第二步：等待当前报文处理结束，中断时保留调用线程的中断状态。
        if (receiverThread != null && receiverThread != Thread.currentThread()) {
            try {
                receiverThread.join(STOP_WAIT_MILLIS);
                if (receiverThread.isAlive()) {
                    LOGGER.warn("采集指令响应接收线程未在关闭等待时间内退出");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                LOGGER.warn("等待采集指令响应接收线程退出时被中断");
            }
        }
    }

    /** 释放资源后通知应用生命周期管理器继续关闭。 */
    @Override
    public void stop(Runnable callback) {
        // 关闭失败也必须通知生命周期管理器，避免其他组件无限等待。
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    /** 读取实际接收运行状态。 */
    @Override
    public boolean isRunning() {
        return running;
    }

    /** 由应用生命周期自动调用启动入口。 */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /** 在其他组件初始化后启动，并在应用关闭时优先停止接收。 */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
