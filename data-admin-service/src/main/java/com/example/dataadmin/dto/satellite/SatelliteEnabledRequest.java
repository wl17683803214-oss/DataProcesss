package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 卫星启用状态请求参数。 */
public class SatelliteEnabledRequest {

    /** 卫星代号。 */
    @NotBlank(message = "卫星代号不能为空")
    private String code;
    /** 目标启用状态。 */
    @NotNull(message = "卫星启用状态不能为空")
    private Boolean enable;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
