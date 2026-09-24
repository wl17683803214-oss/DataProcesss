package com.example.dataadmin.service.impl;

import com.example.dataadmin.config.SubsystemIntegrationProperties;
import com.example.dataadmin.entity.SysMenu;
import com.example.dataadmin.mapper.SysRoleMapper;
import com.example.dataadmin.service.SubsystemIntegrationService;
import com.example.dataadmin.vo.integration.IntegrationMenuVO;
import com.example.dataadmin.vo.integration.SubsystemMenuConfigVO;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 子系统嵌套集成业务实现。 */
@Service
public class SubsystemIntegrationServiceImpl implements SubsystemIntegrationService {

    private final SubsystemIntegrationProperties properties;
    private final SysRoleMapper roleMapper;

    public SubsystemIntegrationServiceImpl(
            SubsystemIntegrationProperties properties,
            SysRoleMapper roleMapper) {
        this.properties = properties;
        this.roleMapper = roleMapper;
    }

    /** 读取子系统基础配置，并把有效菜单组装成树。 */
    @Override
    public SubsystemMenuConfigVO getMenuConfig(List<String> roleIds) {
        // 入参使用基线平台角色编号，先去除空值和重复值。
        List<String> normalizedRoleIds = normalizeRoleIds(roleIds);
        // 对外菜单中的页面地址以统一处理后的前端根地址为准。
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        // 根据基线平台角色编号查询本地角色菜单关系。
        List<IntegrationMenuVO> menus = buildMenuTree(
                roleMapper.findMenusByBaselineRoleIds(normalizedRoleIds));
        return new SubsystemMenuConfigVO(
                requireText(properties.getSystemKey(), "子系统标识未配置"),
                requireText(properties.getLabel(), "子系统名称未配置"),
                requireText(properties.getIcon(), "子系统图标未配置"),
                baseUrl,
                trimToNull(properties.getDefaultPath()),
                resolveOrigin(baseUrl),
                menus);
    }

    /** 清理基线平台角色编号，并保证查询条件至少包含一个有效编号。 */
    private List<String> normalizeRoleIds(List<String> roleIds) {
        Set<String> values = new LinkedHashSet<String>();
        if (roleIds != null) {
            for (String roleId : roleIds) {
                if (!hasText(roleId)) {
                    continue;
                }
                String[] sections = roleId.split(",");
                for (String section : sections) {
                    if (hasText(section)) {
                        values.add(section.trim());
                    }
                }
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("基线平台角色编号不能为空");
        }
        return new ArrayList<String>(values);
    }

    /** 按父子关系组装菜单树，并保持数据库查询的排序结果。 */
    private List<IntegrationMenuVO> buildMenuTree(List<SysMenu> source) {
        Map<Long, List<SysMenu>> childrenByParent =
                new LinkedHashMap<Long, List<SysMenu>>();
        for (SysMenu menu : source) {
            Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            List<SysMenu> children = childrenByParent.get(parentId);
            if (children == null) {
                children = new ArrayList<SysMenu>();
                childrenByParent.put(parentId, children);
            }
            children.add(menu);
        }
        List<IntegrationMenuVO> result = new ArrayList<IntegrationMenuVO>();
        List<SysMenu> rootMenus = childrenByParent.get(0L);
        if (rootMenus == null) {
            return result;
        }
        for (SysMenu rootMenu : rootMenus) {
            result.add(toMenu(rootMenu, childrenByParent));
        }
        return result;
    }

    /** 递归转换一个菜单节点。 */
    private IntegrationMenuVO toMenu(
            SysMenu menu,
            Map<Long, List<SysMenu>> childrenByParent) {
        List<IntegrationMenuVO> children = null;
        List<SysMenu> childMenus = childrenByParent.get(menu.getId());
        if (childMenus != null && !childMenus.isEmpty()) {
            children = new ArrayList<IntegrationMenuVO>();
            for (SysMenu childMenu : childMenus) {
                children.add(toMenu(childMenu, childrenByParent));
            }
        }
        return new IntegrationMenuVO(
                menu.getLabel(),
                menu.getIcon(),
                menu.getPath(),
                children);
    }

    /** 去掉根地址结尾斜线，保证宿主系统可以直接拼接页面路径。 */
    private String normalizeBaseUrl(String value) {
        String baseUrl = requireText(value, "子系统前端访问地址未配置");
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        resolveOrigin(baseUrl);
        return baseUrl;
    }

    /** 从前端根地址提取协议、主机和端口。 */
    private String resolveOrigin(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            if (!hasText(uri.getScheme()) || !hasText(uri.getHost())) {
                throw new IllegalArgumentException("子系统前端访问地址格式不正确");
            }
            StringBuilder origin = new StringBuilder();
            origin.append(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() >= 0) {
                origin.append(':').append(uri.getPort());
            }
            return origin.toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("子系统前端访问地址格式不正确", exception);
        }
    }

    /** 校验必填配置并清除首尾空格。 */
    private String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
