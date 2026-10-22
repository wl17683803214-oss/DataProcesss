package com.example.dataadmin.service.monitor;

import com.example.dataadmin.entity.SysMonitorSnapshot;

import java.util.List;

/** 系统健康监控业务接口，统一提供当前状态和历史快照查询。 */
public interface SystemMonitorService {

    /** 定时采集健康状态、缓存快照并推送健康告警变化。 */
    void collectSystemMonitorStatus();

    /** 批量保存暂存快照，事务回滚时恢复待保存数据。 */
    void flushSystemMonitorSnapshots();

    /** 即时采集并返回当前健康状态。 */
    SysMonitorSnapshot getSystemMonitorCurrent();

    /** 查询指定分钟范围内的健康快照。 */
    List<SysMonitorSnapshot> getSystemMonitorHistory(int minutes);
}
