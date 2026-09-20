package com.example.common.auth;

/** 两个业务服务共用的认证常量。 */
public final class AuthConstants {
    public static final String AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String LOGIN_KEY_PREFIX = "login:token:";

    private AuthConstants() {
    }
}
