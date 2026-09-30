package com.example.dataadmin.service;

import com.example.dataadmin.entity.MonitorDashboardWidget;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.mapper.MonitorDashboardWidgetMapper;
import com.example.security.context.LoginUserContext;
import org.springframework.stereotype.Service;

/** 校验遥测参数勾选场景和可视化组件归属。 */
@Service
public class TelemetrySelectionScopeService {
    private final MonitorDashboardWidgetMapper widgetMapper;

    public TelemetrySelectionScopeService(MonitorDashboardWidgetMapper widgetMapper) {
        this.widgetMapper = widgetMapper;
    }

    /** 固定页面使用零，组件页面必须匹配任务、用户和组件类型。 */
    public long require(String taskId, Integer selectionType, Long targetId) {
        BusinessEnums.SelectionType type = BusinessEnums.SelectionType.fromCode(selectionType);
        if (type == BusinessEnums.SelectionType.PROCESSED_TABLE
                || type == BusinessEnums.SelectionType.PROCESSED_CURVE) {
            if (targetId == null || targetId != 0L) {
                throw new IllegalArgumentException("处理后数据页面的目标主键必须为零");
            }
            return 0L;
        }
        if (targetId == null || targetId <= 0L) {
            throw new IllegalArgumentException("数据可视化组件主键不能为空");
        }
        MonitorDashboardWidget widget = widgetMapper.findById(targetId);
        Long userId = LoginUserContext.getRequired().getUserId();
        int expectedType = type == BusinessEnums.SelectionType.VISUALIZATION_TABLE ? 2 : 1;
        if (widget == null || !taskId.equals(widget.getTaskId())
                || !userId.equals(widget.getUserId())
                || widget.getWidgetType() == null || widget.getWidgetType() != expectedType) {
            throw new IllegalArgumentException("当前任务下不存在匹配的可视化组件");
        }
        return targetId;
    }
}
