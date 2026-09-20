package com.example.security.config;

import com.example.security.filter.TokenAuthenticationFilter;
import com.example.security.handler.SecurityExceptionHandler;
import com.example.security.properties.AuthProperties;
import com.example.security.security.CurrentLoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/** 引入 common-security 后自动注册登录认证过滤器。 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityAutoConfiguration {

    /** 注册跨域过滤器，便于前端在移除 Gateway 后直接访问两个业务服务。 */
    @Bean
    public CorsFilter commonSecurityCorsFilter(AuthProperties authProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
                authProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"));
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            CorsFilter commonSecurityCorsFilter) {
        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<CorsFilter>();
        registration.setFilter(commonSecurityCorsFilter);
        registration.setOrder(-200);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenAuthenticationFilter tokenAuthenticationFilter(
            AuthProperties authProperties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        return new TokenAuthenticationFilter(
                authProperties,
                redisTemplate,
                objectMapper);
    }

    @Bean
    public FilterRegistrationBean<TokenAuthenticationFilter> authenticationFilterRegistration(
            TokenAuthenticationFilter authenticationFilter) {
        FilterRegistrationBean<TokenAuthenticationFilter> registration =
                new FilterRegistrationBean<TokenAuthenticationFilter>();
        registration.setFilter(authenticationFilter);
        registration.setOrder(-100);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentLoginUser currentLoginUser() {
        return new CurrentLoginUser();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }
}
