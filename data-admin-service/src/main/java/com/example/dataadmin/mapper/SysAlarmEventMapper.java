package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.SysAlarmEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统告警事件表数据访问接口。
 *
 * SQL统一维护在 SysAlarmEventMapper.xml 中。
 */
public interface SysAlarmEventMapper {
    /** 查询系统告警列表。 */
    List<SysAlarmEvent> findAll();
    /** 按主键查询单条业务数据。 */
    SysAlarmEvent findById(@Param("id") Long id);
    /** 新增业务数据并回填主键。 */
    int insert(SysAlarmEvent entity);
    /** 按主键更新业务数据。 */
    int update(SysAlarmEvent entity);
    /** 按主键删除业务数据。 */
    int delete(@Param("id") Long id);
    /** 更新业务状态。 */
    int updateStatus(@Param("id") Long id, @Param("alarmStatus") Integer alarmStatus);
}
