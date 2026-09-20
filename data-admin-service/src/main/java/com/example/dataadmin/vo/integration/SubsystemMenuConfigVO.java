package com.example.dataadmin.vo.integration;

import com.example.dataadmin.vo.auth.RouteVO;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/** 提供给外部宿主系统的子系统菜单配置。 */
@JsonPropertyOrder({"systemKey", "label", "icon", "baseUrl", "defaultPath", "origin", "menus"})
public class SubsystemMenuConfigVO {
    private String systemKey;
    private String label;
    private String icon;
    private String baseUrl;
    private String defaultPath;
    private String origin;
    private List<RouteVO> menus;

    public SubsystemMenuConfigVO(
            String systemKey,
            String label,
            String icon,
            String baseUrl,
            String defaultPath,
            String origin,
            List<RouteVO> menus) {
        this.systemKey = systemKey;
        this.label = label;
        this.icon = icon;
        this.baseUrl = baseUrl;
        this.defaultPath = defaultPath;
        this.origin = origin;
        this.menus = menus;
    }

    public String getSystemKey() { return systemKey; }
    public void setSystemKey(String systemKey) { this.systemKey = systemKey; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getDefaultPath() { return defaultPath; }
    public void setDefaultPath(String defaultPath) { this.defaultPath = defaultPath; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public List<RouteVO> getMenus() { return menus; }
    public void setMenus(List<RouteVO> menus) { this.menus = menus; }
}
