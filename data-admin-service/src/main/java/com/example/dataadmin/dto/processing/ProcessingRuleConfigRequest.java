package com.example.dataadmin.dto.processing;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 保存任务级处理规则开关的请求参数。 */
public class ProcessingRuleConfigRequest {
    @NotBlank(message = "试验任务编号不能为空")
    private String taskId;
    @NotNull @Min(0) @Max(1)
    private Integer parameterRangeCheckEnabled;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { this.taskId = value; }
    public Integer getParameterRangeCheckEnabled() { return parameterRangeCheckEnabled; }
    public void setParameterRangeCheckEnabled(Integer value) { this.parameterRangeCheckEnabled = value; }
}
