package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;

/** 卫星代号请求参数。 */
public class SatelliteCodeRequest {

    /** 卫星代号。 */
    @NotBlank(message = "卫星代号不能为空")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
