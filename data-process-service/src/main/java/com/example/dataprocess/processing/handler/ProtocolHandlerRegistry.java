package com.example.dataprocess.processing.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 根据传输协议查找协议处理器，新增协议时无需修改采集接收器。 */
@Component
public class ProtocolHandlerRegistry {

    /** 传输协议与处理器的注册关系。 */
    private final Map<Integer, ProtocolHandler> handlers =
            new HashMap<Integer, ProtocolHandler>();

    public ProtocolHandlerRegistry(List<ProtocolHandler> protocolHandlers) {
        // Spring自动注入全部协议处理器，并在启动时检查重复注册。
        for (ProtocolHandler handler : protocolHandlers) {
            for (Integer transferProtocol : handler.supportedTransferProtocols()) {
                ProtocolHandler previous = handlers.put(transferProtocol, handler);
                if (previous != null) {
                    throw new IllegalStateException(
                            "传输协议存在重复处理器：" + transferProtocol);
                }
            }
        }
    }

    /** 查找指定传输协议的处理器。 */
    public ProtocolHandler get(Integer transferProtocol) {
        if (transferProtocol == null) {
            throw new IllegalArgumentException("采集接口没有配置传输协议");
        }
        ProtocolHandler handler = handlers.get(transferProtocol);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "暂不支持的采集传输协议：" + transferProtocol);
        }
        return handler;
    }
}
