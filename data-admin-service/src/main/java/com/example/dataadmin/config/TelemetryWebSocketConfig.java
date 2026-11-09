package com.example.dataadmin.config;

import com.example.common.telemetry.TelemetryRealtimeBatch;
import com.example.common.telemetry.TelemetryRealtimeCache;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.dataadmin.service.ProcessedTelemetryQueryService;
import com.example.dataadmin.service.TelemetryWebSocketHandler;
import com.example.security.properties.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

/** 注册原生遥测连接及Redis转发监听，两者与系统监控连接分开。 */
@Configuration
@EnableWebSocket
public class TelemetryWebSocketConfig {
    @Bean(destroyMethod = "close")
    public TelemetryWebSocketHandler telemetryWebSocketHandler(ObjectMapper mapper, WebSocketTokenInterceptor auth,
            ProcessedTelemetryQueryService query, TelemetryRealtimeCache cache, TelemetryRealtimeProperties properties) {
        return new TelemetryWebSocketHandler(mapper, auth, query, cache, properties);
    }

    @Bean
    public WebSocketConfigurer telemetryWebSocketRoutes(TelemetryWebSocketHandler handler, AuthProperties auth) {
        // 复用部署已配置的前端来源与登录令牌校验。
        return registry -> registry.addHandler(handler, "/ws/telemetry")
                .setAllowedOriginPatterns(auth.getAllowedOriginPatterns().toArray(new String[0]));
    }

    @Bean
    public ThreadPoolTaskExecutor telemetryRedisDispatchExecutor(TelemetryRealtimeProperties properties,
            TelemetryWebSocketHandler handler) {
        // 单线程按Redis到达顺序分发，慢浏览器只积压其独立发送队列。
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(properties.getPushQueueCapacity());
        executor.setThreadNamePrefix("遥测实时分发线程-");
        executor.setRejectedExecutionHandler((task, pool) -> handler.signalRedisGap());
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer telemetryRedisListener(RedisConnectionFactory factory,
            TelemetryRealtimeProperties properties, TelemetryWebSocketHandler handler,
            @org.springframework.beans.factory.annotation.Qualifier("telemetryRedisDispatchExecutor")
            ThreadPoolTaskExecutor executor) {
        // 订阅线程单独运行，避免和接收队列共用同一个工作线程。
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.setTaskExecutor(executor);
        container.setSubscriptionExecutor(new org.springframework.core.task.SimpleAsyncTaskExecutor("遥测订阅线程-"));
        container.setErrorHandler(exception -> handler.signalRedisGap());
        if (properties.isEnabled()) {
            container.addMessageListener((message, pattern) -> handler.receive(message.getBody()),
                    new ChannelTopic(properties.getKeyPrefix() + TelemetryRealtimeBatch.CHANNEL_SUFFIX));
        }
        return container;
    }
}
