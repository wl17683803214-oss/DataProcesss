package com.example.dataadmin.service;

/** 负责在采集接口配置提交后同步数据处理服务的运行任务。 */
public interface CollectInterfaceRuntimeSyncService {

    /** 事务提交后合并并提交后台同步，调用线程不等待处理服务响应。 */
    void syncAfterCommit();
}
