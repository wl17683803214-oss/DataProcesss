package com.example.dataadmin.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** 数据处理页面的任务级参数值范围检查配置。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"parameterRangeCheckEnabled"})
public class ProcessingRuleConfig {
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
