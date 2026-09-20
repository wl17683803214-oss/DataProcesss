package com.example.dataadmin.dto.task;

/** 调度系统下发的试验任务生命周期事件。 */
public class TaskLifecycleEvent {

    /** 生命周期事件类型。 */
    private String eventType;
    /** 关联需求业务编号。 */
    private String requirementId;
    /** 外部试验任务编号。 */
    private String taskId;
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
    /** 执行轮次。 */
    private Integer executionAttempt;
    /** 结束原因。 */
    private String endReason;
    /** 消息基础信息。 */
    private BaseInfo baseInfo;

    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }
    public String getRequirementId() { return requirementId; }
    public void setRequirementId(String value) { requirementId = value; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
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
    public String getEndReason() { return endReason; }
    public void setEndReason(String value) { endReason = value; }
    public BaseInfo getBaseInfo() { return baseInfo; }
    public void setBaseInfo(BaseInfo value) { baseInfo = value; }

    /** 生命周期消息基础信息。 */
    public static class BaseInfo {
        /** 业务名称。 */
        private String businessName;
        /** 发送方编号。 */
        private String senderId;
        /** 发送方名称。 */
        private String senderName;
        /** 接收方编号。 */
        private String receiverId;
        /** 接收方名称。 */
        private String receiverName;
        /** 事件时间。 */
        private String time;

        public String getBusinessName() { return businessName; }
        public void setBusinessName(String value) { businessName = value; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String value) { senderId = value; }
        public String getSenderName() { return senderName; }
        public void setSenderName(String value) { senderName = value; }
        public String getReceiverId() { return receiverId; }
        public void setReceiverId(String value) { receiverId = value; }
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String value) { receiverName = value; }
        public String getTime() { return time; }
        public void setTime(String value) { time = value; }
    }
}
