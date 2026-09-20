package com.example.dataprocess.entity;

import java.time.LocalDateTime;

/** FEP文件从本地接收至消息发布的处理记录。 */
public class FepFileTransferRecord {

    /** 记录主键。 */
    private Long id;
    /** 试验任务编号。 */
    private String taskId;
    /** 采集接口编号。 */
    private Long interfaceId;
    /** 原始文件名。 */
    private String fileName;
    /** 小写文件后缀，不包含点号。 */
    private String fileType;
    /** 文件字节长度。 */
    private Long fileLength;
    /** 本地完整文件路径。 */
    private String localPath;
    /** 对象存储中的对象名称。 */
    private String objectName;
    /** 文件对外访问地址。 */
    private String fileUrl;
    /** 文件处理状态。 */
    private Integer processStatus;
    /** 已经失败的重试次数。 */
    private Integer retryCount;
    /** 最近一次失败原因。 */
    private String lastError;
    /** 下次允许重试的时间。 */
    private LocalDateTime nextRetryTime;
    /** 文件接收完成时间。 */
    private LocalDateTime completedTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileLength() { return fileLength; }
    public void setFileLength(Long fileLength) { this.fileLength = fileLength; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Integer getProcessStatus() { return processStatus; }
    public void setProcessStatus(Integer processStatus) { this.processStatus = processStatus; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
