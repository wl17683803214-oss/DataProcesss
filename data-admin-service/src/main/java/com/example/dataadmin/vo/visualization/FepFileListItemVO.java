package com.example.dataadmin.vo.visualization;

import java.time.LocalDateTime;

/** 图片与文件组件共用的文件列表记录。 */
public class FepFileListItemVO {
    /** 文件接收记录主键。 */
    private Long id;
    /** 采集接口主键。 */
    private Long interfaceId;
    /** 采集接口名称。 */
    private String interfaceName;
    /** 原始文件名。 */
    private String fileName;
    /** 小写文件后缀。 */
    private String fileType;
    /** 文件字节数。 */
    private Long fileLength;
    /** 对外访问地址。 */
    private String fileUrl;
    /** 文件接收完成时间。 */
    private LocalDateTime completedTime;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long value) { this.interfaceId = value; }
    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String value) { this.interfaceName = value; }
    public String getFileName() { return fileName; }
    public void setFileName(String value) { this.fileName = value; }
    public String getFileType() { return fileType; }
    public void setFileType(String value) { this.fileType = value; }
    public Long getFileLength() { return fileLength; }
    public void setFileLength(Long value) { this.fileLength = value; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String value) { this.fileUrl = value; }
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime value) { this.completedTime = value; }
}
