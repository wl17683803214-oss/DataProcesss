package com.example.dataadmin.service;

import com.example.dataadmin.dto.auth.BaselineIdentity;
import com.example.dataadmin.entity.SysUser;

/** 将基线平台身份同步为本地用户和页面角色。 */
public interface SsoUserSyncService {

    /** 在一个本地事务中同步用户、角色及其绑定关系。 */
    SysUser synchronize(BaselineIdentity identity);
}
