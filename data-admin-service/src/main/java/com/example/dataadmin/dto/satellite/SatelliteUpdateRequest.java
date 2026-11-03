package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotNull;

/** 使用本地主键定位编辑记录，不通过可能重复的卫星代号定位。 */
public class SatelliteUpdateRequest extends SatelliteInfoRequest {
    /** 本地卫星主键。 */
    @NotNull(message = "卫星主键不能为空")
    private Long id;

    /** 获取主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
}
