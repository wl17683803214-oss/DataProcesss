package com.example.dataprocess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/** 指令响应消息主题及拆帧配置，监听地址和启停由采集接口管理。 */
@Component
@Validated
@ConfigurationProperties(prefix = "data-processing.command-response")
public class CommandResponseProperties {

    /** 指令响应消息主题，发送单指令结果的原始协议缓冲区字节。 */
    @NotBlank(message = "指令响应消息主题不能为空")
    private String topic = "topic_zl_out_sjcl";
    /** 标准PDXP包头之前的传输头字节数，零表示没有。 */
    @Min(value = 0, message = "指令响应传输头长度不能小于0")
    @Max(value = 65475, message = "指令响应传输头长度超过UDP报文允许范围")
    private int transportHeaderLength;

    /** 读取消息主题。 */
    public String getTopic() {
        return topic;
    }

    /** 绑定消息主题。 */
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
