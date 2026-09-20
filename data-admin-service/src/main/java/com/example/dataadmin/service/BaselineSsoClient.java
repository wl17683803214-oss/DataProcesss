package com.example.dataadmin.service;

import com.example.dataadmin.dto.auth.BaselineIdentity;
import com.example.dataadmin.dto.auth.SsoLoginRequest;

/** 调用基线平台完成令牌、用户和角色查询。 */
public interface BaselineSsoClient {

    /** 校验平台令牌并返回允许访问本系统的用户身份。 */
    BaselineIdentity authenticate(SsoLoginRequest request);
}
