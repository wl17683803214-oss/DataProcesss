package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 参数配置数据访问，任务ID始终参与查询和修改。 */
public interface TelemetryParseRuleConfigMapper {
    /** 查询当前任务所选设备卫星的参数。 */
    List<TelemetryParseRuleConfig> findAllByTaskId(@Param("taskId") String taskId, @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 分批导入配置。 */
    int batchInsert(@Param("list") List<TelemetryParseRuleConfig> list);
    /** 逻辑删除当前任务同类型设备卫星的参数。 */
    int deleteByType(@Param("taskId") String taskId, @Param("type") String type);
}
