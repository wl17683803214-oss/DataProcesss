package com.example.dataadmin.vo.integration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/** 提供给外部宿主系统的菜单节点。 */
@JsonPropertyOrder({"label", "icon", "path", "children"})
public class IntegrationMenuVO {

    /** 菜单显示名称。 */
    private String label;
    /** 菜单图标。 */
    private String icon;
    /** 菜单页面路径。 */
    private String path;
    /** 子菜单列表。 */
    private List<IntegrationMenuVO> children;

    public IntegrationMenuVO(
            String label,
            String icon,
            String path,
            List<IntegrationMenuVO> children) {
        this.label = label;
        this.icon = icon;
        this.path = path;
        this.children = children;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public List<IntegrationMenuVO> getChildren() { return children; }
    public void setChildren(List<IntegrationMenuVO> children) { this.children = children; }
}
