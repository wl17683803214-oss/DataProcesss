package com.example.security.filter;

import com.example.common.auth.AuthConstants;
import com.example.common.auth.JwtUtils;
import com.example.common.auth.LoginUser;
import com.example.common.response.ApiResponse;
import com.example.security.context.LoginUserContext;
import com.example.security.properties.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 在每个业务服务内部独立验证 JWT 和 Redis 登录会话。 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthProperties authProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TokenAuthenticationFilter(
            AuthProperties authProperties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /** 白名单接口和浏览器预检请求不执行登录校验。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 关闭鉴权开关时，所有请求均不校验登录令牌。
        if (!authProperties.isEnabled()) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String requestPath = request.getRequestURI();
        for (String excludePath : authProperties.getExcludePaths()) {
            if (pathMatcher.match(excludePath, requestPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            LoginUser loginUser = authenticate(request);
            LoginUserContext.set(loginUser);
            filterChain.doFilter(request, response);
        } catch (SecurityException exception) {
            writeUnauthorized(response, exception.getMessage());
        } finally {
            LoginUserContext.clear();
        }
    }

    /** 验证请求令牌，并从 Redis 读取当前登录会话。 */
    private LoginUser authenticate(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)
                || !authorization.startsWith(AuthConstants.TOKEN_PREFIX)) {
            throw new SecurityException("未登录或登录已过期");
        }
        if (!StringUtils.hasText(authProperties.getJwtSecret())) {
            throw new SecurityException("JWT 签名密钥未配置");
        }

        try {
            String token = authorization.substring(AuthConstants.TOKEN_PREFIX.length());
            Claims claims = JwtUtils.parseToken(token, authProperties.getJwtSecret());
            String tokenId = claims.get("tokenId", String.class);
            if (!StringUtils.hasText(tokenId)) {
                throw new SecurityException("登录已过期");
            }

            String loginKey = AuthConstants.LOGIN_KEY_PREFIX + tokenId;
            String loginUserJson = redisTemplate.opsForValue().get(loginKey);
            if (!StringUtils.hasText(loginUserJson)) {
                throw new SecurityException("未登录或登录已过期");
            }
            return objectMapper.readValue(loginUserJson, LoginUser.class);
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecurityException("登录已过期");
        }
    }

    /** 认证失败时直接返回统一的 401 JSON，不再进入业务 Controller。 */
    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.<Void>fail(HttpStatus.UNAUTHORIZED.value(), message));
    }
}
