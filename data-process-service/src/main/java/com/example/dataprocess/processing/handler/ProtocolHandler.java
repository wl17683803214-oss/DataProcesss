package com.example.dataprocess.processing.handler;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.tool.TcpTool;
import com.example.dataprocess.tool.UdpTool;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

/** 数据协议处理器定义，由具体协议实现UDP报文处理和TCP流拆包。 */
public interface ProtocolHandler {

    /** 返回当前处理器支持的传输协议。 */
    List<Integer> supportedTransferProtocols();

    /** 为一个UDP采集接口创建独立协议会话。 */
    UdpSession createUdpSession(
            CollectInterfaceRuntimeConfig config,
            UdpTool.DatagramEndpoint endpoint);

    /** 为一个TCP采集接口创建独立协议会话。 */
    TcpSession createTcpSession(CollectInterfaceRuntimeConfig config);

    /** 单个UDP采集接口使用的协议会话。 */
    interface UdpSession {

        /** 处理一个具有完整UDP边界的报文。 */
        void handle(UdpTool.ReceivedPacket packet);
    }

    /** 单个TCP采集接口使用的协议会话。 */
    interface TcpSession {

        /** 持续从当前TCP连接恢复并处理完整协议包。 */
        void receive(
                TcpTool.TcpConnection connection,
                BooleanSupplier running) throws IOException;
    }
}
