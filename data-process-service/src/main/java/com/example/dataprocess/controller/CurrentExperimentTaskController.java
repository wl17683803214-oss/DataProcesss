package com.example.dataprocess.controller;

import com.example.common.response.ApiResponse;
import com.example.dataprocess.collection.CurrentExperimentTaskCache;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 任务开始或结束后刷新共享缓存，不触发采集接口重启。 */
@RestController
@RequestMapping("/internal/collection/tasks")
public class CurrentExperimentTaskController {
    /** 所有接口共享的当前任务缓存。 */
    private final CurrentExperimentTaskCache cache;

    /** 注入任务缓存组件。 */
    public CurrentExperimentTaskController(CurrentExperimentTaskCache cache) {
        this.cache = cache;
    }

    /** 重查数据库而非接收指定任务，避免乱序事件推送旧编号。 */
    @PostMapping("/refresh")
    public ApiResponse<String> refresh() {
        return ApiResponse.success(cache.refresh());
    }
}
