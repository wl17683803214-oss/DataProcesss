package com.example.dataadmin.service;

import com.example.dataadmin.entity.ExperimentTask;
import com.example.dataadmin.dto.task.TaskLifecycleEvent;

import java.util.List;

/**
 * 试验任务查询服务。
 */
public interface ExperimentTaskService {

    /** 查询全部试验任务。 */
    List<ExperimentTask> listTasks();

    /** 幂等处理调度系统下发的任务生命周期事件。 */
    void handleLifecycleEvent(TaskLifecycleEvent event);
}
