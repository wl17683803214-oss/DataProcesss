package com.example.dataprocess.config;

import com.example.common.telemetry.TelemetryRealtimeCache;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.dataprocess.service.TelemetryRealtimeCacheWriter;
import com.example.dataprocess.service.TelemetryRealtimePublisher;
import java.io.IOException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 处理服务只创建一个独立的遥测快照写入线程。 */
@Configuration
public class TelemetryRealtimeConfig {
    /** 读取有中文注释的明确配置，不使用环境变量占位表达式。 */
    @Bean
    @ConfigurationProperties(prefix = "telemetry-realtime")
    public TelemetryRealtimeProperties telemetryRealtimeProperties() {
        return new TelemetryRealtimeProperties();
    }

    /** 沿用现有Redis地址和认证配置，不另建连接池。 */
    @Bean
    public TelemetryRealtimeCache telemetryRealtimeCache(StringRedisTemplate redis,
            TelemetryRealtimeProperties properties) throws IOException {
        return new TelemetryRealtimeCache(redis, properties);
    }

    /** 独立转发队列控制Redis发布积压，不占用采集和入库线程。 */
    @Bean(destroyMethod = "close")
    public TelemetryRealtimePublisher telemetryRealtimePublisher(StringRedisTemplate redis,
            TelemetryRealtimeProperties properties) {
        properties.validate();
        return new TelemetryRealtimePublisher(redis, properties);
    }

    /** 有界异步队列由独立工作线程消费，关闭时立即丢弃尚未写入的快照。 */
    @Bean(destroyMethod = "close")
    public TelemetryRealtimeCacheWriter telemetryRealtimeCacheWriter(TelemetryRealtimeCache cache,
            TelemetryRealtimeProperties properties, TelemetryRealtimePublisher publisher) {
        return new TelemetryRealtimeCacheWriter(cache, properties, publisher);
    }
}
