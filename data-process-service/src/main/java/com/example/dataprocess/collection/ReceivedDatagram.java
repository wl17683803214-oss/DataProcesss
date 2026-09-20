package com.example.dataprocess.collection;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import java.util.Arrays;

/** UDP接收线程提交给数据处理线程的不可变报文。 */
public final class ReceivedDatagram {

    /** 收包时使用的接口配置快照。 */
    private final CollectInterfaceRuntimeConfig interfaceConfig;
    /** 本次UDP报文的完整字节副本。 */
    private final byte[] data;

    public ReceivedDatagram(
            CollectInterfaceRuntimeConfig interfaceConfig,
            byte[] data) {
        // 复制字节，确保接收缓冲区不会被其他线程修改。
        this.interfaceConfig = interfaceConfig;
        this.data = Arrays.copyOf(data, data.length);
    }

    public CollectInterfaceRuntimeConfig getInterfaceConfig() {
        return interfaceConfig;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
}
