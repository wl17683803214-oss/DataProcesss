package com.example.dataprocess.collection;

import com.example.dataprocess.mapper.CurrentExperimentTaskMapper;
import com.example.common.enums.ExperimentTaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** 所有采集接口共享的当前任务快照，仅在启动和任务事件通知时查库。 */
@Component
public class CurrentExperimentTaskCache {
    /** 缓存刷新日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentExperimentTaskCache.class);
    /** 最新运行中任务查询组件。 */
    private final CurrentExperimentTaskMapper mapper;
    /** 空值表示没有运行中的试验任务，禁止新遥测和新文件持久化。 */
    private volatile String taskId;

    /** 注入已有业务库的数据访问组件。 */
    public CurrentExperimentTaskCache(CurrentExperimentTaskMapper mapper) {
        this.mapper = mapper;
    }

    /** 接收器创建之前初始化缓存，查库失败时阻止服务以未知归属开始采集。 */
    @PostConstruct
    public void initialize() {
        refresh();
    }

    /** 串行重查数据库最终状态，重复或乱序通知不会覆盖为旧任务。 */
    public synchronized String refresh() {
        // 第一步：查询最新开始且仍在运行的任务，查询失败时不发布新快照。
        String latest = mapper.findLatestRunningTaskId(ExperimentTaskStatus.RUNNING.getCode());
        // 第二步：一次发布可空任务编号，采集线程只读取内存。
        taskId = latest;
        LOGGER.info("当前试验任务缓存已刷新，任务编号：{}", latest == null ? "无运行中任务" : latest);
        return latest;
    }

    /** 单帧或单文件读取一次，后续异步处理不得再次改变其归属。 */
    public String currentTaskId() {
        return taskId;
    }
}
