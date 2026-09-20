package com.example.dataadmin.vo.alarm;

import com.example.dataadmin.entity.SysHealthAlarmEvent;
import com.example.dataadmin.enums.SystemHealthAlarmChangeType;

/** WebSocket 推送的单条系统健康告警变化。 */
public class SystemHealthAlarmChangeVO {

    /** 告警变化类型枚举：CREATED、UPDATED、RECOVERED。 */
    private SystemHealthAlarmChangeType changeType;

    /** 发生变化后的完整告警数据。 */
    private SysHealthAlarmEvent alarmEvent;

    public SystemHealthAlarmChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(SystemHealthAlarmChangeType changeType) {
        this.changeType = changeType;
    }

    public SysHealthAlarmEvent getAlarmEvent() {
        return alarmEvent;
    }

    public void setAlarmEvent(SysHealthAlarmEvent alarmEvent) {
        this.alarmEvent = alarmEvent;
    }
}
