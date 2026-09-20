package com.example.dataadmin.controller;

import com.example.common.auth.LoginUser;
import com.example.common.response.ApiResponse;
import com.example.dataadmin.dto.LoginRequest;
import com.example.dataadmin.dto.auth.SsoLoginRequest;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.service.AuthService;
import com.example.dataadmin.vo.EnumOptionVO;
import com.example.dataadmin.vo.auth.LoginVO;
import com.example.dataadmin.vo.auth.RouteVO;
import com.example.dataadmin.vo.auth.UserInfoVO;
import com.example.security.security.CurrentLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/** 对外提供登录、退出、用户信息和左侧菜单接口。 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentLoginUser currentLoginUser;

    public AuthController(
            AuthService authService,
            CurrentLoginUser currentLoginUser) {
        this.authService = authService;
        this.currentLoginUser = currentLoginUser;
    }

    /** 查询认证模块涉及的枚举选项；当前返回空对象。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.empty());
    }

    /** 用户登录，该接口在后台服务的独立鉴权白名单中。 */
    @Operation(summary = "用户登录", security = {})
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /** 使用经过基线平台校验的身份换取本系统登录令牌。 */
    @Operation(summary = "基线平台统一登录", security = {})
    @PostMapping("/sso/login")
    public ApiResponse<LoginVO> ssoLogin(
            @Valid @RequestBody SsoLoginRequest request) {
        return ApiResponse.success(authService.ssoLogin(request));
    }

    /** 删除当前用户在 Redis 中的登录会话。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        LoginUser loginUser = currentLoginUser.get();
        authService.logout(loginUser.getTokenId());
        return ApiResponse.success();
    }

    /** 查询当前用户资料、角色和权限。 */
    @GetMapping("/user-info")
    public ApiResponse<UserInfoVO> userInfo() {
        LoginUser loginUser = currentLoginUser.get();
        return ApiResponse.success(authService.userInfo(loginUser));
    }

    /** 根据当前角色返回左侧菜单树。 */
    @GetMapping("/routes")
    public ApiResponse<List<RouteVO>> routes() {
        LoginUser loginUser = currentLoginUser.get();
        return ApiResponse.success(authService.routes(loginUser));
    }
}
