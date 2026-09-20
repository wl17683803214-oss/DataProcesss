package com.example.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 各服务独立鉴权所需的配置项。 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 是否启用登录与权限校验，默认启用。 */
    private boolean enabled = true;
    private String jwtSecret;
    private List<String> excludePaths = new ArrayList<String>();
    private List<String> allowedOriginPatterns = new ArrayList<String>(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
