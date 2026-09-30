package com.example.dataadmin.mapper;

import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;

/** 处理后遥测参数勾选记录访问。 */
public interface ProcessedTelemetryFilterSelectionMapper {
    /** 查询设备下已勾选的遥测代号。 */
    List<String> findSelectedCodes(@Param("taskId") String taskId,
                                   @Param("deviceSatelliteId") Long deviceSatelliteId,
                                   @Param("selectionType") int selectionType,
                                   @Param("targetId") Long targetId);
    /** 查询当前任务下已选设备与参数代号。 */
    List<ProcessedTelemetryFilterSelection> findSelected(@Param("taskId") String taskId,
            @Param("selectionType") int selectionType, @Param("targetId") Long targetId);
    /** 批量修改代号时取得设备在所有场景中的有效勾选。 */
    List<ProcessedTelemetryFilterSelection> findSelectedByDevice(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 立即保存勾选或取消勾选状态。 */
    int save(@Param("taskId") String taskId, @Param("deviceSatelliteId") Long deviceSatelliteId,
             @Param("telemetryCode") String telemetryCode,
             @Param("selectionType") int selectionType, @Param("targetId") Long targetId,
             @Param("deleted") int deleted);
    /** 删除组件时清除其所有参数勾选。 */
    int deleteByTarget(@Param("taskId") String taskId, @Param("targetId") Long targetId);
    /** 把原代号的有效勾选记录设为取消，待所有旧代号处理完再恢复新代号。 */
    int markDeleted(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId,
            @Param("telemetryCode") String telemetryCode);
    /** 全量导入后清除已不再存在的勾选参数。 */
    int removeMissing(@Param("taskId") String taskId, @Param("type") String type);
}
