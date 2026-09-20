package com.example.dataadmin.service;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.DataProcessLog;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.ProcessingRuleConfig;
import com.example.dataadmin.vo.processing.DataProcessingOverviewVO;
import com.example.dataadmin.vo.processing.DataProcessingRealtimeVO;
import com.example.dataadmin.vo.processing.CollectInterfaceOptionVO;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import com.example.dataadmin.entity.InvalidTelemetryFrame;

import java.util.List;

/**
 * 数据处理页面业务接口。
 *
 * 负责组织页面所需的查询和写入操作，数据库访问由对应Mapper完成。
 */
public interface DataProcessingService {
    /** 查询数据处理页面使用的已启用采集接口筛选项。 */
    List<CollectInterfaceOptionVO> listEnabledCollectInterfaces();
    /** 查询数据处理页面顶部统计数据。 */
    DataProcessingOverviewVO getOverview(String taskId);
    /** 查询数据处理页面实时监控指标。 */
    DataProcessingRealtimeVO getRealtime(String taskId);
    /** 按任务和采集接口筛选IoTDB中的完整遥测原始帧。 */
    List<RealtimeTelemetryFrameVO> listRealtimeTelemetryFrames(
            String taskId, Long interfaceId);
    /** 按任务和采集接口筛选IoTDB中的处理后遥测参数。 */
    List<ProcessedTelemetryVO> listProcessedTelemetry(
            String taskId, Long interfaceId);
    /** 按任务和页面条件筛选帧检查异常的遥测原始帧。 */
    List<InvalidTelemetryFrame> listInvalidTelemetryFrames(
            String taskId,
            Long interfaceId,
            String satelliteCode,
            String channelCode);
    /** 查询任务级处理规则开关；不存在时插入默认配置后返回。 */
    ProcessingRuleConfig getProcessingRuleConfig(String taskId);
    /** 新增或更新任务级处理规则开关。 */
    ProcessingRuleConfig saveProcessingRuleConfig(ProcessingRuleConfig config);
    /** 分页查询数据处理日志。 */
    PageResult<DataProcessLog> pageProcessLogs(
            String taskId,
            Long processTaskId,
            Integer logLevel,
            Integer pageNum,
            Integer pageSize);
    /** 预留数据处理日志写入能力，自动产生日志的逻辑后续接入。 */
    Long createProcessLog(DataProcessLog log);
    /** 查询当前任务指定类型的设备卫星筛选项。 */
    List<com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO> listDeviceSatellites(String taskId, String type);
    /** 查询指定试验任务的遥测解析规则列表。 */
    List<TelemetryParseRuleConfig> listParseRules(String taskId, Long deviceSatelliteId);
    /** 从Excel或制表符TXT批量导入遥测解析规则，返回成功导入数量。 */
    int importParseRules(
            String taskId,
            String type,
            String fileName,
            byte[] content);
}
