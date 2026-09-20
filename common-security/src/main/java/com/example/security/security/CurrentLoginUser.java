package com.example.security.security;

import com.example.common.auth.LoginUser;
import com.example.security.context.LoginUserContext;

/** 为业务层提供当前登录用户访问入口。 */
public class CurrentLoginUser {

    public LoginUser get() {
        return LoginUserContext.getRequired();
    }
}
