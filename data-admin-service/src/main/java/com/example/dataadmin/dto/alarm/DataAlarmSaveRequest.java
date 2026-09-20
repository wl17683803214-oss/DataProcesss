package com.example.dataadmin.dto.alarm;

import java.time.LocalDateTime;

/**
 * 新增或修改数据监测告警的请求参数。
 */
public class DataAlarmSaveRequest {

    /** 告警主键 ID；编辑时必填。 */
    private Long id;
    /** 试验任务 ID。 */
    private String taskId;
    /** 告警触发时间。 */
    private LocalDateTime alarmTime;
    /** 数据来源名称。 */
    private String sourceName;
    /** 告警内容。 */
    private String alarmMsg;
    /** 告警等级：1 提示，2 一般，3 轻微，4 严重，5 紧急。 */
    private Integer alarmLevel;
    /** 处理状态：0 未处理，1 已处理。 */
    private Integer alarmStatus;
    /** 试验任务名称。 */
    private String taskName;
    /** 触发告警的通道编码。 */
    private String channelCode;
    /** 告警触发规则说明。 */
    private String triggerRule;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getAlarmMsg() {
        return alarmMsg;
    }

    public void setAlarmMsg(String alarmMsg) {
        this.alarmMsg = alarmMsg;
    }

    public Integer getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(Integer alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getTriggerRule() {
        return triggerRule;
    }

    public void setTriggerRule(String triggerRule) {
        this.triggerRule = triggerRule;
    }
}
