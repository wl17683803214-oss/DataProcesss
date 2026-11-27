package com.example.dataadmin.dto.processing;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 清除单个页面或组件的全部遥测参数勾选。 */
public class TelemetryFilterClearRequest {
    /** 复用已有勾选场景，表格和曲线分别清除。 */
    @NotNull(message = "参数勾选类型不能为空")
    private Integer selectionType;
    /** 固定页面为零，可视化页面为当前组件主键。 */
    @NotNull(message = "勾选目标主键不能为空")
    private Long targetId;

    /** 获取当前试验任务编号。 */
    /** 兼容内部调用，勾选和布局不再具有任务归属。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getTaskId() { return null; }

    /** 设置当前试验任务编号。 */
    /** 兼容旧参数，不保存任务绑定。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setTaskId(String ignored) { }

    /** 获取参数勾选场景编码。 */
    public Integer getSelectionType() {
        return selectionType;
    }

    /** 设置参数勾选场景编码。 */
    public void setSelectionType(Integer selectionType) {
        this.selectionType = selectionType;
    }

    /** 获取当前页面或组件的目标主键。 */
    public Long getTargetId() {
        return targetId;
    }

    /** 设置当前页面或组件的目标主键。 */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
