package com.example.common.formula;

import java.time.LocalDateTime;

/** 每个公式编号只保存一份脚本，管理服务和处理服务共用存储模型。 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class FormulaScript {
    /** 公式编号。 */
    private String code;
    /** 中文名称。 */
    private String name;
    /** 可再次导出的完整源码。 */
    private String source;
    /** 首次导入时间。 */
    private LocalDateTime createTime;
    /** 最近一次内容更新时间。 */
    private LocalDateTime updateTime;

    /** 读取公式编号。 */
    public String getCode() { return code; }
    /** 设置公式编号。 */
    public void setCode(String code) { this.code = code; }
    /** 读取名称。 */
    public String getName() { return name; }
    /** 设置名称。 */
    public void setName(String name) { this.name = name; }
    /** 读取源码。 */
    public String getSource() { return source; }
    /** 设置源码。 */
    public void setSource(String source) { this.source = source; }
    /** 读取创建时间。 */
    public LocalDateTime getCreateTime() { return createTime; }
    /** 设置创建时间。 */
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    /** 读取更新时间。 */
    public LocalDateTime getUpdateTime() { return updateTime; }
    /** 设置更新时间。 */
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
