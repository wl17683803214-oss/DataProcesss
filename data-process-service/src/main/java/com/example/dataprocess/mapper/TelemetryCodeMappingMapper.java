package com.example.dataprocess.mapper;

import com.example.dataprocess.entity.TelemetryCodeMapping;
import com.example.dataprocess.entity.PdxpFrameSource;
import com.example.dataprocess.protocol.rpc.PdxpProtocolField;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** RPC结果关联遥测代号的数据访问接口。 */
public interface TelemetryCodeMappingMapper {

    /** 按任务和参数解析配置主键查询有效设备卫星信息。 */
    PdxpFrameSource findFrameSourceByRuleId(
            @Param("taskId") String taskId,
            @Param("ruleId") Long ruleId);

    /** 按设备主键加载导入工作表名称。 */
    PdxpFrameSource findFrameSourceByDeviceId(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 接口启动时一次加载有效的本地解析字段。 */
    List<PdxpProtocolField> findPdxpFieldsByDeviceId(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId);

    /** 按任务和遥测代号索引列表批量查询映射。 */
    List<TelemetryCodeMapping> findByTaskIdAndTableIndexes(
            @Param("taskId") String taskId,
            @Param("tableIndexes") List<Integer> tableIndexes);
}
