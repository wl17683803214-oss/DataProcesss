package com.example.common.auth;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** Redis 中保存的登录会话，包含当前用户和角色快照。 */
public class LoginUser implements Serializable {

    private Long userId;
    private String username;
    private String tokenId;
    private Set<String> roles = new HashSet<String>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

}
