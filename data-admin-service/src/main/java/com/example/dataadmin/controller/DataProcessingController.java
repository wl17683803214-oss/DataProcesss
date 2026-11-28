package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.common.response.PageResult;
import com.example.dataadmin.dto.processing.ProcessingRuleConfigRequest;
import com.example.dataadmin.dto.processing.TelemetryParseRuleBatchUpdateRequest;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.ProcessingRuleConfig;
import com.example.dataadmin.service.DataProcessingService;
import com.example.dataadmin.service.BaselineSatelliteSyncService;
import com.example.dataadmin.service.BaselineDeviceSyncService;
import com.example.dataadmin.vo.processing.BaselineDeviceSyncVO;
import com.example.dataadmin.vo.processing.BaselineSatelliteSyncVO;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.dataadmin.vo.processing.DataProcessingOverviewVO;
import com.example.dataadmin.vo.processing.DataProcessingRealtimeVO;
import com.example.dataadmin.vo.processing.CollectInterfaceOptionVO;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.example.dataadmin.entity.InvalidTelemetryFrame;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.vo.EnumOptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据处理任务和遥测解析规则接口。
 */
@RestController
@RequestMapping("/processing")
public class DataProcessingController {

    private final DataProcessingService processingService;
    /** 基线卫星参数同步服务。 */
    @Autowired
    private BaselineSatelliteSyncService baselineSatelliteSyncService;
    /** 基线设备参数表同步服务。 */
    @Autowired
    private BaselineDeviceSyncService baselineDeviceSyncService;

    /** 下载基线设备参数表到内存，按固定列顺序同步到全局配置。 */
    @PostMapping("/rules/sync-baseline-devices")
    public ApiResponse<BaselineDeviceSyncVO> syncBaselineDevices() {
        // 先查询并校验全部参数表，再进入独立写事务。
        return ApiResponse.success(baselineDeviceSyncService.synchronize(null));
    }

    /** 将基线全部卫星遥测参数同步到当前任务。 */
    @PostMapping("/rules/sync-baseline")
    public ApiResponse<BaselineSatelliteSyncVO> syncBaselineRules() {
        // 远程查询和完整校验成功后才进入参数替换事务。
        return ApiResponse.success(baselineSatelliteSyncService.synchronize(null));
    }

    public DataProcessingController(DataProcessingService processingService) {
        this.processingService = processingService;
    }

    /** 查询数据处理页面涉及的全部枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.processing());
    }

    /** 按试验任务和可选名称查询正常帧、异常帧的采集接口筛选项。 */
    @GetMapping("/collect-interfaces")
    public ApiResponse<List<CollectInterfaceOptionVO>> listCollectInterfaces(
            @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        // 缺少任务编号时由请求参数校验拒绝，不执行跨任务查询。
        return ApiResponse.success(processingService.listCollectInterfaceOptions(null, interfaceName));
    }

    /** 按时间范围查询今日处理量和历史累计去重数量。 */
    @GetMapping("/overview")
    public ApiResponse<DataProcessingOverviewVO> getOverview() {
        // 处理总览汇总全部接口指标，不接收任务筛选。
        return ApiResponse.success(processingService.getOverview(null));
    }

    /** 按时间范围查询滚动处理速率，以及当前CPU和物理内存指标。 */
    @GetMapping("/realtime")
    public ApiResponse<DataProcessingRealtimeVO> getRealtime() {
        return ApiResponse.success(processingService.getRealtime(null));
    }

    /** 按接口和时间范围查询最新的一百条正常帧。 */
    @GetMapping("/realtime/telemetry")
    public ApiResponse<List<RealtimeTelemetryFrameVO>> listRealtimeTelemetry(
            @RequestParam Long interfaceId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        // 时间默认值和范围校验统一交给业务层，响应直接返回数组。
        return ApiResponse.success(
                processingService.listRealtimeTelemetryFrames(null, interfaceId, startTime, endTime));
    }

    /** 查询全部已勾选参数的最新处理结果。 */
    @GetMapping("/processed")
    public ApiResponse<List<ProcessedTelemetryVO>> listProcessedTelemetry(
            @RequestParam(defaultValue = "1") Integer selectionType,
            @RequestParam(defaultValue = "0") Long targetId,
            @RequestParam(required = false) String telemetryType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        // 按表格场景取得已勾选参数；可视化表格传自己的组件主键。
        return ApiResponse.success(processingService.listProcessedTelemetry(
                null, selectionType, targetId, telemetryType, startTime, endTime));
    }

    /** 查询全部已勾选参数的最近曲线点。 */
    @GetMapping("/processed/curve")
    public ApiResponse<List<ProcessedTelemetryCurveVO>> listProcessedTelemetryCurves(
            @RequestParam(defaultValue = "2") Integer selectionType,
            @RequestParam(defaultValue = "0") Long targetId,
            @RequestParam(required = false) String telemetryType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        // 每条曲线按自己的组件勾选查询，并保留实际采样时间轴。
        return ApiResponse.success(processingService.listProcessedTelemetryCurves(
                null, selectionType, targetId, telemetryType, startTime, endTime));
    }

    /** 按任务、接口和时间范围查询最新的一百条异常帧。 */
    @GetMapping("/realtime/invalid")
    public ApiResponse<List<InvalidTelemetryFrame>> listInvalidTelemetry(
            @RequestParam Long interfaceId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        // 正常和异常帧共用时间范围规则，各自最多返回一百条。
        return ApiResponse.success(
                processingService.listInvalidTelemetryFrames(null, interfaceId, startTime, endTime));
    }

    /** 查询全局参数值范围检查开关。 */
    @GetMapping("/rule-config")
    public ApiResponse<ProcessingRuleConfig> getRuleConfig() {
        return ApiResponse.success(
                processingService.getProcessingRuleConfig(null));
    }

    /** 保存全局参数值范围检查开关。 */
    @PostMapping("/rule-config/update")
    public ApiResponse<ProcessingRuleConfig> updateRuleConfig(
            @Valid @RequestBody ProcessingRuleConfigRequest request) {
        ProcessingRuleConfig config = new ProcessingRuleConfig();
        BeanUtils.copyProperties(request, config);
        return ApiResponse.success(
                processingService.saveProcessingRuleConfig(config));
    }

    /** 查询参数筛选用设备卫星，仅返回编码字段。 */
    @GetMapping("/device-satellites")
    public ApiResponse<List<com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO>> listDeviceSatellites(
            @RequestParam(required = false) String type) {
        return ApiResponse.success(processingService.listDeviceSatellites(null, type));
    }

    /** 查询遥测解析规则。 */
    @GetMapping("/rules")
    public ApiResponse<PageResult<TelemetryParseRuleConfig>> listRules(
            @RequestParam Long deviceSatelliteId,
            @RequestParam(required = false) Long systemId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(processingService.pageParseRules(
                null, deviceSatelliteId, systemId, pageNum, pageSize));
    }

    /** 在全局配置中原子修改多条遥测参数解析配置。 */
    @PostMapping("/rules/batch-update")
    public ApiResponse<Integer> batchUpdateRules(
            @Valid @RequestBody TelemetryParseRuleBatchUpdateRequest request) {
        return ApiResponse.success(processingService.batchUpdateParseRules(request));
    }

    /** 根据设备卫星主键删除目录及全部关联参数配置。 */
    @PostMapping("/rules/delete")
    public ApiResponse<Void> deleteRules(@RequestParam Long deviceSatelliteId) {
        // 复用业务服务执行事务删除并返回统一成功响应。
        processingService.deleteParseRules(deviceSatelliteId);
        return ApiResponse.success();
    }

    /** 从Excel页签或制表符TXT导入设备卫星及参数，仅替换同名配置。 */
    @PostMapping(value = "/rules/import", consumes = "multipart/form-data")
    public ApiResponse<Integer> importRules(
            @RequestParam String type,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的Excel或TXT文件");
        }
        try {
            return ApiResponse.success(
                    processingService.importParseRules(
                            null,
                            type,
                            file.getOriginalFilename(),
                            file.getBytes()));
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("读取导入文件失败", ex);
        }
    }

}
