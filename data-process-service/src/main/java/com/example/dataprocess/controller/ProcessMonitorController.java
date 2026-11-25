package com.example.dataprocess.controller;

import com.example.common.response.ApiResponse;
import com.example.dataprocess.service.ProcessCpuSampler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供管理服务查询处理进程 CPU 的内部接口。 */
@RestController
@RequestMapping("/internal/monitor")
public class ProcessMonitorController {

    private final ProcessCpuSampler cpuSampler;

    public ProcessMonitorController(ProcessCpuSampler cpuSampler) {
        this.cpuSampler = cpuSampler;
    }

    /** 直接返回后台采样值，查询请求无需等待下一次采样。 */
    @GetMapping("/cpu")
    public ApiResponse<Double> getCpuUsage() {
        return ApiResponse.success(cpuSampler.getCpuUsagePercent());
    }
}
