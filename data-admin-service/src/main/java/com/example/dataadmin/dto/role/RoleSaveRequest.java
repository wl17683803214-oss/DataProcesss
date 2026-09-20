package com.example.dataadmin.dto.role;

import javax.validation.constraints.NotBlank;

/** 新增或修改角色请求。 */
public class RoleSaveRequest {
    private Long id;
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
