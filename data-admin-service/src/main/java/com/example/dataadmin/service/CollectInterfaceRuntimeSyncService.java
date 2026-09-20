package com.example.dataadmin.service;

/** 负责在采集接口配置提交后同步数据处理服务的运行任务。 */
public interface CollectInterfaceRuntimeSyncService {

    /** 注册事务提交后的完整接口列表同步动作。 */
    void syncAfterCommit();
}
