package com.example.dataadmin.service;

import com.example.common.tool.HttpRequestTool;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;

/** 生命周期事务提交后通知处理服务刷新任务缓存，失败交给原有消息消费重试。 */
@Service
public class CurrentExperimentTaskSyncService {
    /** 刷新通知失败日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentExperimentTaskSyncService.class);
    /** 复用内部服务请求工具及超时配置。 */
    private final HttpRequestTool http;
    /** 从已有采集同步地址推导同服务内的任务刷新地址。 */
    private final String refreshUrl;

    /** 复用现有部署地址，避免引入必须同步配置的新连接项。 */
    public CurrentExperimentTaskSyncService(
            @Value("${data-processing.interface-sync-url:http://127.0.0.1:8083/internal/collection/interfaces/sync}") String syncUrl,
            @Value("${data-processing.interface-sync-connect-timeout-millis:5000}") int connectTimeout,
            @Value("${data-processing.interface-sync-read-timeout-millis:10000}") int readTimeout) {
        // 第一步：保留已有服务前缀，只替换本次通知的内部接口路径。
        String suffix = "/internal/collection/interfaces/sync";
        if (!syncUrl.endsWith(suffix)) {
            throw new IllegalArgumentException("采集同步地址必须以内部采集同步路径结尾");
        }
        this.refreshUrl = syncUrl.substring(0, syncUrl.length() - suffix.length())
                + "/internal/collection/tasks/refresh";
        // 第二步：沿用既有超时，不在生命周期事务中执行远程调用。
        this.http = new HttpRequestTool(connectTimeout, readTimeout);
    }

    /** 数据提交成功才通知，通知异常向外传播以触发已有生命周期消息重试。 */
    public void refreshAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 第一步：回滚时不刷新，重复消费会重新查询已提交的数据库最终状态。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifyProcessor();
                }
            });
        } else {
            // 第二步：兼容无外层事务的内部调用。
            notifyProcessor();
        }
    }

    /** 不吞掉通知错误，禁止任务已切换而消息被错误确认成功。 */
    private void notifyProcessor() {
        try {
            JsonNode response = http.postJson(refreshUrl, Collections.emptyMap(), JsonNode.class);
            if (response == null || response.path("code").asInt() != 200) {
                throw new IllegalStateException("处理服务未确认当前试验任务缓存刷新");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("当前试验任务缓存刷新通知失败，等待生命周期消息重试", exception);
            throw exception;
        }
    }
}
