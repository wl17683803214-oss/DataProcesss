package com.example.dataadmin.config;

import com.example.common.telemetry.TelemetryRealtimeCache;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import java.io.IOException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 管理服务直接读取处理服务写入的共享快照，不创建缓存写入线程。 */
@Configuration
public class TelemetryRealtimeConfig {
    /** 读写双方必须配置相同的前缀和帧窗口。 */
    @Bean
    @ConfigurationProperties(prefix = "telemetry-realtime")
    public TelemetryRealtimeProperties telemetryRealtimeProperties() {
        return new TelemetryRealtimeProperties();
    }

    /** 复用现有Redis连接和公共快照读取实现。 */
    @Bean
    public TelemetryRealtimeCache telemetryRealtimeCache(StringRedisTemplate redis,
            TelemetryRealtimeProperties properties) throws IOException {
        return new TelemetryRealtimeCache(redis, properties);
    }
}
