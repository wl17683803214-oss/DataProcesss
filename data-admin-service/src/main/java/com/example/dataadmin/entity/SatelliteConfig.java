package com.example.dataadmin.entity;

import java.time.LocalDateTime;

/** 独立于试验任务的本地卫星档案，名称和代号允许重复。 */
public class SatelliteConfig {
    /** 本地主键。 */
    private Long id;
    /** 卫星名称。 */
    private String name;
    /** 卫星代号。 */
    private String code;
    /** 英文启用状态。 */
    private String enable;
    /** 遥测参数表的本地绝对路径。 */
    private String tablePath;
    /** 上传文件中的完整配置文本。 */
    private String jsonContent;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 获取主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
    /** 获取名称。 */
    public String getName() { return name; }
    /** 设置名称。 */
    public void setName(String name) { this.name = name; }
    /** 获取代号。 */
    public String getCode() { return code; }
    /** 设置代号。 */
    public void setCode(String code) { this.code = code; }
    /** 获取英文状态。 */
    public String getEnable() { return enable; }
    /** 设置英文状态。 */
    public void setEnable(String enable) { this.enable = enable; }
    /** 获取参数表地址。 */
    public String getTablePath() { return tablePath; }
    /** 设置参数表地址。 */
    public void setTablePath(String tablePath) { this.tablePath = tablePath; }
    /** 获取配置原文。 */
    public String getJsonContent() { return jsonContent; }
    /** 设置配置原文。 */
    public void setJsonContent(String jsonContent) { this.jsonContent = jsonContent; }
    /** 获取创建时间。 */
    public LocalDateTime getCreateTime() { return createTime; }
    /** 设置创建时间。 */
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    /** 获取更新时间。 */
    public LocalDateTime getUpdateTime() { return updateTime; }
    /** 设置更新时间。 */
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
