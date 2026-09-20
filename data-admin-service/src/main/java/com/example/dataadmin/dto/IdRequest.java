package com.example.dataadmin.dto;

import javax.validation.constraints.NotNull;

/**
 * 仅包含业务主键的请求参数。
 */
public class IdRequest {

    /** 业务数据主键。 */
    @NotNull(message = "业务主键不能为空")
    /** 目标业务记录的主键 ID。 */
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
