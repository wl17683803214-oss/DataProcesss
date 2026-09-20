package com.example.dataadmin.service;

import com.example.dataadmin.vo.integration.SubsystemMenuConfigVO;

import java.util.List;

/** 子系统嵌套集成业务接口。 */
public interface SubsystemIntegrationService {

    /** 按基线平台角色编号查询提供给外部宿主系统的菜单配置。 */
    SubsystemMenuConfigVO getMenuConfig(List<String> roleIds);
}
