package com.example.dataadmin.service.impl;

import com.example.dataadmin.dto.role.RoleSaveRequest;
import com.example.dataadmin.entity.SysMenu;
import com.example.dataadmin.entity.SysRole;
import com.example.dataadmin.mapper.SysRoleMapper;
import com.example.dataadmin.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 角色管理业务实现。 */
@Service
public class RoleServiceImpl implements RoleService {
    private final SysRoleMapper mapper;
    public RoleServiceImpl(SysRoleMapper mapper) { this.mapper = mapper; }

    public List<SysRole> list() { return mapper.findAll(); }

    public SysRole get(Long id) {
        SysRole role = mapper.findById(id);
        if (role == null) throw new IllegalArgumentException("角色不存在");
        return role;
    }

    @Transactional
    public SysRole create(RoleSaveRequest request) {
        validate(request, null);
        SysRole role = toRole(request);
        mapper.insert(role);
        return mapper.findById(role.getId());
    }

    @Transactional
    public SysRole update(RoleSaveRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("角色主键不能为空");
        SysRole old = get(request.getId());
        if ("admin".equals(old.getRoleKey()) && !"admin".equals(request.getRoleKey())) {
            throw new IllegalArgumentException("超级管理员角色标识不能修改");
        }
        validate(request, request.getId());
        SysRole role = toRole(request);
        role.setId(request.getId());
        mapper.update(role);
        return mapper.findById(role.getId());
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = get(id);
        if ("admin".equals(role.getRoleKey())) throw new IllegalArgumentException("超级管理员角色不能删除");
        if (mapper.countUsers(id) > 0) throw new IllegalArgumentException("角色已分配用户，不能删除");
        mapper.deleteRoleMenus(id);
        mapper.delete(id);
    }

    /** 修改角色状态属于数据库写操作，异常时由事务统一回滚。 */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) throw new IllegalArgumentException("角色状态只能为0或1");
        SysRole role = get(id);
        if ("admin".equals(role.getRoleKey()) && status == 0) throw new IllegalArgumentException("超级管理员角色不能停用");
        mapper.updateStatus(id, status);
    }

    public List<SysMenu> listMenus() { return mapper.findMenus(); }
    public List<Long> getRoleMenuIds(Long roleId) { get(roleId); return mapper.findMenuIds(roleId); }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = get(roleId);
        if ("admin".equals(role.getRoleKey())) throw new IllegalArgumentException("超级管理员默认拥有全部页面，无需分配");
        mapper.deleteRoleMenus(roleId);
        Set<Long> uniqueIds = new LinkedHashSet<Long>(menuIds == null ? Collections.<Long>emptyList() : menuIds);
        Set<Long> pageIds = new LinkedHashSet<Long>();
        for (Long menuId : uniqueIds) {
            if (menuId == null) {
                continue;
            }
            SysMenu menu = mapper.findMenuById(menuId);
            if (menu == null) {
                throw new IllegalArgumentException("菜单不存在或已删除：" + menuId);
            }
            pageIds.add(menu.getId());
            // 选择子菜单时自动补齐父菜单，保证前端可以生成完整菜单树。
            Long parentId = menu.getParentId();
            while (parentId != null && parentId > 0) {
                SysMenu parent = mapper.findMenuById(parentId);
                if (parent == null) {
                    throw new IllegalArgumentException("父菜单不存在：" + parentId);
                }
                pageIds.add(parent.getId());
                parentId = parent.getParentId();
            }
        }
        for (Long pageId : pageIds) {
            mapper.insertRoleMenu(roleId, pageId);
        }
    }

    private void validate(RoleSaveRequest request, Long currentId) {
        String key = request.getRoleKey().trim();
        SysRole same = mapper.findByKey(key);
        if (same != null && !same.getId().equals(currentId)) throw new IllegalArgumentException("角色标识已存在");
        if (!key.matches("[A-Za-z0-9:_-]+")) throw new IllegalArgumentException("角色标识只能包含字母、数字、冒号、横线和下划线");
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) throw new IllegalArgumentException("角色状态只能为0或1");
    }

    private SysRole toRole(RoleSaveRequest request) {
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName().trim());
        role.setRoleKey(request.getRoleKey().trim());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        return role;
    }
}
