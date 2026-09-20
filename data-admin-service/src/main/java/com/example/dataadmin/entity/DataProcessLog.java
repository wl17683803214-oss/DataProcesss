package com.example.dataadmin.entity;

import com.example.dataadmin.enums.DataProcessLogLevel;

import java.time.LocalDateTime;

/**
 * 数据处理日志实体。
 *
 * 对应 data_process_log 表，只保存数据处理页面展示需要的基本日志信息。
 */
public class DataProcessLog {

    /** 日志主键 ID。 */
    private Long id;

    /** 所属试验任务 ID。 */
    private String taskId;

    /** 所属数据处理任务 ID；无法确定具体任务时可以为空。 */
    private Long processTaskId;

    /** 日志实际发生时间。 */
    private LocalDateTime logTime;

    /** 日志级别枚举编码：1信息，2警告，3错误。 */
    private Integer logLevel;

    /** 页面展示的日志内容。 */
    private String logContent;

    /** 日志写入数据库的时间。 */
    private LocalDateTime createTime;

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

    public Long getProcessTaskId() {
        return processTaskId;
    }

    public void setProcessTaskId(Long processTaskId) {
        this.processTaskId = processTaskId;
    }

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(LocalDateTime logTime) {
        this.logTime = logTime;
    }

    public Integer getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Integer logLevel) {
        this.logLevel = logLevel;
    }

    /** 返回日志级别中文名称，供数据处理日志接口直接展示。 */
    public String getLogLevelName() {
        return DataProcessLogLevel.descriptionOf(logLevel);
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
