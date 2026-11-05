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
    /** 保存已勾选记录，兼容历史取消记录时恢复有效状态。 */
    int save(@Param("taskId") String taskId, @Param("deviceSatelliteId") Long deviceSatelliteId,
             @Param("telemetryCode") String telemetryCode,
             @Param("selectionType") int selectionType, @Param("targetId") Long targetId);
    /** 按完整业务身份物理删除单个参数的勾选记录。 */
    int deleteSelected(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId,
            @Param("telemetryCode") String telemetryCode,
            @Param("selectionType") Integer selectionType, @Param("targetId") Long targetId);
    /** 按任务、场景和目标物理删除勾选记录，返回实际删除数量。 */
    int clearByScope(@Param("taskId") String taskId,
            @Param("selectionType") Integer selectionType, @Param("targetId") Long targetId);
    /** 删除组件时清除其所有参数勾选。 */
    int deleteByTarget(@Param("taskId") String taskId, @Param("targetId") Long targetId);
    /** 删除原代号在全部场景中的勾选记录，待旧代号处理完再恢复新代号。 */
    int deleteByParameter(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId,
            @Param("telemetryCode") String telemetryCode);
    /** 全量导入后清除已不再存在的勾选参数。 */
    int removeMissing(@Param("taskId") String taskId, @Param("type") String type);
}
