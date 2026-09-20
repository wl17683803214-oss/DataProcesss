package com.example.dataprocess.entity;

import java.time.LocalDateTime;

/** 数据处理单个五秒窗口的数据库统计记录。 */
public class DataProcessingStatisticsRecord {

    /** 试验任务主键。 */
    private String taskId;
    /** 采集接口主键。 */
    private Long interfaceId;
    /** 五秒统计窗口开始时间。 */
    private LocalDateTime statisticsTime;
    /** 窗口内成功处理的帧或文件数量。 */
    private Long processingCount;
    /** 窗口内被覆盖的重复遥测参数数量。 */
    private Long duplicateCount;
    /** 窗口内判定为野值的遥测参数数量。 */
    private Long wildValueCount;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
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
