package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.DmAlarmEvent;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据监测告警事件表数据访问接口。
 *
 * SQL统一维护在 DmAlarmEventMapper.xml 中。
 */
public interface DmAlarmEventMapper {
    /** 按页面筛选条件查询数据监测告警列表。 */
    List<DmAlarmEvent> findAll(
            @Param("taskId") String taskId,
            @Param("sourceName") String sourceName,
            @Param("keyword") String keyword,
            @Param("alarmLevel") Integer alarmLevel,
            @Param("alarmStatus") Integer alarmStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    /** 按试验任务查询非空告警来源。 */
    List<String> findSourceNames(@Param("taskId") String taskId);
    /** 按主键查询单条业务数据。 */
    DmAlarmEvent findById(@Param("id") Long id);
    /** 新增业务数据并回填主键。 */
    int insert(DmAlarmEvent entity);
    /** 按主键更新业务数据。 */
    int update(DmAlarmEvent entity);
    /** 按主键删除业务数据。 */
    int delete(@Param("id") Long id);
    /** 更新业务状态。 */
    int updateStatus(@Param("id") Long id, @Param("alarmStatus") Integer alarmStatus);
}
