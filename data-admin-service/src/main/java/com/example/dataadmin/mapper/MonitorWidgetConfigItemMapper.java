package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.MonitorWidgetConfigItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 监测页面组件数据项配置表数据访问接口。
 *
 * SQL统一维护在 MonitorWidgetConfigItemMapper.xml 中。
 */
public interface MonitorWidgetConfigItemMapper {
    /** 按页面组件ID查询数据项配置列表。 */
    List<MonitorWidgetConfigItem> findAllByWidgetId(@Param("widgetId") Long widgetId);
    /** 按主键查询单条业务数据。 */
    MonitorWidgetConfigItem findById(@Param("id") Long id);
    /** 新增业务数据并回填主键。 */
    int insert(MonitorWidgetConfigItem entity);
    /** 按主键更新业务数据。 */
    int update(MonitorWidgetConfigItem entity);
    /** 按主键删除业务数据。 */
    int delete(@Param("id") Long id);
    /** 删除指定页面组件的全部数据项配置。 */
    int deleteByWidgetId(@Param("widgetId") Long widgetId);
    /** 更新组件数据项的勾选状态。 */
    int updateSelected(@Param("id") Long id, @Param("isSelected") Integer isSelected);
    /** 删除任务下已经不存在对应解析规则的自动同步筛选项。 */
    int deleteObsoleteParseRuleItems(@Param("taskId") String taskId);
    /** 将已有筛选项重新绑定到当前有效解析规则并更新参数信息。 */
    int bindActiveParseRuleItems(@Param("taskId") String taskId);
    /** 将任务下缺少的解析参数补充到曲线和实时数据组件。 */
    int insertMissingParseRuleItems(@Param("taskId") String taskId);
}
