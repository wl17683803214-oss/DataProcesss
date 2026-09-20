package com.example.security.context;

import com.example.common.auth.LoginUser;

/** 保存当前请求的登录用户，请求结束后必须清理，避免线程复用导致用户信息串号。 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<LoginUser>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前登录用户；不存在有效用户时拒绝继续执行。 */
    public static LoginUser getRequired() {
        LoginUser loginUser = get();
        if (loginUser == null) {
            throw new SecurityException("未登录或登录已过期");
        }
        return loginUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
