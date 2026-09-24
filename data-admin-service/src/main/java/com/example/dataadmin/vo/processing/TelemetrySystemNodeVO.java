package com.example.dataadmin.vo.processing;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/** 所属系统层级查询节点。 */
public class TelemetrySystemNodeVO {
    /** 系统主键。 */
    private Long id;
    /** 系统名称。 */
    private String systemName;
    /** 父系统主键。 */
    private Long parentId;
    /** 兄弟节点排序。 */
    private Integer sortOrder;
    /** 有子系统时才输出子节点字段。 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<TelemetrySystemNodeVO> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String value) { this.systemName = value; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long value) { this.parentId = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { this.sortOrder = value; }
    public List<TelemetrySystemNodeVO> getChildren() { return children; }
}
