package com.example.dataadmin.entity;

/** DeviceSatellite对应的业务数据。 */
public class DeviceSatellite {
    /** 主键。 */
    private Long id;
    /** 试验任务ID。 */
    private String taskId;
    /** 设备卫星编码。 */
    private String code;
    /** 设备卫星名称。 */
    private String name;
    /** 类型：1卫星，2设备。 */
    private String type;
    /** 是否删除：0否，1是。 */
    private Integer isDeleted;
    /** 创建时间。 */
    private java.time.LocalDateTime createTime;
    /** 更新时间。 */
    private java.time.LocalDateTime updateTime;

    /** 获取主键。 */
    public Long getId() {
        return id;
    }

    /** 设置主键。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取试验任务ID。 */
    public String getTaskId() {
        return taskId;
    }

    /** 设置试验任务ID。 */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /** 获取设备卫星编码。 */
    public String getCode() {
        return code;
    }

    /** 设置设备卫星编码。 */
    public void setCode(String code) {
        this.code = code;
    }

    /** 获取设备卫星名称。 */
    public String getName() {
        return name;
    }

    /** 设置设备卫星名称。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 获取类型：1卫星，2设备。 */
    public String getType() {
        return type;
    }

    /** 设置类型：1卫星，2设备。 */
    public void setType(String type) {
        this.type = type;
    }

    /** 获取是否删除：0否，1是。 */
    public Integer getIsDeleted() {
        return isDeleted;
    }

    /** 设置是否删除：0否，1是。 */
    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    /** 获取创建时间。 */
    public java.time.LocalDateTime getCreateTime() {
        return createTime;
    }

    /** 设置创建时间。 */
    public void setCreateTime(java.time.LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /** 获取更新时间。 */
    public java.time.LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /** 设置更新时间。 */
    public void setUpdateTime(java.time.LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
