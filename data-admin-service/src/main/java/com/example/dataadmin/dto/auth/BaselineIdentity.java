package com.example.dataadmin.dto.auth;

import java.util.ArrayList;
import java.util.List;

/** 基线平台校验完成后供本地账号同步使用的身份信息。 */
public class BaselineIdentity {

    /** 基线平台用户编号。 */
    private String userId;
    /** 基线平台登录用户名。 */
    private String username;
    /** 基线平台用户显示名称。 */
    private String nickname;
    /** 用户当前拥有且允许访问本系统的角色。 */
    private List<BaselineRole> roles = new ArrayList<BaselineRole>();

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public List<BaselineRole> getRoles() { return roles; }
    public void setRoles(List<BaselineRole> roles) { this.roles = roles; }

    /** 基线平台角色的同步字段。 */
    public static class BaselineRole {
        /** 基线平台角色编号。 */
        private String roleId;
        /** 基线平台角色名称。 */
        private String roleName;
        /** 基线平台角色标识。 */
        private String roleKey;

        public String getRoleId() { return roleId; }
        public void setRoleId(String roleId) { this.roleId = roleId; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getRoleKey() { return roleKey; }
        public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    }
}
