package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/**
 * 数据处理任务表实体。
 *
 * 对应数据库表 data_process_task，用于承载业务数据和MyBatis查询结果。
 */
public class DataProcessTask {
    /** 自增主键。 */
    private Long id;
    /** 试验任务ID。 */
    private String taskId;
    /** 任务名称。 */
    private String taskName;
    /** 采集接口ID，对应collect_interface_config.id。 */
    private Long sourceId;
    /** 已清洗数量。 */
    private Integer processedCount;
    /** 任务状态：0等待中 1运行中 2已完成。 */
    private Integer taskStatus;
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

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Integer getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(Integer processedCount) {
        this.processedCount = processedCount;
    }

    public Integer getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(Integer taskStatus) {
        this.taskStatus = taskStatus;
    }

    /** 返回处理任务状态中文名称。 */
    public String getTaskStatusName() {
        return EnumData.labelOf(BusinessEnums.ProcessTaskStatus.values(), taskStatus);
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
