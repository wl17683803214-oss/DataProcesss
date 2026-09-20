package com.example.dataadmin.vo.satellite;

/** 遥测通道返回数据。 */
public class TmChannelVO {

    private String name;
    private Boolean enable;
    private String channelId;
    private Boolean toDescramble;
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
