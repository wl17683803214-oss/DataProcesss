package com.example.dataadmin.service.impl;

import com.example.dataadmin.entity.ExperimentTask;
import com.example.dataadmin.dto.task.TaskLifecycleEvent;
import com.example.dataadmin.enums.ExperimentTaskStatus;
import com.example.dataadmin.enums.TaskEndReason;
import com.example.dataadmin.enums.TaskLifecycleEventType;
import com.example.dataadmin.mapper.ExperimentTaskMapper;
import com.example.dataadmin.service.CollectInterfaceRuntimeSyncService;
import com.example.dataadmin.service.ExperimentTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 试验任务查询服务实现。
 */
@Service
public class ExperimentTaskServiceImpl implements ExperimentTaskService {

    private final ExperimentTaskMapper experimentTaskMapper;
    /** 任务状态变化后同步当前应该运行的采集接口。 */
    private final CollectInterfaceRuntimeSyncService interfaceRuntimeSyncService;

    public ExperimentTaskServiceImpl(
            ExperimentTaskMapper experimentTaskMapper,
            CollectInterfaceRuntimeSyncService interfaceRuntimeSyncService) {
        this.experimentTaskMapper = experimentTaskMapper;
        this.interfaceRuntimeSyncService = interfaceRuntimeSyncService;
    }

    @Override
    public List<ExperimentTask> listTasks() {
        return experimentTaskMapper.findAll();
    }

    /** 按事件类型更新任务快照，并通过数据库条件处理重复和乱序消息。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleLifecycleEvent(TaskLifecycleEvent event) {
        validate(event);
        TaskLifecycleEventType eventType =
                TaskLifecycleEventType.require(event.getEventType());
        ExperimentTask task = toEntity(event);
        if (eventType == TaskLifecycleEventType.STARTED) {
            // 开始事件将任务置为运行中，同轮已结束记录由SQL阻止状态回退。
            task.setTaskStatus(ExperimentTaskStatus.RUNNING.getCode());
            task.setEndReason(null);
            experimentTaskMapper.upsertStarted(task);
            // 事务提交后启动当前运行任务中已启用的采集接口。
            interfaceRuntimeSyncService.syncAfterCommit();
            return;
        }

        // 结束事件必须携带结束原因，并由原因映射最终任务状态。
        TaskEndReason endReason = TaskEndReason.require(event.getEndReason());
        task.setTaskStatus(endReason.getTaskStatus().getCode());
        task.setEndReason(endReason.name());
        experimentTaskMapper.upsertEnded(task);
        // 事务提交后发送完整列表，结束当前任务时处理服务会停止缺失接口。
        interfaceRuntimeSyncService.syncAfterCommit();
    }

    /** 将消息字段转换为任务表快照。 */
    private ExperimentTask toEntity(TaskLifecycleEvent event) {
        ExperimentTask task = new ExperimentTask();
        task.setTaskId(event.getTaskId().trim());
        task.setRequirementId(trimToNull(event.getRequirementId()));
        task.setTaskName(hasText(event.getTaskName())
                ? event.getTaskName().trim()
                : event.getTaskId().trim());
        task.setTaskPriority(event.getTaskPriority());
        task.setPlanId(trimToNull(event.getPlanId()));
        task.setPlanCode(trimToNull(event.getPlanCode()));
        task.setFlowInstanceId(trimToNull(event.getFlowInstanceId()));
        task.setExecutionAttempt(event.getExecutionAttempt() == null
                || event.getExecutionAttempt() < 1
                ? 1
                : event.getExecutionAttempt());
        task.setEventTime(event.getBaseInfo() == null
                ? LocalDateTime.now()
                : parseEventTime(event.getBaseInfo().getTime()));
        return task;
    }

    /** 只校验无法由系统补齐的任务身份字段。 */
    private void validate(TaskLifecycleEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("任务生命周期消息不能为空");
        }
        requireText(event.getTaskId(), "外部试验任务编号不能为空");
    }

    /** 将事件时间转换为数据库本地时间，缺失或异常时使用接收时间。 */
    private LocalDateTime parseEventTime(String value) {
        if (!hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.ofInstant(
                    Instant.parse(value.trim()), ZoneId.systemDefault());
        } catch (RuntimeException exception) {
            return LocalDateTime.now();
        }
    }

    /** 校验必填文本字段。 */
    private void requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    /** 判断文本是否包含有效内容。 */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** 去除可选文本两端空白，空白内容统一转换为空值。 */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
