package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 遥测通道请求参数。 */
public class TmChannelRequest {

    /** 通道名称。 */
    @NotBlank(message = "通道名称不能为空")
    private String name;
    /** 通道是否启用。 */
    @NotNull(message = "通道启用状态不能为空")
    private Boolean enable;
    /** 十六进制通道代号。 */
    @NotBlank(message = "通道代号不能为空")
    private String channelId;
    /** 通道数据是否需要解扰。 */
    @NotNull(message = "通道解扰状态不能为空")
    private Boolean toDescramble;
    /** 通道同步头。 */
    @NotBlank(message = "通道同步头不能为空")
    private String tmSyncHead;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public Boolean getToDescramble() { return toDescramble; }
    public void setToDescramble(Boolean toDescramble) { this.toDescramble = toDescramble; }
    public String getTmSyncHead() { return tmSyncHead; }
    public void setTmSyncHead(String tmSyncHead) { this.tmSyncHead = tmSyncHead; }
}
