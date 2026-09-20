package com.example.dataadmin.service.impl;

import com.example.dataadmin.entity.*;
import com.example.dataadmin.mapper.*;
import com.example.dataadmin.service.DataVisualizationService;
import com.example.dataadmin.service.VisualizationParameterSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据可视化页面业务实现。
 *
 * 查询默认使用只读事务，新增、修改和删除操作单独开启写事务。
 */
@Service
@Transactional(readOnly = true)
public class DataVisualizationServiceImpl implements DataVisualizationService {

    /** MonitorDashboardWidget数据访问组件。 */
    private final MonitorDashboardWidgetMapper widgetMapper;
    /** MonitorWidgetConfigItem数据访问组件。 */
    private final MonitorWidgetConfigItemMapper itemMapper;
    /** 可视化参数筛选项同步服务。 */
    private final VisualizationParameterSyncService parameterSyncService;

    public DataVisualizationServiceImpl(
            MonitorDashboardWidgetMapper widgetMapper,
            MonitorWidgetConfigItemMapper itemMapper,
            VisualizationParameterSyncService parameterSyncService) {
        this.widgetMapper = widgetMapper;
        this.itemMapper = itemMapper;
        this.parameterSyncService = parameterSyncService;
    }

    /** 查询当前用户在指定试验任务下的全部页面组件。 */
    @Override
    public List<MonitorDashboardWidget> listWidgets(String taskId, Long userId) {
        return widgetMapper.findAllByTaskAndUser(taskId, userId);
    }

    /** 查询页面组件详情。 */
    @Override
    public MonitorDashboardWidget getWidget(Long id) {
        return widgetMapper.findById(id);
    }

    /** 新增页面组件。 */
    @Override
    @Transactional
    public Long createWidget(MonitorDashboardWidget widget) {
        widgetMapper.insert(widget);
        if (widget.getWidgetType() != null
                && (widget.getWidgetType() == 1
                || widget.getWidgetType() == 2)) {
            // 组件晚于解析规则创建时，也要立即装载任务已有参数。
            parameterSyncService.synchronizeTaskParameters(
                    widget.getTaskId());
        }
        return widget.getId();
    }

    /** 更新页面组件的位置、大小和基本信息。 */
    @Override
    @Transactional
    public boolean updateWidget(MonitorDashboardWidget widget) {
        return widgetMapper.update(widget) > 0;
    }

    /** 删除页面组件及其数据项配置。 */
    @Override
    @Transactional
    public boolean deleteWidget(Long id) {
        // 先清理组件下的数据项，避免残留无法归属的配置记录。
        itemMapper.deleteByWidgetId(id);
        return widgetMapper.delete(id) > 0;
    }

    /** 查询页面组件的数据项配置。 */
    @Override
    public List<MonitorWidgetConfigItem> listWidgetItems(Long widgetId) {
        return itemMapper.findAllByWidgetId(widgetId);
    }

    /** 查询组件数据项配置详情。 */
    @Override
    public MonitorWidgetConfigItem getWidgetItem(Long id) {
        return itemMapper.findById(id);
    }

    /** 新增组件数据项配置。 */
    @Override
    @Transactional
    public Long createWidgetItem(MonitorWidgetConfigItem item) {
        itemMapper.insert(item);
        return item.getId();
    }

    /** 更新组件数据项配置。 */
    @Override
    @Transactional
    public boolean updateWidgetItem(MonitorWidgetConfigItem item) {
        return itemMapper.update(item) > 0;
    }

    /** 删除组件数据项配置。 */
    @Override
    @Transactional
    public boolean deleteWidgetItem(Long id) {
        return itemMapper.delete(id) > 0;
    }

    /** 更新组件数据项的勾选状态。 */
    @Override
    @Transactional
    public boolean updateItemSelected(Long id, Integer isSelected) {
        return itemMapper.updateSelected(id, isSelected) > 0;
    }

}
