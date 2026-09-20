package com.example.dataadmin.service;

/**
 * 可视化参数筛选项同步服务。
 */
public interface VisualizationParameterSyncService {

    /**
     * 将指定试验任务的有效遥测解析参数同步到实时曲线和实时数据组件。
     *
     * @param taskId 试验任务ID
     */
    void synchronizeTaskParameters(String taskId);
}
