package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;

/** 本地卫星档案请求，名称和代号均允许重复。 */
public class SatelliteInfoRequest {
    /** 卫星名称。 */
    @NotBlank(message = "卫星名称不能为空")
    private String name;
    /** 卫星代号，仅作为远程调用参数，不作为本地唯一标识。 */
    @NotBlank(message = "卫星代号不能为空")
    private String code;
    /** 英文启用状态，省略时新增默认禁用，编辑保留原状态。 */
    private String enable;
    /** 已存在于本机或共享目录的遥测参数表地址，未配置时允许为空。 */
    private String tablePath;

    /** 获取名称。 */
    public String getName() { return name; }
    /** 设置名称。 */
    public void setName(String name) { this.name = name; }
    /** 获取代号。 */
    public String getCode() { return code; }
    /** 设置代号。 */
    public void setCode(String code) { this.code = code; }
    /** 获取英文状态。 */
    public String getEnable() { return enable; }
    /** 设置英文状态。 */
    public void setEnable(String enable) { this.enable = enable; }
    /** 获取参数表地址。 */
    public String getTablePath() { return tablePath; }
    /** 设置参数表地址。 */
    public void setTablePath(String tablePath) { this.tablePath = tablePath; }
}
