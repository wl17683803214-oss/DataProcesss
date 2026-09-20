package com.example.dataprocess.config;

import org.apache.iotdb.session.pool.SessionPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IoTDB 连接配置。
 *
 * <p>启用IoTDB时创建连接池；禁用时不会连接IoTDB服务。</p>
 */
@Configuration
@ConfigurationProperties(prefix = "iotdb")
@ConditionalOnProperty(
        prefix = "iotdb",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class IoTDBConfig {

    private String host = "127.0.0.1";
    private int port = 6667;
    private String username = "root";
    private String password = "root";
    private int maxPoolSize = 5;

    @Bean(destroyMethod = "close")
    public SessionPool iotdbSessionPool() {
        // SessionPool供多个数据处理线程复用，应用停止时由Spring自动关闭。
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
