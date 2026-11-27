package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 采集接口的UDP接收缓冲配置。 */
@Component
@ConfigurationProperties(prefix = "data-processing.udp")
public class UdpReceiveProperties {

    /** 每个UDP采集套接字向操作系统申请的缓冲字节数。 */
    private volatile int receiveBufferBytes = 4 * 1024 * 1024;

    /** 读取新建UDP端点时使用的缓冲字节数。 */
    public int getReceiveBufferBytes() {
        return receiveBufferBytes;
    }

    /** 配置缓冲字节数，拒绝无效的非正数。 */
    public void setReceiveBufferBytes(int receiveBufferBytes) {
        if (receiveBufferBytes <= 0) {
            throw new IllegalArgumentException("UDP采集接收缓冲字节数必须大于0");
        }
        this.receiveBufferBytes = receiveBufferBytes;
    }
}
