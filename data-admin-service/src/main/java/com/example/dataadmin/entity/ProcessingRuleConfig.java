package com.example.dataadmin.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** 数据处理页面的任务级参数值范围检查配置。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"parameterRangeCheckEnabled"})
public class ProcessingRuleConfig {
    private String taskId;
    private Integer parameterRangeCheckEnabled;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Integer getParameterRangeCheckEnabled() { return parameterRangeCheckEnabled; }
    public void setParameterRangeCheckEnabled(Integer value) { this.parameterRangeCheckEnabled = value; }
}
