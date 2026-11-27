package com.example.dataadmin.entity;

import java.time.LocalDateTime;

/** 数据处理五秒统计查询实体。 */
public class DataProcessingStatistics {

    /** 统计记录主键。 */
    private Long id;
    /** 采集接口主键。 */
    private Long interfaceId;
    /** 五秒统计窗口开始时间。 */
    private LocalDateTime statisticsTime;
    /** 窗口内成功处理数量。 */
    private Long processingCount;
    /** 窗口内被覆盖的重复参数数量。 */
    private Long duplicateCount;
    /** 窗口内野值参数数量。 */
    private Long wildValueCount;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    /** 兼容既有内部调用，配置及全局记录不再具有任务归属。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getTaskId() { return null; }
    /** 兼容旧调用参数，不接收或保存任务绑定。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setTaskId(String ignored) { }
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long value) { interfaceId = value; }
    public LocalDateTime getStatisticsTime() { return statisticsTime; }
    public void setStatisticsTime(LocalDateTime value) { statisticsTime = value; }
    public Long getProcessingCount() { return processingCount; }
    public void setProcessingCount(Long value) { processingCount = value; }
    public Long getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(Long value) { duplicateCount = value; }
    public Long getWildValueCount() { return wildValueCount; }
    public void setWildValueCount(Long value) { wildValueCount = value; }
}
