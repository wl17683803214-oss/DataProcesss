package com.example.dataadmin.controller;

import com.example.dataadmin.service.SubsystemIntegrationService;
import com.example.dataadmin.vo.integration.SubsystemMenuConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 提供子系统嵌套集成所需的公开接口。 */
@RestController
@RequestMapping("/integration")
public class SubsystemIntegrationController {

    private final SubsystemIntegrationService integrationService;

    public SubsystemIntegrationController(
            SubsystemIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /** 根据基线平台角色编号直接返回子系统信息和有权访问的菜单树。 */
    @Operation(summary = "查询子系统菜单配置", security = {})
    @GetMapping("/menu-config")
    public SubsystemMenuConfigVO menuConfig(
            @RequestParam("roleIds") List<String> roleIds) {
        return integrationService.getMenuConfig(roleIds);
    }
}
