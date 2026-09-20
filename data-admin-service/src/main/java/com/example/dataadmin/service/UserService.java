package com.example.dataadmin.service;

import com.example.dataadmin.dto.UserSaveRequest;
import com.example.dataadmin.entity.SysUser;

import java.util.List;

/** 系统用户业务接口。 */
public interface UserService {
    /** 查询未删除的用户列表。 */
    List<SysUser> list();

    /** 根据主键查询用户。 */
    SysUser get(Long id);

    /** 创建用户并按需分配角色。 */
    SysUser create(UserSaveRequest request);

    /** 修改用户资料并按需调整角色。 */
    SysUser update(Long id, UserSaveRequest request);

    /** 逻辑删除用户。 */
    void delete(Long id);

    /** 启用或禁用用户。 */
    void updateStatus(Long id, Integer status);

    /** 使用 BCrypt 重置用户密码。 */
    void resetPassword(Long id, String password);
}
