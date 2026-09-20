package com.example.dataadmin.service;

import com.example.common.auth.LoginUser;
import com.example.dataadmin.dto.LoginRequest;
import com.example.dataadmin.dto.auth.SsoLoginRequest;
import com.example.dataadmin.vo.auth.LoginVO;
import com.example.dataadmin.vo.auth.RouteVO;
import com.example.dataadmin.vo.auth.UserInfoVO;

import java.util.List;

/** 登录认证业务接口。 */
public interface AuthService {
    /** 校验账号密码并创建登录会话。 */
    LoginVO login(LoginRequest request);

    /** 校验基线平台令牌、同步本地账号并创建本系统登录会话。 */
    LoginVO ssoLogin(SsoLoginRequest request);

    /** 删除指定登录会话。 */
    void logout(String tokenId);

    /** 返回当前用户及其角色信息。 */
    UserInfoVO userInfo(LoginUser loginUser);

    /** 返回当前用户可访问的左侧菜单树。 */
    List<RouteVO> routes(LoginUser user);
}
