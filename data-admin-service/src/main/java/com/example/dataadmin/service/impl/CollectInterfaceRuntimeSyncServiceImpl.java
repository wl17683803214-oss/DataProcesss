package com.example.dataadmin.service.impl;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.dto.collection.CollectInterfaceSyncRequest;
import com.example.dataadmin.mapper.CollectInterfaceConfigMapper;
import com.example.dataadmin.service.CollectInterfaceRuntimeSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/** 采集接口运行状态同步实现。 */
@Service
public class CollectInterfaceRuntimeSyncServiceImpl
        implements CollectInterfaceRuntimeSyncService {

    /** 采集接口同步日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CollectInterfaceRuntimeSyncServiceImpl.class);

    /** 采集接口配置数据访问组件。 */
    private final CollectInterfaceConfigMapper interfaceMapper;

    /** 数据处理服务采集接口同步地址。 */
    private final String syncUrl;

    /** 内部服务调用使用的公共HTTP工具。 */
    private final HttpRequestTool httpRequestTool;

    public CollectInterfaceRuntimeSyncServiceImpl(
            CollectInterfaceConfigMapper interfaceMapper,
            @Value("${data-processing.interface-sync-url:"
                    + "http://127.0.0.1:8083/internal/collection/interfaces/sync}")
            String syncUrl,
            @Value("${data-processing.interface-sync-connect-timeout-millis:5000}")
            int connectTimeoutMillis,
            @Value("${data-processing.interface-sync-read-timeout-millis:10000}")
            int readTimeoutMillis) {
        this.interfaceMapper = interfaceMapper;
        this.syncUrl = syncUrl;
        // 使用公共工具并按管理服务配置设置内部调用超时时间。
        this.httpRequestTool = new HttpRequestTool(
                connectTimeoutMillis,
                readTimeoutMillis);
    }

    @Override
    public void syncAfterCommit() {
        // 第一步：存在活动事务时等待提交成功，避免处理服务读取到旧配置。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            synchronizeEnabledInterfacesSafely();
                        }
                    });
            return;
        }

        // 第二步：没有事务时直接同步，保证该组件也能独立复用。
        synchronizeEnabledInterfacesSafely();
    }

    /** 查询完整启用列表并通知数据处理服务，失败时保留已提交配置。 */
    private void synchronizeEnabledInterfacesSafely() {
        try {
            // 第一步：数据库启用开关决定数据处理服务最终运行哪些接口。
            List<Long> enabledIds = interfaceMapper.findAllEnabledIds();
            CollectInterfaceSyncRequest request =
                    new CollectInterfaceSyncRequest(enabledIds);

            // 第二步：以JSON格式发送完整列表，列表中不存在的接口由处理服务停止。
            httpRequestTool.postJson(
                    syncUrl,
                    request,
                    Void.class);
            LOGGER.info("采集接口运行列表同步成功，启用接口数量：{}", enabledIds.size());
        } catch (Exception exception) {
            // 同步发生在事务提交后，失败只能记录，不能伪装成数据库回滚。
            LOGGER.error("采集接口运行列表同步失败，原因：{}", exception.getMessage(), exception);
        }
    }
}
