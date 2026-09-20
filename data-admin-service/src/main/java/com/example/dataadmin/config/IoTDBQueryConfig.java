package com.example.dataadmin.config;

import org.apache.iotdb.session.pool.SessionPool;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 管理服务查询实时遥测使用的 IoTDB 连接池。 */
@Configuration
@ConfigurationProperties(prefix = "iotdb")
public class IoTDBQueryConfig {

    private String host = "127.0.0.1";
    private int port = 6667;
    private String username = "root";
    private String password = "root";
    private int maxPoolSize = 5;

    @Bean(destroyMethod = "close")
    public SessionPool iotdbQuerySessionPool() {
        return new SessionPool(host, port, username, password, maxPoolSize);
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
}
