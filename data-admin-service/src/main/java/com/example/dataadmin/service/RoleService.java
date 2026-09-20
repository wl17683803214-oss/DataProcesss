package com.example.dataadmin.service;

import com.example.dataadmin.dto.role.RoleSaveRequest;
import com.example.dataadmin.entity.SysMenu;
import com.example.dataadmin.entity.SysRole;
import java.util.List;

/** 角色及页面访问范围管理。 */
public interface RoleService {
    List<SysRole> list();
    SysRole get(Long id);
    SysRole create(RoleSaveRequest request);
    SysRole update(RoleSaveRequest request);
    void delete(Long id);
    void updateStatus(Long id, Integer status);
    List<SysMenu> listMenus();
    List<Long> getRoleMenuIds(Long roleId);
    void assignMenus(Long roleId, List<Long> menuIds);
}
