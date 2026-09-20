package com.example.dataadmin.dto;

/** 用户登录请求参数。 */
public class LoginRequest {

    /** 登录用户名。 */
    private String username;
    /** 登录密码。 */
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
