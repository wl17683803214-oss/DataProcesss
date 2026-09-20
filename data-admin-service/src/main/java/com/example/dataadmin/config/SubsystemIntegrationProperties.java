package com.example.dataadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 子系统嵌套集成配置。 */
@Component
@ConfigurationProperties(prefix = "subsystem.integration")
public class SubsystemIntegrationProperties {

    /** 子系统唯一标识。 */
    private String systemKey = "data-processing";
    /** 子系统显示名称。 */
    private String label = "数据处理软件";
    /** 子系统图标。 */
    private String icon = "system";
    /** 子系统前端访问根地址。 */
    private String baseUrl;
    /** 子系统默认页面路径。 */
    private String defaultPath = "/dashboard";

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
}
