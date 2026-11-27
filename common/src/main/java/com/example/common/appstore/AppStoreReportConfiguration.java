package com.example.common.appstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 管理服务和处理服务分别引入同一套上报组件。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppStoreReportProperties.class)
public class AppStoreReportConfiguration {

    /** 复用服务已有的序列化组件和共享缓存连接。 */
    @Bean
    public AppStoreReporter appStoreReporter(AppStoreReportProperties properties,
            ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
        return new AppStoreReporter(properties, objectMapper, redisTemplate);
    }
}
