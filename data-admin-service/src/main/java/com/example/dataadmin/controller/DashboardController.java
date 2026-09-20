package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.enums.DashboardRange;
import com.example.dataadmin.service.DashboardService;
import com.example.dataadmin.vo.EnumOptionVO;
import com.example.dataadmin.vo.dashboard.CollectionDashboardVO;
import com.example.dataadmin.vo.dashboard.ProcessingDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 系统首页总览，采集与处理分别查询真实统计数据。 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    /** 总览业务组件。 */
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** 查询首页总览涉及的全部枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.dashboard());
    }

    /** 查询数据采集总览，供前端按固定周期轮询。 */
    @GetMapping("/collection")
    public ApiResponse<CollectionDashboardVO> collection(
            @RequestParam(required = false) String taskId,
            @RequestParam(defaultValue = "3h") DashboardRange range) {
        // 三种时间范围统一查询五秒统计表，不再返回控制器内置示例。
        return ApiResponse.success(
                service.getCollectionDashboard(taskId, range));
    }

    /** 查询数据处理总览，供前端按固定周期轮询。 */
    @GetMapping("/processing")
    public ApiResponse<ProcessingDashboardVO> processing(
            @RequestParam(required = false) String taskId,
            @RequestParam(defaultValue = "3h") DashboardRange range) {
        // 处理量、去重量和异常量全部来自真实五秒统计记录。
        return ApiResponse.success(
                service.getProcessingDashboard(taskId, range));
    }
}
