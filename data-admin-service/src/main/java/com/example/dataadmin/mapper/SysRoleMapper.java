package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.SysMenu;
import com.example.dataadmin.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 角色与菜单权限数据访问。 */
public interface SysRoleMapper {
    List<SysRole> findAll();
    SysRole findById(Long id);
    SysRole findByKey(String roleKey);
    SysRole findByBaselineRoleId(String baselineRoleId);
    int insert(SysRole role);
    int update(SysRole role);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int delete(Long id);
    int countUsers(Long roleId);
    List<SysMenu> findMenus();
    List<SysMenu> findMenusByBaselineRoleIds(
            @Param("roleIds") List<String> roleIds);
    SysMenu findMenuById(Long id);
    List<Long> findMenuIds(Long roleId);
    int deleteRoleMenus(Long roleId);
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
