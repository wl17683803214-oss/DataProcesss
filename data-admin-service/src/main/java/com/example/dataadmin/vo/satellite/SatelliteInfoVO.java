package com.example.dataadmin.vo.satellite;

import java.util.ArrayList;
import java.util.List;

/** 卫星完整配置返回数据。 */
public class SatelliteInfoVO {

    private String name;
    private String code;
    private Boolean enable;
    private String tableA;
    private String tableB;
    private String luaPath;
    private Integer tmLen;
    private DescrambleRuleVO descrambleRule;
    private List<TmChannelVO> tmChannels = new ArrayList<TmChannelVO>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
    public String getTableA() { return tableA; }
    public void setTableA(String tableA) { this.tableA = tableA; }
    public String getTableB() { return tableB; }
    public void setTableB(String tableB) { this.tableB = tableB; }
    public String getLuaPath() { return luaPath; }
    public void setLuaPath(String luaPath) { this.luaPath = luaPath; }
    public Integer getTmLen() { return tmLen; }
    public void setTmLen(Integer tmLen) { this.tmLen = tmLen; }
    public DescrambleRuleVO getDescrambleRule() { return descrambleRule; }
    public void setDescrambleRule(DescrambleRuleVO descrambleRule) { this.descrambleRule = descrambleRule; }
    public List<TmChannelVO> getTmChannels() { return tmChannels; }
    public void setTmChannels(List<TmChannelVO> tmChannels) { this.tmChannels = tmChannels; }
}
