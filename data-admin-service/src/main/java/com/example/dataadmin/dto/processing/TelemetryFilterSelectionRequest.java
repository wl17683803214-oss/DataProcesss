package com.example.dataadmin.dto.processing;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 单个遥测参数勾选状态更新请求。 */
public class TelemetryFilterSelectionRequest {
    /** 当前试验任务编号。 */
    @NotBlank
    private String taskId;
    /** 当前参数解析配置主键。 */
    @NotNull
    private Long parseRuleId;
    /** 勾选时为真，取消时为假。 */
    @NotNull
    private Boolean checked;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Long getParseRuleId() { return parseRuleId; }
    public void setParseRuleId(Long parseRuleId) { this.parseRuleId = parseRuleId; }
    public Boolean getChecked() { return checked; }
    public void setChecked(Boolean checked) { this.checked = checked; }
}
