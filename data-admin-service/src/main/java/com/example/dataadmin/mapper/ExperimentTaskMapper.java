package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.ExperimentTask;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 试验任务数据访问接口。
 */
public interface ExperimentTaskMapper {

    /** 查询全部试验任务，并按最近事件时间倒序排列。 */
    List<ExperimentTask> findAll();

    /** 只查询指定生命周期状态的任务编号，供健康上报使用。 */
    List<String> findTaskIdsByStatus(@Param("status") Integer status);

    /** 幂等保存任务开始事件，并阻止迟到开始事件覆盖同轮结束状态。 */
    int upsertStarted(ExperimentTask task);

    /** 幂等保存任务结束事件，较新的执行轮次优先。 */
    int upsertEnded(ExperimentTask task);
}
