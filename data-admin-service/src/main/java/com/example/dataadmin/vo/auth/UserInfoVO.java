package com.example.dataadmin.vo.auth;

import com.example.dataadmin.entity.SysUser;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Set;

/** 当前登录用户及其角色信息。 */
@JsonPropertyOrder({"user", "roles"})
public class UserInfoVO {
    private SysUser user;
    private Set<String> roles;

    public UserInfoVO(SysUser user, Set<String> roles) {
        this.user = user;
        this.roles = roles;
    }

    public SysUser getUser() { return user; }
    public void setUser(SysUser user) { this.user = user; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
