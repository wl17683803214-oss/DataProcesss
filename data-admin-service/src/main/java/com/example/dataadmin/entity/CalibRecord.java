package com.example.dataadmin.entity;

import java.time.LocalDateTime;

/**
 * 校准记录表实体。
 *
 * 对应数据库表 calib_record，用于承载业务数据和MyBatis查询结果。
 */
public class CalibRecord {
    /** 自增主键ID。 */
    private Long id;
    /** 试验任务ID。 */
    private String taskId;
    /** 校准通道ID。 */
    private Long channelId;
    /** 通道名称。 */
    private String channelName;
    /** 累计检出的野值数量。 */
    private Long outlierCount;
    /** 是否删除：0否 1是。 */
    private Integer isDeleted;
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

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Long getOutlierCount() {
        return outlierCount;
    }

    public void setOutlierCount(Long outlierCount) {
        this.outlierCount = outlierCount;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
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
