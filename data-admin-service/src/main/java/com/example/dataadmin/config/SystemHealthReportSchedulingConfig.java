package com.example.dataadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** 健康消息使用独立调度线程，网络发送不阻塞采样与快照入库。 */
@Configuration
public class SystemHealthReportSchedulingConfig {
    /** 保留默认业务调度器，与独立的消息发送调度器分开。 */
    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        // 保持现有定时任务串行执行，避免改变快照与告警的并发语义。
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("系统业务调度-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    /** 创建串行上报调度器，避免慢发送产生无限积压。 */
    @Bean("systemHealthReportScheduler")
    public ThreadPoolTaskScheduler systemHealthReportScheduler() {
        // 单线程串行发送，同一轮不会与下一轮并行操作。
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("健康消息上报-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
