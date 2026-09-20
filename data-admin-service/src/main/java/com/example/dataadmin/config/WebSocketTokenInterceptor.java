package com.example.dataadmin.config;

import com.example.common.auth.AuthConstants;
import com.example.common.auth.JwtUtils;
import com.example.common.auth.LoginUser;
import com.example.security.properties.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * /ws/** 由 HTTP 认证过滤器放行，因此必须在 STOMP CONNECT 阶段重新校验 JWT 与 Redis 会话。
 */
@Component
public class WebSocketTokenInterceptor implements ChannelInterceptor {

    private final AuthProperties authProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketTokenInterceptor(
            AuthProperties authProperties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }
        try {
            String token = accessor.getFirstNativeHeader("accessToken");
            if (token == null || token.trim().isEmpty()) {
                throw new SecurityException("缺少 WebSocket 登录令牌");
            }
            if (token.startsWith(AuthConstants.TOKEN_PREFIX)) {
                token = token.substring(AuthConstants.TOKEN_PREFIX.length());
            }
            Claims claims = JwtUtils.parseToken(token, authProperties.getJwtSecret());
            String tokenId = claims.get("tokenId", String.class);
            String session = redisTemplate.opsForValue().get(
                    AuthConstants.LOGIN_KEY_PREFIX + tokenId);
            if (session == null) {
                throw new SecurityException("登录已过期");
            }
            LoginUser user = objectMapper.readValue(session, LoginUser.class);
            accessor.setUser(new LoginUserPrincipal(user.getUsername()));
            return message;
        } catch (Exception exception) {
            throw new SecurityException("登录已过期");
        }
    }

    private static final class LoginUserPrincipal implements Principal {
        private final String username;

        private LoginUserPrincipal(String username) {
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }
    }
}
