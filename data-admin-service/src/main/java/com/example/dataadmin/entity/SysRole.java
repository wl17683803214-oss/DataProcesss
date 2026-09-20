package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/** 系统角色。 */
public class SysRole {
    private Long id;
    private String roleName;
    private String roleKey;
    /** 基线平台角色编号，本地创建的角色可以为空。 */
    private String baselineRoleId;
    /** 基线平台角色标识，用于展示和同步名称。 */
    private String baselineRoleKey;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    public String getBaselineRoleId() { return baselineRoleId; }
    public void setBaselineRoleId(String baselineRoleId) { this.baselineRoleId = baselineRoleId; }
    public String getBaselineRoleKey() { return baselineRoleKey; }
    public void setBaselineRoleKey(String baselineRoleKey) { this.baselineRoleKey = baselineRoleKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    /** 返回角色状态中文名称。 */
    public String getStatusName() {
        return EnumData.labelOf(BusinessEnums.Enabled.values(), status);
    }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
