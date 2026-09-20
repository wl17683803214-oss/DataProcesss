package com.example.dataadmin.service.impl;

import com.example.dataadmin.dto.auth.BaselineIdentity;
import com.example.dataadmin.entity.SysRole;
import com.example.dataadmin.entity.SysUser;
import com.example.dataadmin.mapper.SysRoleMapper;
import com.example.dataadmin.mapper.SysUserMapper;
import com.example.dataadmin.service.SsoUserSyncService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 基线平台用户和角色的本地同步实现。 */
@Service
public class SsoUserSyncServiceImpl implements SsoUserSyncService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE_KEY = "admin";
    private static final String RANDOM_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String RANDOM_PASSWORD_UPPERCASE =
            "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String RANDOM_PASSWORD_LOWERCASE =
            "abcdefghijkmnopqrstuvwxyz";
    private static final String RANDOM_PASSWORD_DIGITS = "23456789";
    private static final int RANDOM_PASSWORD_LENGTH = 8;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public SsoUserSyncServiceImpl(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /** 同步用户和角色，任一写入失败时整体回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser synchronize(BaselineIdentity identity) {
        validateIdentity(identity);
        if (ADMIN_USERNAME.equalsIgnoreCase(identity.getUsername().trim())) {
            return synchronizeAdministrator(identity);
        }

        SysUser user = findOrCreateUser(identity);
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("本地用户已停用");
        }

        // 平台角色以平台角色编号幂等同步，已有页面分配关系保持不变。
        Set<Long> roleIds = new LinkedHashSet<Long>();
        for (BaselineIdentity.BaselineRole platformRole : identity.getRoles()) {
            roleIds.add(findOrCreateRole(platformRole).getId());
        }
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("基线平台用户没有可同步的本地角色");
        }

        // SSO登录以平台当前角色为准，替换该用户原来的全部角色关系。
        userMapper.deleteUserRoles(user.getId());
        for (Long roleId : roleIds) {
            userMapper.insertUserRole(user.getId(), roleId);
        }
        return userMapper.findById(user.getId());
    }

    /** 平台admin固定映射到本地admin并确保超级管理员关系存在。 */
    private SysUser synchronizeAdministrator(BaselineIdentity identity) {
        SysUser administrator = userMapper.findByUsername(ADMIN_USERNAME);
        SysRole administratorRole = roleMapper.findByKey(ADMIN_ROLE_KEY);
        if (administrator == null || administratorRole == null) {
            throw new IllegalStateException("本地管理员账号或超级管理员角色不存在");
        }
        if (!Integer.valueOf(1).equals(administrator.getStatus())
                || !Integer.valueOf(1).equals(administratorRole.getStatus())) {
            throw new IllegalStateException("本地管理员账号或超级管理员角色已停用");
        }

        // 只补充平台身份和显示名称，本地管理员密码保持不变。
        userMapper.updateBaselineIdentity(
                administrator.getId(),
                identity.getUserId(),
                identity.getNickname());
        userMapper.deleteUserRoles(administrator.getId());
        userMapper.insertUserRole(administrator.getId(), administratorRole.getId());
        return userMapper.findById(administrator.getId());
    }

    /** 优先按平台用户编号查询，再按用户名复用本地账号。 */
    private SysUser findOrCreateUser(BaselineIdentity identity) {
        SysUser user = userMapper.findByBaselineUserId(identity.getUserId());
        if (user == null) {
            user = userMapper.findByUsername(identity.getUsername().trim());
        }
        if (user != null) {
            // 同名本地账号已经绑定其他平台用户时拒绝覆盖，避免账号身份被错误接管。
            if (hasText(user.getBaselineUserId())
                    && !identity.getUserId().equals(user.getBaselineUserId())) {
                throw new IllegalArgumentException("该用户名已绑定其他基线平台用户");
            }
            userMapper.updateBaselineIdentity(
                    user.getId(), identity.getUserId(), identity.getNickname());
            return userMapper.findById(user.getId());
        }

        // 新用户生成不可预测的八位初始密码，数据库只保存BCrypt密文。
        SysUser created = new SysUser();
        created.setUsername(identity.getUsername().trim());
        created.setPassword(passwordEncoder.encode(randomPassword()));
        created.setBaselineUserId(identity.getUserId());
        created.setNickname(identity.getNickname());
        created.setStatus(1);
        userMapper.insert(created);
        return userMapper.findById(created.getId());
    }

    /** 平台角色不存在时创建本地角色，已存在时更新展示信息。 */
    private SysRole findOrCreateRole(BaselineIdentity.BaselineRole platformRole) {
        SysRole role = roleMapper.findByBaselineRoleId(platformRole.getRoleId());
        if (role == null) {
            role = new SysRole();
            role.setRoleName(platformRole.getRoleName());
            role.setRoleKey("baseline:" + platformRole.getRoleId());
            role.setBaselineRoleId(platformRole.getRoleId());
            role.setBaselineRoleKey(platformRole.getRoleKey());
            role.setStatus(1);
            roleMapper.insert(role);
        } else {
            role.setRoleName(platformRole.getRoleName());
            role.setBaselineRoleKey(platformRole.getRoleKey());
            roleMapper.update(role);
        }
        if (!Integer.valueOf(1).equals(role.getStatus())) {
            throw new IllegalArgumentException("本地角色已停用：" + role.getRoleName());
        }
        return role;
    }

    /** 生成八位字母和数字随机密码。 */
    private String randomPassword() {
        List<Character> characters = new ArrayList<Character>();
        characters.add(randomCharacter(RANDOM_PASSWORD_UPPERCASE));
        characters.add(randomCharacter(RANDOM_PASSWORD_LOWERCASE));
        characters.add(randomCharacter(RANDOM_PASSWORD_DIGITS));
        while (characters.size() < RANDOM_PASSWORD_LENGTH) {
            characters.add(randomCharacter(RANDOM_PASSWORD_CHARACTERS));
        }
        Collections.shuffle(characters, secureRandom);
        StringBuilder password = new StringBuilder(RANDOM_PASSWORD_LENGTH);
        for (Character character : characters) {
            password.append(character.charValue());
        }
        return password.toString();
    }

    /** 从指定字符集合中随机选择一个字符。 */
    private Character randomCharacter(String characters) {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }

    /** 校验本地同步所需的稳定平台身份字段。 */
    private void validateIdentity(BaselineIdentity identity) {
        if (identity == null
                || !hasText(identity.getUserId())
                || !hasText(identity.getUsername())) {
            throw new IllegalArgumentException("基线平台用户身份不完整");
        }
        if (identity.getRoles() == null) {
            identity.setRoles(new ArrayList<BaselineIdentity.BaselineRole>());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
