package com.example.common.greptime;

/** 两个服务共用的独立时序连接配置。 */
public class GreptimeProperties {
    /** 默认连接本机预置数据库，数据库需要已经存在。 */
    private String url = "jdbc:mysql://127.0.0.1:4002/public?useSSL=false&useServerPrepStmts=false&connectTimeout=10000&socketTimeout=30000&connectionTimeZone=Asia/Shanghai&forceConnectionTimeZoneToSession=true";
    private String username = "";
    private String password = "";
    private int maxPoolSize = 5;
    private int connectionTimeoutMillis = 10000;
    private int queryTimeoutSeconds = 30;
    private int batchSize = 500;
    private int writeAttempts = 3;
    private boolean enabled = true;
    private boolean autoCreateTables = true;

    /** 获取协议连接地址。 */
    public String getUrl() {
        return url;
    }
    /** 设置协议连接地址。 */
    public void setUrl(String url) {
        this.url = url;
    }
    /** 获取连接账号。 */
    public String getUsername() {
        return username;
    }
    /** 设置连接账号。 */
    public void setUsername(String username) {
        this.username = username;
    }
    /** 获取连接密码。 */
    public String getPassword() {
        return password;
    }
    /** 设置连接密码。 */
    public void setPassword(String password) {
        this.password = password;
    }
    /** 获取连接上限。 */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    /** 设置连接上限。 */
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    /** 获取连接等待时间。 */
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }
    /** 设置连接等待时间。 */
    public void setConnectionTimeoutMillis(int value) {
        connectionTimeoutMillis = value;
    }
    /** 获取语句超时。 */
    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }
    /** 设置语句超时。 */
    public void setQueryTimeoutSeconds(int value) {
        queryTimeoutSeconds = value;
    }
    /** 获取每批参数数量。 */
    public int getBatchSize() {
        return batchSize;
    }
    /** 设置每批参数数量。 */
    public void setBatchSize(int value) {
        batchSize = value;
    }
    /** 获取写入尝试次数。 */
    public int getWriteAttempts() {
        return writeAttempts;
    }
    /** 设置写入尝试次数。 */
    public void setWriteAttempts(int value) {
        writeAttempts = value;
    }
    /** 获取启用状态。 */
    public boolean isEnabled() {
        return enabled;
    }
    /** 设置启用状态。 */
    public void setEnabled(boolean value) {
        enabled = value;
    }
    /** 获取启动建表开关。 */
    public boolean isAutoCreateTables() {
        return autoCreateTables;
    }
    /** 设置启动建表开关。 */
    public void setAutoCreateTables(boolean value) {
        autoCreateTables = value;
    }

    /** 启动前拒绝无效连接和批量参数。 */
    public void validate() {
        if (url == null || !url.startsWith("jdbc:mysql:") || maxPoolSize < 2
                || connectionTimeoutMillis < 250 || queryTimeoutSeconds < 1
                || batchSize < 1 || batchSize > 5000 || writeAttempts < 1 || writeAttempts > 5) {
            throw new IllegalArgumentException("时序连接配置无效，连接数至少为2，批量大小为1至5000，写入次数为1至5");
        }
    }
}
