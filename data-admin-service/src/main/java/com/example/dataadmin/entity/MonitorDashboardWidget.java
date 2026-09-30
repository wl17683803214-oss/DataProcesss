package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/**
 * 用户试验任务监测页面组件表实体。
 *
 * 对应数据库表 monitor_dashboard_widget，用于承载业务数据和MyBatis查询结果。
 */
public class MonitorDashboardWidget {
    /** 组件主键ID。 */
    private Long id;
    /** 试验任务ID。 */
    private String taskId;
    /** 用户ID，对应系统用户ID。 */
    private Long userId;
    /** 组件唯一标识，同一用户同一试验任务内唯一。 */
    private String widgetKey;
    /** 组件类型：1实时曲线 2实时数据 3实时告警 4载荷图像 5文件表格。 */
    private Integer widgetType;
    /** 组件显示标题。 */
    private String widgetTitle;
    /** 组件左上角横向网格位置，从0开始。 */
    private Integer gridX;
    /** 组件左上角纵向网格位置，从0开始。 */
    private Integer gridY;
    /** 组件占用的网格列数。 */
    private Integer gridWidth;
    /** 组件占用的网格行数。 */
    private Integer gridHeight;
    /** 是否显示组件：0否 1是。 */
    private Integer enabled;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getWidgetKey() {
        return widgetKey;
    }

    public void setWidgetKey(String widgetKey) {
        this.widgetKey = widgetKey;
    }

    public Integer getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(Integer widgetType) {
        this.widgetType = widgetType;
    }

    /** 返回组件类型中文名称。 */
    public String getWidgetTypeName() {
        return EnumData.labelOf(BusinessEnums.WidgetType.values(), widgetType);
    }

    public String getWidgetTitle() {
        return widgetTitle;
    }

    public void setWidgetTitle(String widgetTitle) {
        this.widgetTitle = widgetTitle;
    }

    public Integer getGridX() {
        return gridX;
    }

    public void setGridX(Integer gridX) {
        this.gridX = gridX;
    }

    public Integer getGridY() {
        return gridY;
    }

    public void setGridY(Integer gridY) {
        this.gridY = gridY;
    }

    public Integer getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(Integer gridWidth) {
        this.gridWidth = gridWidth;
    }

    public Integer getGridHeight() {
        return gridHeight;
    }

    public void setGridHeight(Integer gridHeight) {
        this.gridHeight = gridHeight;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    /** 返回组件显示状态中文名称。 */
    public String getEnabledName() {
        return EnumData.labelOf(BusinessEnums.Visible.values(), enabled);
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

}
