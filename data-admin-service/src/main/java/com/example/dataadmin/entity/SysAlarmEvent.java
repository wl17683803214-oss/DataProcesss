package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/**
 * 系统告警事件表实体。
 *
 * 对应数据库表 sys_alarm_event，用于承载业务数据和MyBatis查询结果。
 */
public class SysAlarmEvent {
    /** 主键ID。 */
    private Long id;
    /** 告警时间。 */
    private LocalDateTime alarmTime;
    /** 系统对象。 */
    private String objName;
    /** 告警类型。 */
    private String alarmType;
    /** 告警内容。 */
    private String alarmContent;
    /** 告警级别：1提示 2一般 3严重。 */
    private Integer alarmLevel;
    /** 告警状态：0未恢复 1已恢复。 */
    private Integer alarmStatus;
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

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getObjName() {
        return objName;
    }

    public void setObjName(String objName) {
        this.objName = objName;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmContent() {
        return alarmContent;
    }

    public void setAlarmContent(String alarmContent) {
        this.alarmContent = alarmContent;
    }

    public Integer getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(Integer alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    /** 返回系统告警级别中文名称。 */
    public String getAlarmLevelName() {
        return EnumData.labelOf(BusinessEnums.SystemAlarmLevel.values(), alarmLevel);
    }

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    /** 返回系统告警恢复状态中文名称。 */
    public String getAlarmStatusName() {
        return EnumData.labelOf(BusinessEnums.SystemAlarmStatus.values(), alarmStatus);
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
