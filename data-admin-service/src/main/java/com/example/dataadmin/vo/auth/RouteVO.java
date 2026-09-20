package com.example.dataadmin.vo.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/** 左侧菜单节点；叶子菜单没有子节点时不输出 children 字段。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"label", "icon", "path", "hidden", "children"})
public class RouteVO {
    private String label;
    private String icon;
    private String path;
    private Boolean hidden;
    private List<RouteVO> children;

    public RouteVO(String label, String icon, String path,
                   Boolean hidden, List<RouteVO> children) {
        this.label = label;
        this.icon = icon;
        this.path = path;
        this.hidden = hidden;
        this.children = children;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Boolean getHidden() { return hidden; }
    public void setHidden(Boolean hidden) { this.hidden = hidden; }
    public List<RouteVO> getChildren() { return children; }
    public void setChildren(List<RouteVO> children) { this.children = children; }
}
