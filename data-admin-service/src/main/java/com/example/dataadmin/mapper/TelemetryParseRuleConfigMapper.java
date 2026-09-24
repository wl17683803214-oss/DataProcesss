package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.TelemetryFilterSearchRecord;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 参数配置数据访问，任务ID始终参与查询和修改。 */
public interface TelemetryParseRuleConfigMapper {
    /** 查询当前任务所选设备卫星的参数。 */
    List<TelemetryParseRuleConfig> findAllByTaskId(@Param("taskId") String taskId, @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 查询当前层级直属的有效遥测参数。 */
    List<TelemetryParseRuleConfig> findDirectBySystem(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId, @Param("systemId") Long systemId);
    /** 按主键和任务查验待勾选参数。 */
    TelemetryParseRuleConfig findActiveById(@Param("taskId") String taskId, @Param("id") Long id);
    /** 按遥测代号前缀查找当前任务的有效参数及其勾选状态。 */
    List<TelemetryFilterSearchRecord> searchByTelemetryCode(@Param("taskId") String taskId,
            @Param("codePrefix") String codePrefix);
    /** 统计设备及可选系统子树中的有效参数。 */
    long countPage(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId,
            @Param("systemId") Long systemId);
    /** 分页查询设备及可选系统子树中的有效参数。 */
    List<TelemetryParseRuleConfig> findPage(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId,
            @Param("systemId") Long systemId, @Param("limit") int limit,
            @Param("offset") long offset);
    /** 分批导入配置。 */
    int batchInsert(@Param("list") List<TelemetryParseRuleConfig> list);
    /** 逻辑删除当前任务同类型设备卫星的参数。 */
    int deleteByType(@Param("taskId") String taskId, @Param("type") String type);
}
