package com.example.dataadmin.dto.visualization;

/**
 * 新增或修改可视化组件的请求参数。
 */
public class WidgetSaveRequest {

    /** 可视化组件主键 ID；编辑时必填。 */
    private Long id;
    /** 试验任务 ID。 */
    private String taskId;
    /** 组件唯一标识。 */
    private String widgetKey;
    /** 组件类型：1 实时曲线，2 实时数据，3 实时告警，4 载荷图像。 */
    private Integer widgetType;
    /** 页面显示标题。 */
    private String widgetTitle;
    /** 组件左上角横向网格坐标。 */
    private Integer gridX;
    /** 组件左上角纵向网格坐标。 */
    private Integer gridY;
    /** 组件占用网格列数。 */
    private Integer gridWidth;
    /** 组件占用网格行数。 */
    private Integer gridHeight;
    /** 是否显示：0 否，1 是。 */
    private Integer enabled;

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
}
