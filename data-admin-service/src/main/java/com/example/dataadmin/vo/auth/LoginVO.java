package com.example.dataadmin.vo.auth;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** 登录成功后返回的令牌信息。 */
@JsonPropertyOrder({"token", "tokenType", "expiresIn"})
public class LoginVO {
    private String token;
    private String tokenType;
    private Long expiresIn;

    public LoginVO(String token, String tokenType, Long expiresIn) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
}
