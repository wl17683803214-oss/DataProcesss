package com.example.dataadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 采集接口五秒统计查询实体。 */
public class CollectInterfaceStatistics {

    /** 统计记录主键。 */
    private Long id;
    /** 采集接口主键。 */
    private Long interfaceId;
    /** 五秒统计窗口开始时间。 */
    private LocalDateTime statisticsTime;
    /** 窗口内采集量。 */
    private Long collectionCount;
    /** 窗口内平均接口速率，单位B/s。 */
    private BigDecimal interfaceRate;

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
    public Long getCollectionCount() { return collectionCount; }
    public void setCollectionCount(Long value) { collectionCount = value; }
    public BigDecimal getInterfaceRate() { return interfaceRate; }
    public void setInterfaceRate(BigDecimal value) { interfaceRate = value; }
}
