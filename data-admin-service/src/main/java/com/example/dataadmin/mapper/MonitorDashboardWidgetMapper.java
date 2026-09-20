package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.MonitorDashboardWidget;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户试验任务监测页面组件表数据访问接口。
 *
 * SQL统一维护在 MonitorDashboardWidgetMapper.xml 中。
 */
public interface MonitorDashboardWidgetMapper {
    /** 按试验任务ID和用户ID查询页面组件列表。 */
    List<MonitorDashboardWidget> findAllByTaskAndUser(@Param("taskId") String taskId, @Param("userId") Long userId);
    /** 按主键查询单条业务数据。 */
    MonitorDashboardWidget findById(@Param("id") Long id);
    /** 新增业务数据并回填主键。 */
    int insert(MonitorDashboardWidget entity);
    /** 按主键更新业务数据。 */
    int update(MonitorDashboardWidget entity);
    /** 按主键删除业务数据。 */
    int delete(@Param("id") Long id);
}
