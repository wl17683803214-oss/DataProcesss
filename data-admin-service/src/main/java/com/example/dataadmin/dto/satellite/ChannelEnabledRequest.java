package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 遥测通道启用状态请求参数。 */
public class ChannelEnabledRequest {

    /** 卫星代号。 */
    @NotBlank(message = "卫星代号不能为空")
    private String satelliteCode;
    /** 十六进制通道代号。 */
    @NotBlank(message = "通道代号不能为空")
    private String channelId;
    /** 目标启用状态。 */
    @NotNull(message = "通道启用状态不能为空")
    private Boolean enable;

    public String getSatelliteCode() { return satelliteCode; }
    public void setSatelliteCode(String satelliteCode) { this.satelliteCode = satelliteCode; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
