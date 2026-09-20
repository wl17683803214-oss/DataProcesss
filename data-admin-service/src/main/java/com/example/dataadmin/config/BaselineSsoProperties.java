package com.example.dataadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 基线平台统一登录和开放接口配置。 */
@Component
@ConfigurationProperties(prefix = "baseline.sso")
public class BaselineSsoProperties {

    /** 基线平台内部接口根地址，可以包含网关前缀。 */
    private String apiBaseUrl;
    /** 基线平台分配给本系统的客户端编号。 */
    private String clientId;
    /** HTTP连接超时时间，单位毫秒。 */
    private int connectTimeoutMillis = 5000;
    /** HTTP响应读取超时时间，单位毫秒。 */
    private int readTimeoutMillis = 10000;

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
}
