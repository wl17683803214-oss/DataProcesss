package com.example.common.greptime;

import java.sql.Timestamp;

/** 遥测整帧，仅用于存储映射，不作为接口返回对象。 */
public class TelemetryFrameRecord {
    /** 同一帧在缓存快照与实时推送中的共同标识。 */
    private String realtimeId;
    /** 该帧在缓存中的到期毫秒值。 */
    private long expiresAt;
    /** 缓存计入额度的记录字节数，供浏览器同步淘汰。 */
    private int realtimeBytes;
    /** Redis分配的接口内写入顺序，用于快照与增量交接。 */
    private long realtimeOrder;

    public String getRealtimeId() { return realtimeId; }
    public void setRealtimeId(String value) { realtimeId = value; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long value) { expiresAt = value; }
    public int getRealtimeBytes() { return realtimeBytes; }
    public void setRealtimeBytes(int value) { realtimeBytes = value; }
    public long getRealtimeOrder() { return realtimeOrder; }
    public void setRealtimeOrder(long value) { realtimeOrder = value; }
    /** 源时间。 */
    private Timestamp sourceTime;
    /** 任务编号。 */
    private String taskId;
    /** 采集接口主键。 */
    private Long interfaceId;
    /** 来源类型。 */
    private String sourceType;
    /** 本地设备卫星主键或远程卫星代号。 */
    private String sourceIdentity;
    /** 遥测类型。 */
    private String telemetryType;
    /** 设备卫星名称。 */
    private String deviceSatelliteName;
    /** 通道名称。 */
    private String channelName;
    /** 完整采集报文。 */
    private byte[] raw;
    /** 帧检查状态编码。 */
    private String checkStatus;

    /** 获取源时间。 */
    public Timestamp getSourceTime() {
        return sourceTime;
    }

    /** 设置源时间。 */
    public void setSourceTime(Timestamp sourceTime) {
        this.sourceTime = sourceTime;
    }

    /** 获取任务编号。 */
    public String getTaskId() {
        return taskId;
    }

    /** 设置任务编号。 */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /** 获取采集接口主键。 */
    public Long getInterfaceId() {
        return interfaceId;
    }

    /** 设置采集接口主键。 */
    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
    }

    /** 获取来源类型。 */
    public String getSourceType() {
        return sourceType;
    }

    /** 设置来源类型。 */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /** 获取本地设备卫星主键或远程卫星代号。 */
    public String getSourceIdentity() {
        return sourceIdentity;
    }

    /** 设置本地设备卫星主键或远程卫星代号。 */
    public void setSourceIdentity(String sourceIdentity) {
        this.sourceIdentity = sourceIdentity;
    }

    /** 获取遥测类型。 */
    public String getTelemetryType() {
        return telemetryType;
    }

    /** 设置遥测类型。 */
    public void setTelemetryType(String telemetryType) {
        this.telemetryType = telemetryType;
    }

    /** 获取设备卫星名称。 */
    public String getDeviceSatelliteName() {
        return deviceSatelliteName;
    }

    /** 设置设备卫星名称。 */
    public void setDeviceSatelliteName(String deviceSatelliteName) {
        this.deviceSatelliteName = deviceSatelliteName;
    }

    /** 获取通道名称。 */
    public String getChannelName() {
        return channelName;
    }

    /** 设置通道名称。 */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /** 获取完整采集报文。 */
    public byte[] getRaw() {
        return raw;
    }

    /** 设置完整采集报文。 */
    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    /** 获取帧检查状态编码。 */
    public String getCheckStatus() {
        return checkStatus;
    }

    /** 设置帧检查状态编码。 */
    public void setCheckStatus(String checkStatus) {
        this.checkStatus = checkStatus;
    }
}
