package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotNull;

/** 按本地主键操作单条卫星。 */
public class SatelliteIdRequest {
    /** 本地主键。 */
    @NotNull(message = "卫星主键不能为空")
    private Long id;

    /** 获取主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
}
