package com.example.dataadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 基线平台统一登录和开放接口配置。 */
@Component
@ConfigurationProperties(prefix = "baseline.sso")
public class BaselineSsoProperties {

    /** 基线平台内部接口根地址，可以包含网关前缀。 */
    private String apiBaseUrl;
    /** 卫星列表接口相对路径，由配置文件提供。 */
    private String satelliteListPath;
    /** 设备试验参数表列表接口相对路径，由配置文件提供。 */
    private String trialParamTableListPath;

    /** 返回卫星列表路径。 */
    public String getSatelliteListPath() { return satelliteListPath; }
    /** 保存卫星列表路径。 */
    public void setSatelliteListPath(String path) { this.satelliteListPath = path; }
    /** 返回设备参数表列表路径。 */
    public String getTrialParamTableListPath() { return trialParamTableListPath; }
    /** 保存设备参数表列表路径。 */
    public void setTrialParamTableListPath(String path) { this.trialParamTableListPath = path; }
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
