package com.example.common.appstore;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 由各服务独立配置的应用商店上报参数。 */
@ConfigurationProperties(prefix = "app-store.report")
public class AppStoreReportProperties {

    /** 是否上报日志。 */
    private boolean logEnabled;

    /** 是否定时上报状态。 */
    private boolean statusEnabled;

    /** 同名应用的多个实例是否使用共享缓存协调状态上报。 */
    private boolean statusDeduplicate = true;

    /** 应用商店的协议、主机、端口和接口前缀。 */
    private String scheme = "http";
    private String host = "";
    private int port = 20_001;
    private String pathPrefix = "/app/external";

    /** 当前服务在应用商店登记的名称。 */
    private String appName = "";

    /** 状态上报周期，单位毫秒。 */
    private long statusIntervalMillis = 180_000L;

    /** 连接及响应读取超时，单位毫秒。 */
    private int connectTimeoutMillis = 3_000;
    private int readTimeoutMillis = 5_000;

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    public boolean isStatusEnabled() {
        return statusEnabled;
    }

    public void setStatusEnabled(boolean statusEnabled) {
        this.statusEnabled = statusEnabled;
    }

    public boolean isStatusDeduplicate() {
        return statusDeduplicate;
    }

    public void setStatusDeduplicate(boolean statusDeduplicate) {
        this.statusDeduplicate = statusDeduplicate;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getStatusIntervalMillis() {
        return statusIntervalMillis;
    }

    public void setStatusIntervalMillis(long statusIntervalMillis) {
        this.statusIntervalMillis = statusIntervalMillis;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
