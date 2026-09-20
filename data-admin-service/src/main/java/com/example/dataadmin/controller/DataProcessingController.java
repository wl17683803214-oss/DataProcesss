package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.common.response.PageResult;
import com.example.dataadmin.dto.processing.ProcessingRuleConfigRequest;
import com.example.dataadmin.entity.DataProcessLog;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.ProcessingRuleConfig;
import com.example.dataadmin.service.DataProcessingService;
import com.example.dataadmin.vo.processing.DataProcessingOverviewVO;
import com.example.dataadmin.vo.processing.DataProcessingRealtimeVO;
import com.example.dataadmin.vo.processing.CollectInterfaceOptionVO;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
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

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 数据处理任务和遥测解析规则接口。
 */
@RestController
@RequestMapping("/processing")
public class DataProcessingController {

    private final DataProcessingService processingService;

    public DataProcessingController(DataProcessingService processingService) {
        this.processingService = processingService;
    }

    /** 查询数据处理页面涉及的全部枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.processing());
    }

    /** 查询数据处理页面筛选框使用的已启用采集接口。 */
    @GetMapping("/collect-interfaces")
    public ApiResponse<List<CollectInterfaceOptionVO>> listCollectInterfaces() {
        return ApiResponse.success(
                processingService.listEnabledCollectInterfaces());
    }

    /** 按试验任务查询今日处理量和历史累计去重数量。 */
    @GetMapping("/overview")
    public ApiResponse<DataProcessingOverviewVO> getOverview(
            @RequestParam String taskId) {
        // 处理总览必须限定试验任务，避免跨任务累计处理指标。
        return ApiResponse.success(processingService.getOverview(taskId));
    }

    /** 按试验任务查询实时处理速率及数据处理服务运行指标。 */
    @GetMapping("/realtime")
    public ApiResponse<DataProcessingRealtimeVO> getRealtime(
            @RequestParam String taskId) {
        return ApiResponse.success(processingService.getRealtime(taskId));
    }

    /** 按任务查询IoTDB中最新的处理成功遥测帧。 */
    @GetMapping("/realtime/telemetry")
    public ApiResponse<List<RealtimeTelemetryFrameVO>> listRealtimeTelemetry(
            @RequestParam String taskId,
            @RequestParam(required = false) Long interfaceId) {
        return ApiResponse.success(
                processingService.listRealtimeTelemetryFrames(
                        taskId, interfaceId));
    }

    /** 按任务和采集接口查询IoTDB中的处理后遥测参数。 */
    @GetMapping("/processed")
    public ApiResponse<List<ProcessedTelemetryVO>> listProcessedTelemetry(
            @RequestParam String taskId,
            @RequestParam(required = false) Long interfaceId) {
        return ApiResponse.success(
                processingService.listProcessedTelemetry(
                        taskId, interfaceId));
    }

    /** 按任务和页面条件查询IoTDB中帧检查异常的遥测原始帧。 */
    @GetMapping("/realtime/invalid")
    public ApiResponse<List<InvalidTelemetryFrame>> listInvalidTelemetry(
            @RequestParam String taskId,
            @RequestParam(required = false) Long interfaceId,
            @RequestParam(required = false) String satelliteCode,
            @RequestParam(required = false) String channelCode) {
        return ApiResponse.success(
                processingService.listInvalidTelemetryFrames(
                        taskId, interfaceId, satelliteCode, channelCode));
    }

    /** 查询任务级参数值范围检查开关。 */
    @GetMapping("/rule-config")
    public ApiResponse<ProcessingRuleConfig> getRuleConfig(
            @RequestParam String taskId) {
        return ApiResponse.success(
                processingService.getProcessingRuleConfig(taskId));
    }

    /** 保存任务级参数值范围检查开关。 */
    @PostMapping("/rule-config/update")
    public ApiResponse<ProcessingRuleConfig> updateRuleConfig(
            @Valid @RequestBody ProcessingRuleConfigRequest request) {
        ProcessingRuleConfig config = new ProcessingRuleConfig();
        BeanUtils.copyProperties(request, config);
        return ApiResponse.success(
                processingService.saveProcessingRuleConfig(config));
    }

    /** 使用PageHelper分页查询数据处理日志。 */
    @GetMapping("/logs")
    public ApiResponse<PageResult<DataProcessLog>> listLogs(
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) Long processTaskId,
            @RequestParam(required = false) Integer logLevel,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(processingService.pageProcessLogs(
                taskId,
                processTaskId,
                logLevel,
                pageNum,
                pageSize));
    }

    /** 查询参数筛选用设备卫星，仅返回编码字段。 */
    @GetMapping("/device-satellites")
    public ApiResponse<List<com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO>> listDeviceSatellites(
            @RequestParam String taskId,
            @RequestParam(required = false) String type) {
        return ApiResponse.success(processingService.listDeviceSatellites(taskId, type));
    }

    /** 查询遥测解析规则。 */
    @GetMapping("/rules")
    public ApiResponse<List<TelemetryParseRuleConfig>> listRules(
            @RequestParam String taskId,
            @RequestParam Long deviceSatelliteId) {
        return ApiResponse.success(processingService.listParseRules(taskId, deviceSatelliteId));
    }

    /** 从Excel页签或制表符TXT导入设备卫星及参数，按当前任务和类型替换。 */
    @PostMapping(value = "/rules/import", consumes = "multipart/form-data")
    public ApiResponse<Integer> importRules(
            @RequestParam String taskId,
            @RequestParam String type,
            @RequestParam("file") MultipartFile file) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的Excel或TXT文件");
        }
        try {
            return ApiResponse.success(
                    processingService.importParseRules(
                            taskId,
                            type,
                            file.getOriginalFilename(),
                            file.getBytes()));
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("读取导入文件失败", ex);
        }
    }

}
