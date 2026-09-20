package com.example.dataadmin.service;

import com.example.dataadmin.entity.*;

import java.util.List;

/**
 * 数据可视化页面业务接口。
 *
 * 负责组织页面所需的查询和写入操作，数据库访问由对应Mapper完成。
 */
public interface DataVisualizationService {
    /** 查询当前用户在指定试验任务下的全部页面组件。 */
    List<MonitorDashboardWidget> listWidgets(String taskId, Long userId);
    /** 查询页面组件详情。 */
    MonitorDashboardWidget getWidget(Long id);
    /** 新增页面组件。 */
    Long createWidget(MonitorDashboardWidget widget);
    /** 更新页面组件的位置、大小和基本信息。 */
    boolean updateWidget(MonitorDashboardWidget widget);
    /** 删除页面组件及其数据项配置。 */
    boolean deleteWidget(Long id);
    /** 查询页面组件的数据项配置。 */
    List<MonitorWidgetConfigItem> listWidgetItems(Long widgetId);
    /** 查询组件数据项配置详情。 */
    MonitorWidgetConfigItem getWidgetItem(Long id);
    /** 新增组件数据项配置。 */
    Long createWidgetItem(MonitorWidgetConfigItem item);
    /** 更新组件数据项配置。 */
    boolean updateWidgetItem(MonitorWidgetConfigItem item);
    /** 删除组件数据项配置。 */
    boolean deleteWidgetItem(Long id);
    /** 更新组件数据项的勾选状态。 */
    boolean updateItemSelected(Long id, Integer isSelected);
}
