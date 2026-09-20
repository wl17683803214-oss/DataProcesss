package com.example.datasimulator.fep.transport;

import com.example.common.protocol.fep.FepPacketType;
import com.example.common.protocol.fep.FepProtocolTool;

import java.io.IOException;

/** 文件交换协议的网络传输接口。 */
public interface FepTransport extends AutoCloseable {

    /** 发送一个完整协议包。 */
    void send(byte[] packetData) throws IOException;

    /** 接收一个指定类型的完整协议包。 */
    byte[] receive(FepPacketType expectedType) throws IOException;

    /** 取得接收端固定应答包的长度。 */
    static int fixedPacketLength(FepPacketType packetType) {
        // 请求应答包由类型、文件名、数据单元号和文件标识组成。
        if (packetType == FepPacketType.REQUEST_RESPONSE) {
            return 1
                    + FepProtocolTool.FILE_NAME_LENGTH
                    + FepProtocolTool.UNIT_NUMBER_FIELD_LENGTH
                    + FepProtocolTool.FILE_ID_FIELD_LENGTH;
        }

        // 结束确认包由类型和文件标识组成。
        if (packetType == FepPacketType.FINISH_CONFIRMATION) {
            return 1 + FepProtocolTool.FILE_ID_FIELD_LENGTH;
        }
        throw new IllegalArgumentException("该协议包不是接收端固定应答包");
    }
}
