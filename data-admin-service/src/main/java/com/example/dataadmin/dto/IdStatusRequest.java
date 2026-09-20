package com.example.dataadmin.dto;

import javax.validation.constraints.NotNull;

/**
 * 业务主键和目标状态请求参数。
 */
public class IdStatusRequest {

    /** 业务数据主键。 */
    @NotNull(message = "业务主键不能为空")
    /** 目标业务记录的主键 ID。 */
    private Long id;

    /** 目标业务状态。 */
    @NotNull(message = "状态不能为空")
    /** 要更新的状态值。 */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
