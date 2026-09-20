package com.example.dataadmin.entity;

import java.time.LocalDateTime;

/** 帧检查异常的遥测原始帧。 */
public class InvalidTelemetryFrame {

    /** 采集接口主键。 */
    private Long interfaceId;

    /** 遥测消息时间。 */
    private LocalDateTime receiveTime;

    /** 卫星编码。 */
    private String satelliteCode;

    /** 通道编码。 */
    private String channelCode;

    /** 帧检查结果编码。 */
    private Integer checkResult;

    /** 帧检查结果中文名称。 */
    private String checkResultName;

    /** 完整原始帧字节数。 */
    private Integer rawLength;

    /** 大写十六进制格式的完整原始帧。 */
    private String rawFrame;

    /** 遥测帧唯一编码。 */
    private String uniqueCode;

    public Long getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
    }

    public LocalDateTime getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    public String getSatelliteCode() {
        return satelliteCode;
    }

    public void setSatelliteCode(String satelliteCode) {
        this.satelliteCode = satelliteCode;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public Integer getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(Integer checkResult) {
        this.checkResult = checkResult;
    }

    public String getCheckResultName() {
        return checkResultName;
    }

    public void setCheckResultName(String checkResultName) {
        this.checkResultName = checkResultName;
    }

    public Integer getRawLength() {
        return rawLength;
    }

    public void setRawLength(Integer rawLength) {
        this.rawLength = rawLength;
    }

    public String getRawFrame() {
        return rawFrame;
    }

    public void setRawFrame(String rawFrame) {
        this.rawFrame = rawFrame;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }
}
