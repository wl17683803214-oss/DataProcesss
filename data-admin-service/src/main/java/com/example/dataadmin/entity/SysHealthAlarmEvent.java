package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/** 系统健康告警实体，对应 sys_health_alarm_event 表。 */
public class SysHealthAlarmEvent {

    /** 告警主键 ID。 */
    private Long id;

    /** 告警触发时间。 */
    private LocalDateTime alarmTime;

    /** 告警对象名称。 */
    private String targetName;

    /** 指标编码。 */
    private String metricCode;

    /** 告警级别：1提示，2一般，3严重。 */
    private Integer alarmLevel;

    /** 告警内容。 */
    private String alarmContent;

    /** 告警状态：0未恢复，1已恢复。 */
    private Integer alarmStatus;

    /** 当前指标值。 */
    private Double currentValue;

    /** 告警阈值。 */
    private Double thresholdValue;

    /** 首次告警时间。 */
    private LocalDateTime firstAlarmTime;

    /** 最近一次异常时间。 */
    private LocalDateTime lastAlarmTime;

    /** 恢复时间；未恢复时为空。 */
    private LocalDateTime recoverTime;

    /** 连续命中次数。 */
    private Integer triggerCount;

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

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public Integer getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(Integer alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    /** 返回系统健康告警级别中文名称。 */
    public String getAlarmLevelName() {
        return EnumData.labelOf(BusinessEnums.SystemAlarmLevel.values(), alarmLevel);
    }

    public String getAlarmContent() {
        return alarmContent;
    }

    public void setAlarmContent(String alarmContent) {
        this.alarmContent = alarmContent;
    }

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    /** 返回系统健康告警恢复状态中文名称。 */
    public String getAlarmStatusName() {
        return EnumData.labelOf(BusinessEnums.SystemAlarmStatus.values(), alarmStatus);
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public Double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public LocalDateTime getFirstAlarmTime() {
        return firstAlarmTime;
    }

    public void setFirstAlarmTime(LocalDateTime firstAlarmTime) {
        this.firstAlarmTime = firstAlarmTime;
    }

    public LocalDateTime getLastAlarmTime() {
        return lastAlarmTime;
    }

    public void setLastAlarmTime(LocalDateTime lastAlarmTime) {
        this.lastAlarmTime = lastAlarmTime;
    }

    public LocalDateTime getRecoverTime() {
        return recoverTime;
    }

    public void setRecoverTime(LocalDateTime recoverTime) {
        this.recoverTime = recoverTime;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
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
