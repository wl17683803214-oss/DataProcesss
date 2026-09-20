package com.example.datasimulator.fep.config;

/** 文件交换模拟源支持的传输方式。 */
public enum FepTransferType {

    /** 使用面向连接的可靠字节流。 */
    TCP("传输控制协议"),
    /** 使用无连接的数据报。 */
    UDP("用户数据报协议");

    /** 传输方式中文名称。 */
    private final String name;

    FepTransferType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
