package com.example.dataadmin.entity;

import com.example.dataadmin.enums.ExperimentTaskStatus;
import com.example.dataadmin.enums.TaskEndReason;

import java.time.LocalDateTime;

/** 试验任务实体，对应调度系统下发的任务生命周期信息。 */
public class ExperimentTask {

    /** 调度系统下发的外部试验任务编号。 */
    private String taskId;
    /** 关联需求业务编号。 */
    private String requirementId;
    /** 试验任务名称。 */
    private String taskName;
    /** 任务优先级。 */
    private Integer taskPriority;
    /** 试验方案内部编号。 */
    private String planId;
    /** 试验方案业务编码。 */
    private String planCode;
    /** 流程实例内部编号。 */
    private String flowInstanceId;
    /** 当前执行轮次。 */
    private Integer executionAttempt;
    /** 任务状态编码。 */
    private Integer taskStatus;
    /** 任务结束原因。 */
    private String endReason;
    /** 最近一次生命周期事件发生时间。 */
    private LocalDateTime eventTime;
    /** 记录创建时间。 */
    private LocalDateTime createTime;
    /** 记录更新时间。 */
    private LocalDateTime updateTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
    public String getRequirementId() { return requirementId; }
    public void setRequirementId(String value) { requirementId = value; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String value) { taskName = value; }
    public Integer getTaskPriority() { return taskPriority; }
    public void setTaskPriority(Integer value) { taskPriority = value; }
    public String getPlanId() { return planId; }
    public void setPlanId(String value) { planId = value; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String value) { planCode = value; }
    public String getFlowInstanceId() { return flowInstanceId; }
    public void setFlowInstanceId(String value) { flowInstanceId = value; }
    public Integer getExecutionAttempt() { return executionAttempt; }
    public void setExecutionAttempt(Integer value) { executionAttempt = value; }
    public Integer getTaskStatus() { return taskStatus; }
    public void setTaskStatus(Integer value) { taskStatus = value; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String value) { endReason = value; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime value) { eventTime = value; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime value) { createTime = value; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime value) { updateTime = value; }

    /** 返回任务状态的中文名称。 */
    public String getTaskStatusName() {
        return ExperimentTaskStatus.labelOf(taskStatus);
    }

    /** 返回任务结束原因的中文名称。 */
    public String getEndReasonName() {
        return TaskEndReason.labelOf(endReason);
    }
}
