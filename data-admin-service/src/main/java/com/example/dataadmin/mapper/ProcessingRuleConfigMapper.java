package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.ProcessingRuleConfig;
import org.apache.ibatis.annotations.Param;

/** 任务级处理规则配置数据访问接口。 */
public interface ProcessingRuleConfigMapper {
    ProcessingRuleConfig findByTaskId(@Param("taskId") String taskId);
    int insert(ProcessingRuleConfig config);
    int update(ProcessingRuleConfig config);
}
