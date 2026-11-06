package com.example.dataadmin.service;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.ProcessingRuleConfig;
import com.example.dataadmin.vo.processing.DataProcessingOverviewVO;
import com.example.dataadmin.vo.processing.DataProcessingRealtimeVO;
import com.example.dataadmin.vo.processing.CollectInterfaceOptionVO;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.example.dataadmin.entity.InvalidTelemetryFrame;
import com.example.dataadmin.dto.processing.TelemetryParseRuleBatchUpdateRequest;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 数据处理页面业务接口。
 *
 * 负责组织页面所需的查询和写入操作，数据库访问由对应Mapper完成。
 */
public interface DataProcessingService {
    /** 按现有文件导入规则，在独立事务内按设备名称替换基线设备参数。 */
    int importBaselineDeviceRules(String taskId,
            java.util.Map<String, List<TelemetryParseRuleConfig>> rules);
    /** 在独立写事务内按卫星编码替换已校验的基线参数。 */
    int importBaselineRules(String taskId,
            java.util.Map<String, List<TelemetryParseRuleConfig>> rules,
            java.util.Map<String, String> names);
    /** 仅使用设备卫星主键删除目录及关联参数配置。 */
    void deleteParseRules(Long deviceSatelliteId);
    /** 按名称查询数据处理页面使用的采集接口筛选项。 */
    List<CollectInterfaceOptionVO> listCollectInterfaceOptions(String interfaceName);
    /** 查询数据处理页面顶部统计数据。 */
    DataProcessingOverviewVO getOverview(String taskId);
    /** 查询数据处理页面实时监控指标。 */
    DataProcessingRealtimeVO getRealtime(String taskId);
    /** 按任务、接口和时间范围查询最新的一百条正常整帧。 */
    List<RealtimeTelemetryFrameVO> listRealtimeTelemetryFrames(
            String taskId,
            Long interfaceId,
            LocalDateTime startTime,
            LocalDateTime endTime);
    /** 查询当前任务全部已勾选参数的最新处理结果。 */
    List<ProcessedTelemetryVO> listProcessedTelemetry(String taskId,
            Integer selectionType, Long targetId, String telemetryType, LocalDateTime startTime, LocalDateTime endTime);
    /** 查询当前任务全部已勾选参数的最近曲线点。 */
    List<ProcessedTelemetryCurveVO> listProcessedTelemetryCurves(String taskId,
            Integer selectionType, Long targetId, String telemetryType, LocalDateTime startTime, LocalDateTime endTime);
    /** 按任务、接口和时间范围查询最新的一百条异常整帧。 */
    List<InvalidTelemetryFrame> listInvalidTelemetryFrames(
            String taskId,
            Long interfaceId,
            LocalDateTime startTime,
            LocalDateTime endTime);
    /** 查询任务级处理规则开关；不存在时插入默认配置后返回。 */
    ProcessingRuleConfig getProcessingRuleConfig(String taskId);
    /** 新增或更新任务级处理规则开关。 */
    ProcessingRuleConfig saveProcessingRuleConfig(ProcessingRuleConfig config);
    /** 查询当前任务指定类型的设备卫星筛选项。 */
    List<com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO> listDeviceSatellites(String taskId, String type);
    /** 查询指定试验任务的遥测解析规则列表。 */
    List<TelemetryParseRuleConfig> listParseRules(String taskId, Long deviceSatelliteId);
    /** 分页查询设备全部参数或指定所属系统的直属参数。 */
    PageResult<TelemetryParseRuleConfig> pageParseRules(String taskId, Long deviceSatelliteId,
            Long systemId, Integer pageNum, Integer pageSize);
    /** 同一事务内完整修改当前任务的多条参数解析配置。 */
    int batchUpdateParseRules(TelemetryParseRuleBatchUpdateRequest request);
    /** 从Excel或制表符TXT批量导入遥测解析规则，返回成功导入数量。 */
    int importParseRules(
            String taskId,
            String type,
            String fileName,
            byte[] content);
}
