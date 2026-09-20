package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.dto.IdRequest;
import com.example.dataadmin.dto.IdStatusRequest;
import com.example.dataadmin.dto.role.RoleMenuRequest;
import com.example.dataadmin.dto.role.RoleSaveRequest;
import com.example.dataadmin.entity.SysMenu;
import com.example.dataadmin.entity.SysRole;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.service.RoleService;
import com.example.dataadmin.vo.EnumOptionVO;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** 类似若依的角色与菜单权限管理接口。 */
@RestController
@RequestMapping("/system")
public class RoleController {
    private final RoleService roleService;
    public RoleController(RoleService roleService) { this.roleService = roleService; }

    /** 查询角色和菜单页面涉及的全部枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.role());
    }
    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> list() { return ApiResponse.success(roleService.list()); }
    @GetMapping("/roles/{id}")
    public ApiResponse<SysRole> get(@PathVariable Long id) { return ApiResponse.success(roleService.get(id)); }
    @PostMapping("/roles/create")
    public ApiResponse<SysRole> create(@Valid @RequestBody RoleSaveRequest request) { return ApiResponse.success(roleService.create(request)); }
    @PostMapping("/roles/update")
    public ApiResponse<SysRole> update(@Valid @RequestBody RoleSaveRequest request) { return ApiResponse.success(roleService.update(request)); }
    @PostMapping("/roles/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody IdRequest request) { roleService.delete(request.getId()); return ApiResponse.success(); }
    @PostMapping("/roles/status")
    public ApiResponse<Void> status(@Valid @RequestBody IdStatusRequest request) { roleService.updateStatus(request.getId(), request.getStatus()); return ApiResponse.success(); }
    @GetMapping("/menus")
    public ApiResponse<List<SysMenu>> menus() { return ApiResponse.success(roleService.listMenus()); }
    @GetMapping("/roles/{id}/menus")
    public ApiResponse<List<Long>> roleMenus(@PathVariable Long id) { return ApiResponse.success(roleService.getRoleMenuIds(id)); }
    @PostMapping("/roles/menus")
    public ApiResponse<Void> assignMenus(@Valid @RequestBody RoleMenuRequest request) { roleService.assignMenus(request.getRoleId(), request.getMenuIds()); return ApiResponse.success(); }
}
