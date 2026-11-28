package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.ProcessingRuleConfig;
/** 全局处理规则配置数据访问接口。 */
public interface ProcessingRuleConfigMapper {
    /** 读取固定主键的全局配置，不按任务查询。 */
    ProcessingRuleConfig findGlobal();
    /** 首次访问补齐默认配置，已有配置保持不变。 */
    int insert(ProcessingRuleConfig config);
    /** 保存全局开关，不修改历史兼容记录。 */
    int update(ProcessingRuleConfig config);
}
