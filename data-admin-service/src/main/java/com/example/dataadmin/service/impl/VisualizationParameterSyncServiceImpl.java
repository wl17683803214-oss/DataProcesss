package com.example.dataadmin.service.impl;

import com.example.dataadmin.mapper.MonitorWidgetConfigItemMapper;
import com.example.dataadmin.service.VisualizationParameterSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 同步顺序不能调整：先重新绑定当前规则，再清理失效项，最后补充新增项。
     * 全量导入会生成新的规则ID，通过遥测编码重新绑定可以保留原勾选状态。
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

        // 步骤3：补充新参数，默认设置为未勾选。
        itemMapper.insertMissingParseRuleItems(taskId);
    }
}
