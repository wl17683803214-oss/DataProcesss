package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.SysUser;
import com.example.dataadmin.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 用户、角色和页面数据访问接口，SQL统一维护在SysUserMapper.xml。 */
public interface SysUserMapper {
    SysUser findByUsername(String username);
    SysUser findByBaselineUserId(String baselineUserId);
    List<SysUser> findAll();
    SysUser findById(Long id);
    int insert(SysUser user);
    int update(SysUser user);
    int delete(Long id);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int resetPassword(@Param("id") Long id,@Param("password") String password);
    List<String> findRoles(Long userId);
    List<com.example.dataadmin.entity.SysMenu> findPageMenusByUserId(Long userId);
    int updateBaselineIdentity(
            @Param("id") Long id,
            @Param("baselineUserId") String baselineUserId,
            @Param("nickname") String nickname);
    int deleteUserRoles(Long userId);
    int insertUserRole(@Param("userId") Long userId,@Param("roleId") Long roleId);
    int countEnabledRole(Long roleId);
    SysRole findRoleByKey(String roleKey);
}
