package com.example.dataadmin.config;

import com.example.common.greptime.GreptimeProperties;
import com.example.common.greptime.GreptimeStore;
import java.io.IOException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 独立时序连接和自动建表配置，不替代金仓业务数据源。 */
@Configuration
public class GreptimeConfig {
    /** 读取明确配置值，不依赖环境变量占位表达式。 */
    @Bean
    @ConfigurationProperties(prefix = "greptime")
    public GreptimeProperties greptimeProperties() {
        return new GreptimeProperties();
    }

    /** 启用时连接数据库并自动创建缺失表，关闭时释放连接池。 */
    @Bean(destroyMethod = "close")
    public GreptimeStore greptimeStore(GreptimeProperties properties) throws IOException {
        return new GreptimeStore(properties);
    }

    /** 并发查询上限为连接池容量减一，为整帧查询保留连接。 */
    @Bean(destroyMethod = "shutdown")
    public java.util.concurrent.ExecutorService processedTelemetryQueryExecutor(GreptimeProperties properties) {
        return java.util.concurrent.Executors.newFixedThreadPool(properties.getMaxPoolSize() - 1);
    }
}
