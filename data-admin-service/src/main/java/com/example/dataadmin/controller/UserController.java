package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.dto.IdRequest;
import com.example.dataadmin.dto.IdStatusRequest;
import com.example.dataadmin.dto.ResetPasswordRequest;
import com.example.dataadmin.dto.UserSaveRequest;
import com.example.dataadmin.entity.SysUser;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.service.UserService;
import com.example.dataadmin.vo.EnumOptionVO;
import com.example.security.context.LoginUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/** 系统用户管理接口，每个操作均进行方法级权限校验。 */
@RestController
@RequestMapping("/system/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 查询用户管理页面涉及的全部枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.user());
    }
    @GetMapping
    public ApiResponse<List<SysUser>> list() {
        return ApiResponse.success(userService.list());
    }
    @GetMapping("/{id}")
    public ApiResponse<SysUser> get(@PathVariable Long id) {
        return ApiResponse.success(userService.get(id));
    }
    @PostMapping("/create")
    public ApiResponse<SysUser> create(
            @Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.success(userService.create(request));
    }
    @PostMapping("/update")
    public ApiResponse<SysUser> update(
            @Valid @RequestBody UserSaveRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("用户主键不能为空");
        }
        return ApiResponse.success(userService.update(request.getId(), request));
    }
    @PostMapping("/delete")
    public ApiResponse<Void> delete(
            @Valid @RequestBody IdRequest request) {
        userService.delete(request.getId());
        return ApiResponse.success();
    }

    /** 启用或禁用用户：1启用，0禁用。 */
    @PostMapping("/status")
    public ApiResponse<Void> updateStatus(
            @Valid @RequestBody IdStatusRequest request) {
        if (request.getStatus() == 0
                && request.getId().equals(
                        LoginUserContext.getRequired().getUserId())) {
            throw new IllegalArgumentException("不能禁用当前登录用户");
        }
        userService.updateStatus(request.getId(), request.getStatus());
        return ApiResponse.success();
    }
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getId(), request.getPassword());
        return ApiResponse.success();
    }
}
