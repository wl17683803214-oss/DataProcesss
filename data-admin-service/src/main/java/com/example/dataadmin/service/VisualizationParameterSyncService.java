package com.example.dataadmin.service;

import java.util.List;

/**
 * 可视化参数筛选项同步服务。
 */
public interface VisualizationParameterSyncService {

    /**
     * 将指定试验任务的已有组件参数项重新绑定到有效解析参数。
     *
     * @param taskId 试验任务ID
     */
    void synchronizeTaskParameters(String taskId);

    /** 仅更新本次修改参数对应的可视化组件名称和代号。 */
    void synchronizeChangedParameters(String taskId, List<Long> parseRuleIds);
}
