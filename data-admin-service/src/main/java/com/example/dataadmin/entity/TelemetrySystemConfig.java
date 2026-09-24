package com.example.dataadmin.entity;

/** 导入的所属系统层级节点。 */
public class TelemetrySystemConfig {
    /** 主键。 */
    private Long id;
    /** 试验任务编号。 */
    private String taskId;
    /** 设备卫星主键。 */
    private Long deviceSatelliteId;
    /** 系统名称。 */
    private String systemName;
    /** 父系统主键，零表示根节点。 */
    private Long parentId;
    /** 首次出现顺序。 */
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long deviceSatelliteId) { this.deviceSatelliteId = deviceSatelliteId; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
