package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/**
 * 数据监测告警事件表实体。
 *
 * 对应数据库表 dm_alarm_event，用于承载业务数据和MyBatis查询结果。
 */
public class DmAlarmEvent {
    /** 主键ID。 */
    private Long id;
    /** 试验任务ID。 */
    private String taskId;
    /** 告警时间。 */
    private LocalDateTime alarmTime;
    /** 告警来源。 */
    private String sourceName;
    /** 告警信息。 */
    private String alarmMsg;
    /** 告警级别：1提示 2一般 3轻微 4严重 5紧急。 */
    private Integer alarmLevel;
    /** 处理状态：0未处理 1已处理。 */
    private Integer alarmStatus;
    /** 任务名称（历史快照）。 */
    private String taskName;
    /** 通道编码。 */
    private String channelCode;
    /** 触发规则。 */
    private String triggerRule;
    /** 是否删除：0否 1是。 */
    private Integer isDeleted;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

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

    /** 返回数据告警级别中文名称。 */
    public String getAlarmLevelName() {
        return EnumData.labelOf(BusinessEnums.DataAlarmLevel.values(), alarmLevel);
    }

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    /** 返回数据告警处理状态中文名称。 */
    public String getAlarmStatusName() {
        return EnumData.labelOf(BusinessEnums.DataAlarmStatus.values(), alarmStatus);
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

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

}
