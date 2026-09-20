package com.example.dataadmin.enums;

/** 系统健康告警在一次监控采集中发生的变化类型。 */
public enum SystemHealthAlarmChangeType {

    /** 新产生一条未恢复告警。 */
    CREATED,

    /** 已存在的未恢复告警持续触发，当前值和触发次数已更新。 */
    UPDATED,

    /** 指标恢复正常，原告警已关闭。 */
    RECOVERED
}
