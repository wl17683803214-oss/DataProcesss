package com.example.dataadmin.service.impl;

import com.example.dataadmin.mapper.MonitorWidgetConfigItemMapper;
import com.example.dataadmin.service.VisualizationParameterSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 可视化参数筛选项同步服务实现。
 */
@Service
public class VisualizationParameterSyncServiceImpl
        implements VisualizationParameterSyncService {

    /** 可视化组件数据项数据访问组件。 */
    private final MonitorWidgetConfigItemMapper itemMapper;

    public VisualizationParameterSyncServiceImpl(
            MonitorWidgetConfigItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    /**
     * 同步任务参数筛选项。
     *
     * 仅维护已有组件数据项，新增参数通过按需筛选树展示。
     * 全量导入会生成新的规则ID，通过遥测编码重新绑定已有组件数据项。
     */
    @Override
    @Transactional
    public void synchronizeTaskParameters(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }

        // 步骤1：按规则ID或遥测编码重新绑定，并同步参数编码和名称。
        itemMapper.bindActiveParseRuleItems(taskId);

        // 步骤2：删除解析配置中已经不存在的自动同步筛选项。
        itemMapper.deleteObsoleteParseRuleItems(taskId);

        // 新参数不批量写入所有组件，避免大量无用配置项。
    }

    /** 已有参数只更新对应组件项，避免批量编辑时扫描整张任务参数表。 */
    @Override
    @Transactional
    public void synchronizeChangedParameters(String taskId, List<Long> parseRuleIds) {
        if (taskId == null || taskId.trim().isEmpty()
                || parseRuleIds == null || parseRuleIds.isEmpty()) {
            throw new IllegalArgumentException("试验任务和待同步参数不能为空");
        }
        itemMapper.updateChangedParseRuleItems(taskId, parseRuleIds);
    }
}
