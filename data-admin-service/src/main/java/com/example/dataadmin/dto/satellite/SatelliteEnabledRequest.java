package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 使用本地主键设置卫星启用状态。 */
public class SatelliteEnabledRequest {
    /** 本地主键。 */
    @NotNull(message = "卫星主键不能为空")
    private Long id;
    /** 英文启用状态。 */
    @NotBlank(message = "卫星启用状态不能为空")
    private String enable;

    /** 获取主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
    /** 获取英文状态。 */
    public String getEnable() { return enable; }
    /** 设置英文状态。 */
    public void setEnable(String enable) { this.enable = enable; }
}
