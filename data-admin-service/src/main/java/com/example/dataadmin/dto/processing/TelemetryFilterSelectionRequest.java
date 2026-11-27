package com.example.dataadmin.dto.processing;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 单个遥测参数勾选状态更新请求。 */
public class TelemetryFilterSelectionRequest {
    /** 当前参数解析配置主键。 */
    @NotNull
    private Long parseRuleId;
    /** 勾选时为真，取消时为假。 */
    @NotNull
    private Boolean checked;
    /** 勾选场景编码。 */
    @NotNull
    private Integer selectionType;
    /** 固定页面为零，可视化页面为组件主键。 */
    @NotNull
    private Long targetId;

    /** 兼容内部调用，勾选和布局不再具有任务归属。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getTaskId() { return null; }
    /** 兼容旧参数，不保存任务绑定。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setTaskId(String ignored) { }
    public Long getParseRuleId() { return parseRuleId; }
    public void setParseRuleId(Long parseRuleId) { this.parseRuleId = parseRuleId; }
    public Boolean getChecked() { return checked; }
    public void setChecked(Boolean checked) { this.checked = checked; }
    public Integer getSelectionType() { return selectionType; }
    public void setSelectionType(Integer value) { this.selectionType = value; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long value) { this.targetId = value; }
}
