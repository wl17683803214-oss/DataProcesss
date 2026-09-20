package com.example.dataadmin.messaging;

import com.example.dataadmin.dto.task.TaskLifecycleEvent;
import com.example.dataadmin.service.ExperimentTaskService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/** 接收调度系统发送的试验任务开始和结束事件。 */
@Component
@RocketMQMessageListener(
        topic = "${task-lifecycle.topic}",
        selectorExpression = "${task-lifecycle.selector-expression}",
        consumerGroup = "${task-lifecycle.consumer-group}")
public class TaskLifecycleEventConsumer
        implements RocketMQListener<TaskLifecycleEvent> {

    private final ExperimentTaskService experimentTaskService;

    public TaskLifecycleEventConsumer(
            ExperimentTaskService experimentTaskService) {
        this.experimentTaskService = experimentTaskService;
    }

    /** 消费消息失败时抛出异常，由RocketMQ按照消费策略重新投递。 */
    @Override
    public void onMessage(TaskLifecycleEvent event) {
        experimentTaskService.handleLifecycleEvent(event);
    }
}
