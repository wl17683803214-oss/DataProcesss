package com.example.dataadmin.dto.satellite;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/** 卫星完整配置请求参数。 */
public class SatelliteInfoRequest {

    /** 卫星名称。 */
    @NotBlank(message = "卫星名称不能为空")
    private String name;
    /** 卫星代号。 */
    @NotBlank(message = "卫星代号不能为空")
    private String code;
    /** 卫星是否启用。 */
    @NotNull(message = "卫星启用状态不能为空")
    private Boolean enable;
    /** 遥测参数表A相对路径。 */
    private String tableA;
    /** 帧结构表B相对路径。 */
    private String tableB;
    /** Lua脚本目录。 */
    private String luaPath;
    /** 遥测帧长。 */
    @NotNull(message = "遥测帧长不能为空")
    @Min(value = 1, message = "遥测帧长必须大于0")
    private Integer tmLen;
    /** 卫星解扰规则。 */
    @Valid
    private DescrambleRuleRequest descrambleRule;
    /** 卫星遥测通道列表。 */
    @Valid
    private List<TmChannelRequest> tmChannels = new ArrayList<TmChannelRequest>();

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
    public DescrambleRuleRequest getDescrambleRule() { return descrambleRule; }
    public void setDescrambleRule(DescrambleRuleRequest descrambleRule) { this.descrambleRule = descrambleRule; }
    public List<TmChannelRequest> getTmChannels() { return tmChannels; }
    public void setTmChannels(List<TmChannelRequest> tmChannels) { this.tmChannels = tmChannels; }
}
