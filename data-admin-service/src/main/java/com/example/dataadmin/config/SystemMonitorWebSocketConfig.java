package com.example.dataadmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 系统告警页面 WebSocket 配置。
 * 实时消息同时包含当前系统状态和本轮发生变化的告警；历史趋势及初始告警列表仍通过 HTTP 查询。
 */
@Configuration
@EnableWebSocketMessageBroker
public class SystemMonitorWebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final WebSocketTokenInterceptor tokenInterceptor;

    public SystemMonitorWebSocketConfig(
            WebSocketTokenInterceptor tokenInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/system-monitor")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(tokenInterceptor);
    }
}
