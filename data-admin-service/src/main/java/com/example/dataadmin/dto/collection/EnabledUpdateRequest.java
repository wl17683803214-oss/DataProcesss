package com.example.dataadmin.dto.collection;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 启用状态修改参数。
 */
public class EnabledUpdateRequest {

    /** 业务数据主键。 */
    @NotNull(message = "业务主键不能为空")
    /** 目标配置主键 ID。 */
    private Long id;

    /** 启用状态：0 禁用，1 启用。 */
    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态只能为0或1")
    @Max(value = 1, message = "启用状态只能为0或1")
    /** 启用状态：0 禁用，1 启用。 */
    private Integer enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
