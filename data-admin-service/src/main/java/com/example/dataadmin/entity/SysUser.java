package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 系统用户实体，对应人大金仓 dp_user 表。 */
public class SysUser {

    /** 用户主键 ID。 */
    private Long id;

    /** 登录用户名，系统内唯一。 */
    private String username;

    /** 登录密码的 BCrypt 密文；接口响应中不序列化该字段。 */
    @JsonIgnore
    private String password;

    /** 基线平台用户编号，本地创建的用户可以为空。 */
    private String baselineUserId;

    /** 页面显示使用的用户昵称。 */
    private String nickname;

    /** 联系手机号。 */
    private String phone;

    /** 联系邮箱。 */
    private String email;

    /** 用户状态：1 启用，0 停用。 */
    private Integer status;

    /** 逻辑删除标记：0 未删除，1 已删除。 */
    private Integer deleted;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 最后更新时间。 */
    private LocalDateTime updateTime;

    /** 兼容旧客户端的首个角色主键，按角色编号升序选取。 */
    private Long roleId;

    /** 当前角色名称。 */
    private String roleName;

    /** 当前角色权限标识。 */
    private String roleKey;

    /** 完整角色集合，包含已停用但未删除的绑定，供编辑回显。 */
    private List<SysRole> roles = new ArrayList<SysRole>();

    /** 返回全部角色信息。 */
    public List<SysRole> getRoles() { return roles; }

    /** 保存查询到的角色集合。 */
    public void setRoles(List<SysRole> roles) { this.roles = roles; }

    /** 返回多选控件使用的角色编号，未分配时返回空列表。 */
    public List<Long> getRoleIds() {
        List<Long> ids = new ArrayList<Long>();
        for (SysRole role : roles) {
            ids.add(role.getId());
        }
        return ids;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getBaselineUserId() {
        return baselineUserId;
    }

    public void setBaselineUserId(String baselineUserId) {
        this.baselineUserId = baselineUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /** 返回用户状态中文名称。 */
    public String getStatusName() {
        return EnumData.labelOf(BusinessEnums.Enabled.values(), status);
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getRoleId() { return roles.isEmpty() ? roleId : roles.get(0).getId(); }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roles.isEmpty() ? roleName : roles.get(0).getRoleName(); }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleKey() { return roles.isEmpty() ? roleKey : roles.get(0).getRoleKey(); }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
}
