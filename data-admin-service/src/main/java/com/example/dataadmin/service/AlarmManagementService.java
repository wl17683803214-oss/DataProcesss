package com.example.dataadmin.service;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警管理页面业务接口。
 *
 * 负责组织页面所需的查询和写入操作，数据库访问由对应Mapper完成。
 */
public interface AlarmManagementService {

    /** 查询系统健康监测的最新状态。 */
    SysMonitorSnapshot getSystemMonitorCurrent();

    /** 查询系统健康监测历史快照。 */
    List<SysMonitorSnapshot> getSystemMonitorHistory(int minutes);

    /** 查询独立于数据监测告警的系统健康告警事件。 */
    List<SysHealthAlarmEvent> getSystemHealthAlarms(Integer status);
    /** 查询系统告警列表。 */
    List<SysAlarmEvent> listSystemAlarms();
    /** 查询系统告警详情。 */
    SysAlarmEvent getSystemAlarm(Long id);
    /** 新增系统告警。 */
    Long createSystemAlarm(SysAlarmEvent alarm);
    /** 更新系统告警。 */
    boolean updateSystemAlarm(SysAlarmEvent alarm);
    /** 删除系统告警。 */
    boolean deleteSystemAlarm(Long id);
    /** 更新系统告警状态。 */
    boolean updateSystemAlarmStatus(Long id, Integer alarmStatus);
    /** 按页面筛选条件分页查询数据监测告警。 */
    PageResult<DmAlarmEvent> pageDataAlarms(
            String taskId,
            String sourceName,
            String keyword,
            Integer alarmLevel,
            Integer alarmStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize);
    /** 查询数据监测告警来源下拉选项。 */
    List<String> listDataAlarmSources(String taskId);
    /** 查询数据监测告警详情。 */
    DmAlarmEvent getDataAlarm(Long id);
    /** 新增数据监测告警。 */
    Long createDataAlarm(DmAlarmEvent alarm);
    /** 更新数据监测告警。 */
    boolean updateDataAlarm(DmAlarmEvent alarm);
    /** 删除数据监测告警。 */
    boolean deleteDataAlarm(Long id);
    /** 更新数据监测告警状态。 */
    boolean updateDataAlarmStatus(Long id, Integer alarmStatus);
}
