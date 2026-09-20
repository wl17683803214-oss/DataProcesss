package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.entity.ExperimentTask;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.service.ExperimentTaskService;
import com.example.dataadmin.vo.EnumOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 试验任务查询接口。
 */
@RestController
@RequestMapping("/experiment/tasks")
public class ExperimentTaskController {

    private final ExperimentTaskService experimentTaskService;

    public ExperimentTaskController(ExperimentTaskService experimentTaskService) {
        this.experimentTaskService = experimentTaskService;
    }

    /** 查询试验任务状态和结束原因枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.experimentTask());
    }

    /** 查询全部试验任务，供页面筛选框使用。 */
    @GetMapping
    public ApiResponse<List<ExperimentTask>> listTasks() {
        return ApiResponse.success(experimentTaskService.listTasks());
    }
}
