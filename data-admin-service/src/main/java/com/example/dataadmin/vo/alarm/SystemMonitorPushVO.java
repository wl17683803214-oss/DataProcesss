package com.example.dataadmin.vo.alarm;

import com.example.dataadmin.entity.SysMonitorSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * 系统告警页面的统一 WebSocket 消息。
 *
 * 每 5 秒推送一次系统状态，并携带本轮新增、更新或恢复的告警事件。
 */
public class SystemMonitorPushVO {

    /** 本轮采集的最新系统状态。 */
    private SysMonitorSnapshot snapshot;

    /** 本轮发生变化的告警事件；没有变化时返回空数组。 */
    private List<SystemHealthAlarmChangeVO> alarmEvents =
            Collections.<SystemHealthAlarmChangeVO>emptyList();

    public SysMonitorSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SysMonitorSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public List<SystemHealthAlarmChangeVO> getAlarmEvents() {
        return alarmEvents;
    }

    public void setAlarmEvents(List<SystemHealthAlarmChangeVO> alarmEvents) {
        this.alarmEvents = alarmEvents == null
                ? Collections.<SystemHealthAlarmChangeVO>emptyList()
                : alarmEvents;
    }
}
