package com.example.dataadmin.dto.satellite;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 卫星整体更新请求参数。 */
public class SatelliteUpdateRequest {

    /** 用于定位原卫星的代号。 */
    @NotBlank(message = "原卫星代号不能为空")
    private String code;
    /** 卫星完整新配置。 */
    @Valid
    @NotNull(message = "卫星完整配置不能为空")
    private SatelliteInfoRequest info;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public SatelliteInfoRequest getInfo() { return info; }
    public void setInfo(SatelliteInfoRequest info) { this.info = info; }
}
