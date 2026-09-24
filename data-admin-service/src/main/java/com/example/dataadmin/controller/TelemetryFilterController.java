package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.dto.processing.TelemetryFilterSelectionRequest;
import com.example.dataadmin.vo.processing.TelemetrySystemNodeVO;
import com.example.dataadmin.service.TelemetryFilterService;
import com.example.dataadmin.vo.processing.TelemetryFilterNodeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 数据处理页面所属系统与参数筛选接口。 */
@RestController
@RequestMapping("/processing")
public class TelemetryFilterController {
    private final TelemetryFilterService service;

    public TelemetryFilterController(TelemetryFilterService service) {
        this.service = service;
    }

    /** 查询当前设备的有效所属系统节点。 */
    @GetMapping("/systems")
    public ApiResponse<List<TelemetrySystemNodeVO>> systems(
            @RequestParam String taskId, @RequestParam Long deviceSatelliteId) {
        return ApiResponse.success(service.systems(taskId, deviceSatelliteId));
    }

    /** 常规模式返回当前展开层级，传遥测代号时返回全部匹配参数。 */
    @GetMapping("/telemetry-filter-tree")
    public ApiResponse<List<TelemetryFilterNodeVO>> tree(
            @RequestParam String taskId,
            @RequestParam(required = false) Long deviceSatelliteId,
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) String telemetryCode) {
        return ApiResponse.success(service.list(taskId, deviceSatelliteId, systemId,
                telemetryCode));
    }

    /** 勾选或取消勾选时立即保存当前遥测参数。 */
    @PostMapping("/telemetry-filter-selection/update")
    public ApiResponse<Boolean> update(@Valid @RequestBody TelemetryFilterSelectionRequest request) {
        service.update(request);
        return ApiResponse.success(true);
    }
}
