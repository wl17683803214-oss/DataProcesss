package com.example.common.protocol.fep;

/** FEP协议包类型。 */
public enum FepPacketType {

    /** 发送方请求开始传输文件。 */
    SEND_REQUEST(0x01, "发送请求包"),
    /** 接收方返回文件状态和续传位置。 */
    REQUEST_RESPONSE(0x02, "请求应答包"),
    /** 接收方确认文件已经完整接收。 */
    FINISH_CONFIRMATION(0x03, "结束确认包"),
    /** 发送方传输一个文件数据单元。 */
    DATA(0x04, "数据包");

    /** 协议中的单字节类型值。 */
    private final int code;
    /** 包类型中文名称。 */
    private final String name;

    FepPacketType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** 根据协议值取得包类型。 */
    public static FepPacketType fromCode(int code) {
        // 遍历已定义类型，避免在解析代码中重复判断协议值。
        for (FepPacketType packetType : values()) {
            if (packetType.code == code) {
                return packetType;
            }
        }

        throw new IllegalArgumentException(
                String.format("不支持的FEP包类型：0x%02X", code));
    }
}
