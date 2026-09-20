package com.example.dataadmin.dto.role;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/** 给角色分配可以访问的目录和页面。 */
public class RoleMenuRequest {
    @NotNull(message = "角色主键不能为空")
    private Long roleId;
    private List<Long> menuIds = new ArrayList<Long>();

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public List<Long> getMenuIds() { return menuIds; }
    public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }
}
