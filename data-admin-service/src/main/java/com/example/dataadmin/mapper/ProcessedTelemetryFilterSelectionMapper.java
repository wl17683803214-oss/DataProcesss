package com.example.dataadmin.mapper;

import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;

/** 处理后遥测参数勾选记录访问。 */
public interface ProcessedTelemetryFilterSelectionMapper {
    /** 查询设备下已勾选的遥测代号。 */
    List<String> findSelectedCodes(@Param("taskId") String taskId,
                                   @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 查询当前任务下已选设备与参数代号。 */
    List<ProcessedTelemetryFilterSelection> findSelected(@Param("taskId") String taskId);
    /** 立即保存勾选或取消勾选状态。 */
    int save(@Param("taskId") String taskId, @Param("deviceSatelliteId") Long deviceSatelliteId,
             @Param("telemetryCode") String telemetryCode, @Param("deleted") int deleted);
    /** 全量导入后清除已不再存在的勾选参数。 */
    int removeMissing(@Param("taskId") String taskId, @Param("type") String type);
}
