package com.example.dataadmin.service.impl;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.dto.collection.CollectInterfaceSyncRequest;
import com.example.dataadmin.mapper.CollectInterfaceConfigMapper;
import com.example.dataadmin.entity.CollectInterfaceConfig;
import com.example.dataadmin.enums.SystemLogLevel;
import com.example.dataadmin.enums.SystemLogSource;
import com.example.dataadmin.service.CollectSyncAlarmRecorder;
import com.example.dataadmin.service.CollectInterfaceRuntimeSyncService;
import com.example.dataadmin.service.OperationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;

/** 采集接口运行状态同步实现。 */
@Service
public class CollectInterfaceRuntimeSyncServiceImpl
        implements CollectInterfaceRuntimeSyncService {

    /** 内部同步异常同时进入数据采集告警，日志与告警独立表达操作与影响。 */
    private final CollectSyncAlarmRecorder alarmRecorder;
    /** 后台结果复用已有运行日志与独立事务写入组件。 */
    private final OperationLogService operationLogs;
    /** 采集接口同步日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CollectInterfaceRuntimeSyncServiceImpl.class);

    /** 采集接口配置数据访问组件。 */
    private final CollectInterfaceConfigMapper interfaceMapper;

    /** 数据处理服务采集接口同步地址。 */
    private final String syncUrl;

    /** 内部服务调用使用的公共HTTP工具。 */
    private final HttpRequestTool httpRequestTool;
    /** 专用串行执行器，接口请求不执行远程同步。 */
    private final Executor syncExecutor;
    /** 多次配置提交合并为一次待执行的最新配置同步。 */
    private final AtomicBoolean syncRequested = new AtomicBoolean();
    /** 始终最多只有一个排队或运行中的同步工作任务。 */
    private final AtomicBoolean workerScheduled = new AtomicBoolean();
    /** 关闭时不再接受后续同步请求。 */
    private volatile boolean stopping;

    public CollectInterfaceRuntimeSyncServiceImpl(
            CollectInterfaceConfigMapper interfaceMapper,
            @Value("${data-processing.interface-sync-url:"
                    + "http://127.0.0.1:8083/internal/collection/interfaces/sync}")
            String syncUrl,
            @Value("${data-processing.interface-sync-connect-timeout-millis:5000}")
            int connectTimeoutMillis,
            @Value("${data-processing.interface-sync-read-timeout-millis:10000}")
            int readTimeoutMillis,
            @Qualifier("collectInterfaceSyncExecutor") Executor syncExecutor,
            CollectSyncAlarmRecorder alarmRecorder,
            OperationLogService operationLogs) {
        this.interfaceMapper = interfaceMapper;
        this.syncUrl = syncUrl;
        this.syncExecutor = syncExecutor;
        this.alarmRecorder = alarmRecorder;
        this.operationLogs = operationLogs;
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
                            requestBackgroundSync();
                        }
                    });
            return;
        }

        // 第二步：没有事务时同样只提交后台任务，不在调用线程等待网络。
        requestBackgroundSync();
    }

    /** 提交轻量同步标记，人工操作日志只说明配置已保存并已排队。 */
    private void requestBackgroundSync() {
        if (stopping) {
            setOperationResult("配置已保存，服务正在关闭，未提交后台同步", true);
            LOGGER.warn("管理服务正在关闭，采集接口配置已保存，未提交后台同步");
            return;
        }
        // 同步执行时再读取完整列表，避免排队期间的旧启用状态覆盖最新禁用状态。
        syncRequested.set(true);
        try {
            scheduleWorkerIfNeeded();
            setOperationResult("配置已保存，已提交后台同步，请通过运行日志查看执行结果", false);
        } catch (RejectedExecutionException exception) {
            setOperationResult("配置已保存，后台同步任务提交失败，请查看服务日志", true);
            LOGGER.error("采集接口后台同步任务提交失败，配置已保存，等待后续同步或服务重启恢复", exception);
        }
    }

    /** 合并连续操作，只提交一个后台工作任务。 */
    private void scheduleWorkerIfNeeded() {
        if (workerScheduled.compareAndSet(false, true)) {
            try {
                syncExecutor.execute(this::drainSyncRequests);
            } catch (RejectedExecutionException exception) {
                workerScheduled.set(false);
                throw exception;
            }
        }
    }

    /** 串行同步最新配置，执行期间有新提交时再读取一次最新列表。 */
    private void drainSyncRequests() {
        try {
            while (!stopping && syncRequested.getAndSet(false)) {
                synchronizeEnabledInterfacesSafely();
            }
        } finally {
            // 释放标记后再次检查，补上工作任务退出瞬间到达的配置提交。
            workerScheduled.set(false);
            if (!stopping && syncRequested.get()) {
                try {
                    scheduleWorkerIfNeeded();
                } catch (RejectedExecutionException exception) {
                    LOGGER.error("采集接口后续后台同步任务提交失败，最新配置同步标记保留", exception);
                }
            }
        }
    }

    /** 查询完整启用列表并通知数据处理服务，失败时保留已提交配置。 */
    private void synchronizeEnabledInterfacesSafely() {
        long started = System.nanoTime();
        List<Long> enabledIds = new ArrayList<Long>();
        try {
            // 第一步：数据库启用开关决定数据处理服务最终运行哪些接口。
            enabledIds = interfaceMapper.findAllEnabledIds();
            CollectInterfaceSyncRequest request =
                    new CollectInterfaceSyncRequest(enabledIds);

            // 第二步：以JSON格式发送完整列表，列表中不存在的接口由处理服务停止。
            JsonNode response = httpRequestTool.postJson(syncUrl, request, JsonNode.class);
            if (response == null || response.path("code").asInt() != 200 || !response.path("data").isArray()) {
                throw new IllegalStateException("处理服务未返回有效运行接口列表");
            }
            List<Long> runningIds = new ArrayList<Long>();
            for (JsonNode id : response.path("data")) { runningIds.add(id.asLong()); }
            List<Long> failedIds = new ArrayList<Long>(enabledIds);
            failedIds.removeAll(runningIds);
            String detail = "已启动接口ID：" + runningIds + "；已启用但未启动接口ID：" + failedIds
                    + "；同步耗时：" + elapsedMillis(started) + "毫秒";
            recordBackgroundResult(enabledIds, failedIds, detail, null);
            LOGGER.info("采集接口后台同步完成，启用接口数量：{}，未启动接口数量：{}，耗时：{}毫秒",
                    enabledIds.size(), failedIds.size(), elapsedMillis(started));
        } catch (Exception exception) {
            String detail = "配置已保存，后台运行状态同步失败：" + exception.getMessage()
                    + "；同步耗时：" + elapsedMillis(started) + "毫秒";
            recordBackgroundResult(enabledIds, enabledIds, detail, exception);
            LOGGER.error("采集接口后台同步失败，耗时：{}毫秒，原因：{}",
                    elapsedMillis(started), exception.getMessage(), exception);
        }
    }

    /** 后台结果独立记录，并只恢复实际启动成功接口的同步告警。 */
    private void recordBackgroundResult(List<Long> enabledIds, List<Long> failedIds,
            String detail, Exception failure) {
        Map<String, List<Long>> idsByTask = new LinkedHashMap<String, List<Long>>();
        for (Long id : enabledIds) {
            try {
                CollectInterfaceConfig config = interfaceMapper.findById(id);
                // 已被后续操作禁用或删除的接口不再根据旧请求新增失败告警。
                if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
                    continue;
                }
                idsByTask.computeIfAbsent(config.getTaskId(), ignored -> new ArrayList<Long>()).add(id);
                if (failedIds.contains(id)) {
                    alarmRecorder.report(config, failure == null
                            ? "处理服务未返回该接口的运行记录" : failure.getMessage());
                } else {
                    alarmRecorder.recover(config);
                }
            } catch (Exception exception) {
                LOGGER.warn("采集接口后台同步结果上下文记录失败，接口编号：{}", id, exception);
            }
        }
        // 没有运行接口或数据库上下文不可用时，结果作为全局运行日志记录。
        if (idsByTask.isEmpty()) {
            idsByTask.put(null, enabledIds);
        }
        for (Map.Entry<String, List<Long>> entry : idsByTask.entrySet()) {
            try {
                SystemLogLevel level = failure != null ? SystemLogLevel.ERROR
                        : failedIds.isEmpty() ? SystemLogLevel.INFO : SystemLogLevel.WARN;
                operationLogs.record(entry.getKey(), SystemLogSource.COLLECTION, level,
                        failure != null ? "采集接口后台同步失败"
                                : failedIds.isEmpty() ? "采集接口后台同步完成" : "部分采集接口后台同步未完成",
                        "本任务接口ID：" + entry.getValue() + "\n" + detail, null);
            } catch (Exception exception) {
                LOGGER.error("采集接口后台同步结果写入运行日志失败，原同步结果：{}", detail, exception);
            }
        }
    }

    /** 计算本次完整同步的实际耗时。 */
    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    /** 人工请求线程只记录提交状态，不把后台结果写入已经结束的请求。 */
    private void setOperationResult(String detail, boolean failed) {
        org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(com.example.dataadmin.config.OperationLogAdvice.SYNC_RESULT, detail,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
            attributes.setAttribute(com.example.dataadmin.config.OperationLogAdvice.SYNC_FAILED, failed,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        }
    }

    /** 关闭时停止追加同步批次，执行器由Spring统一关闭。 */
    @PreDestroy
    public void stopScheduling() {
        stopping = true;
        syncRequested.set(false);
    }
}
