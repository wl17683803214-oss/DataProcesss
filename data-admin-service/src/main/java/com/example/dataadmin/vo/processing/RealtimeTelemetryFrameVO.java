package com.example.dataadmin.vo.processing;

import java.time.LocalDateTime;

/** 实时遥测处理成功帧。 */
public class RealtimeTelemetryFrameVO {

    /** 采集接口主键。 */
    private Long interfaceId;

    /** 遥测帧产生时间。 */
    private LocalDateTime time;

    /** 卫星编码。 */
    private String satelliteCode;

    /** 通道名称。 */
    private String channelName;

    /** 去除PDXP自定义头和遥测头后的原码字节数。 */
    private Integer rawLength;

    /** 大写十六进制格式的遥测数据原码。 */
    private String rawFrame;

    /** 帧检查结果编码。 */
    private Integer checkResult;

    /** 帧检查结果中文名称。 */
    private String checkResultName;

    public Long getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getSatelliteCode() {
        return satelliteCode;
    }

    public void setSatelliteCode(String satelliteCode) {
        this.satelliteCode = satelliteCode;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
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
}
