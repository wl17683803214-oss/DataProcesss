package com.example.dataprocess.entity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 采集和处理线程共享的只读卫星名称快照。 */
public final class BaselineSatelliteInfo {
    /** 卫星名称。 */
    private final String satelliteName;
    /** 当前卫星的通道编码与名称映射。 */
    private final Map<String, String> channelNames;

    /** 复制映射，保证跨线程读取期间不会发生修改。 */
    public BaselineSatelliteInfo(String satelliteName, Map<String, String> channelNames) {
        this.satelliteName = satelliteName;
        this.channelNames = Collections.unmodifiableMap(new LinkedHashMap<>(channelNames));
    }

    /** 获取卫星名称。 */
    public String getSatelliteName() {
        return satelliteName;
    }

    /** 获取当前卫星下的通道名称。 */
    public Map<String, String> getChannelNames() {
        return channelNames;
    }
}
