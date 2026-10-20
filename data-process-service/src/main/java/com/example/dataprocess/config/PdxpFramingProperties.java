package com.example.dataprocess.config;

import com.example.dataprocess.protocol.rpc.PdxpDataPayloadParser;
import com.example.dataprocess.tool.PdxpParser;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地与远程分别维护PDXP附加数据长度配置，零表示没有该部分。 */
@Component
@ConfigurationProperties(prefix = "data-processing.pdxp")
public class PdxpFramingProperties {
    /** 本地处理独立拆帧配置。 */
    private final Framing local = new Framing();
    /** 远程处理独立拆帧配置。 */
    private final Framing rpc = new Framing();

    /** 提供本地配置绑定入口。 */
    public Framing getLocal() {
        return local;
    }

    /** 提供远程配置绑定入口。 */
    public Framing getRpc() {
        return rpc;
    }

    /** 按接口远程开关选择长度，未开启时使用本地配置。 */
    public Framing select(Integer rpcEnabled) {
        return Integer.valueOf(1).equals(rpcEnabled) ? rpc : local;
    }

    /** 两种处理模式复用长度定义和校验，各自保存配置值。 */
    public static class Framing {
        /** 报文最前面的传输头长度，未配置时使用原默认值。 */
        private int transportHeaderLength = PdxpParser.TRANSPORT_HEADER_LENGTH;
        /** PDXP数据域开头的自定义数据长度。 */
        private int customDataLength = PdxpDataPayloadParser.CUSTOM_DATA_LENGTH;
        /** PDXP数据域末尾的遥测帧头长度。 */
        private int telemetryHeaderLength = PdxpDataPayloadParser.TELEMETRY_HEADER_LENGTH;

        /** 校验字节数，避免负数或超出协议长度范围的配置进入解析流程。 */
        private int validateLength(int length, String name) {
            if (length < 0 || length > 65535) {
                throw new IllegalArgumentException(name + "字节数必须在0至65535之间，0表示没有");
            }
            return length;
        }


        /** 读取当前分组的长度。 */
        public int getTransportHeaderLength() {
            return transportHeaderLength;
        }

        /** 校验并保存当前分组的长度。 */
        public void setTransportHeaderLength(int length) {
            transportHeaderLength = validateLength(length, "传输头");
        }

        /** 读取当前分组的长度。 */
        public int getCustomDataLength() {
            return customDataLength;
        }

        /** 校验并保存当前分组的长度。 */
        public void setCustomDataLength(int length) {
            customDataLength = validateLength(length, "自定义数据");
        }

        /** 读取当前分组的长度。 */
        public int getTelemetryHeaderLength() {
            return telemetryHeaderLength;
        }

        /** 校验并保存当前分组的长度。 */
        public void setTelemetryHeaderLength(int length) {
            telemetryHeaderLength = validateLength(length, "遥测帧头");
        }
    }
}
