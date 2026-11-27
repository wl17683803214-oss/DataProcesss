package com.example.dataadmin.dto.processing;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 保存任务级处理规则开关的请求参数。 */
public class ProcessingRuleConfigRequest {
    @NotNull @Min(0) @Max(1)
    private Integer parameterRangeCheckEnabled;

    /** 兼容既有内部调用，配置及全局记录不再具有任务归属。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getTaskId() { return null; }
    /** 兼容旧调用参数，不接收或保存任务绑定。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setTaskId(String ignored) { }
    public Integer getParameterRangeCheckEnabled() { return parameterRangeCheckEnabled; }
    public void setParameterRangeCheckEnabled(Integer value) { this.parameterRangeCheckEnabled = value; }
}
