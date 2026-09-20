package com.example.dataadmin.dto.auth;

import javax.validation.constraints.NotBlank;

/** 使用基线平台登录令牌换取本系统登录令牌的请求。 */
public class SsoLoginRequest {

    /** 回调携带的客户端编号；传入时必须与本系统配置一致。 */
    private String clientId;

    /** 基线平台签发的登录令牌。 */
    @NotBlank(message = "基线平台登录令牌不能为空")
    private String tokenId;

    /** 可选的客户端地址，原样传给基线平台校验接口。 */
    private String remoteIp;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String remoteIp) { this.remoteIp = remoteIp; }
}
