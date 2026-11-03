package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/** 指令响应独立接收配置，修改后重启数据处理服务生效。 */
@Component
@Validated
@ConfigurationProperties(prefix = "data-processing.command-response")
public class CommandResponseProperties {

    /** 是否启用接收；旧环境未配置时不额外占用端口。 */
    private boolean enabled;
    /** 本机监听地址，默认监听全部网卡。 */
    @NotBlank(message = "指令响应监听地址不能为空")
    private String host = "0.0.0.0";
    /** 指令响应独立监听端口。 */
    @Min(value = 1, message = "指令响应监听端口不能小于1")
    @Max(value = 65535, message = "指令响应监听端口不能大于65535")
    private int port = 19104;
    /** 预留消息主题，本次只记录日志，不发送消息。 */
    @NotBlank(message = "指令响应消息主题不能为空")
    private String topic = "topic_zl_response_sjcl";
    /** 标准PDXP包头之前的传输头字节数，零表示没有。 */
    @Min(value = 0, message = "指令响应传输头长度不能小于0")
    @Max(value = 65475, message = "指令响应传输头长度超过UDP报文允许范围")
    private int transportHeaderLength;

    /** 读取接收开关。 */
    public boolean isEnabled() {
        return enabled;
    }

    /** 绑定接收开关。 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 读取本机监听地址。 */
    public String getHost() {
        return host;
    }

    /** 绑定本机监听地址。 */
    public void setHost(String host) {
        this.host = host;
    }

    /** 读取监听端口。 */
    public int getPort() {
        return port;
    }

    /** 绑定监听端口。 */
    public void setPort(int port) {
        this.port = port;
    }

    /** 读取预留消息主题。 */
    public String getTopic() {
        return topic;
    }

    /** 绑定预留消息主题。 */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /** 读取传输头长度。 */
    public int getTransportHeaderLength() {
        return transportHeaderLength;
    }

    /** 绑定传输头长度。 */
    public void setTransportHeaderLength(int transportHeaderLength) {
        this.transportHeaderLength = transportHeaderLength;
    }
}
