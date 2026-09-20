package com.example.dataadmin.dto.alarm;

import java.time.LocalDateTime;

/**
 * 新增或修改系统告警的请求参数。
 */
public class SystemAlarmSaveRequest {

    /** 告警主键 ID；编辑时必填。 */
    private Long id;
    /** 告警发生时间。 */
    private LocalDateTime alarmTime;
    /** 发生告警的系统对象名称。 */
    private String objName;
    /** 系统告警类型。 */
    private String alarmType;
    /** 系统告警内容。 */
    private String alarmContent;
    /** 告警等级：1 提示，2 一般，3 严重。 */
    private Integer alarmLevel;
    /** 告警状态：0 未恢复，1 已恢复。 */
    private Integer alarmStatus;

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

    public Integer getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(Integer alarmStatus) {
        this.alarmStatus = alarmStatus;
    }
}
