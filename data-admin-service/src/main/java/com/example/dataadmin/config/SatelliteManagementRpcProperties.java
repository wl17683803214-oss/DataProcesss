package com.example.dataadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 卫星目录管理gRPC客户端配置。 */
@Component
@ConfigurationProperties(prefix = "satellite-management.rpc")
public class SatelliteManagementRpcProperties {

    /** gRPC服务实际地址。 */
    private String host = "127.0.0.1";
    /** gRPC服务端口。 */
    private int port = 50051;
    /** 单次管理调用超时时间，单位为毫秒。 */
    private long timeoutMillis = 5000L;
    /** 单次响应允许的最大消息大小。 */
    private int maxInboundMessageSize = 16 * 1024 * 1024;
    /** 参数表上传允许的最大文件大小。 */
    private int maxUploadFileSize = 16 * 1024 * 1024;

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

    public int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public void setMaxInboundMessageSize(int maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
    }

    public int getMaxUploadFileSize() {
        return maxUploadFileSize;
    }

    public void setMaxUploadFileSize(int maxUploadFileSize) {
        this.maxUploadFileSize = maxUploadFileSize;
    }
}
