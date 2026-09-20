package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 数据处理RPC客户端配置。 */
@Component
@ConfigurationProperties(prefix = "data-processing.rpc")
public class DataProcessingRpcProperties {

    /** RPC服务实际地址。 */
    private String host = "127.0.0.1";
    /** RPC服务端口。 */
    private int port = 50051;
    /** 单帧RPC调用超时时间，单位为毫秒。 */
    private long timeoutMillis = 5000L;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
