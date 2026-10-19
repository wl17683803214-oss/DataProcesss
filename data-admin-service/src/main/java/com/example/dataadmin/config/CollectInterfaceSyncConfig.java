package com.example.dataadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 采集接口同步使用独立线程，避免管理请求等待远程服务。 */
@Configuration
public class CollectInterfaceSyncConfig {

    /** 全量同步串行执行，任务由同步服务合并，不回退到请求线程执行。 */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService collectInterfaceSyncExecutor() {
        // 一个工作线程按顺序同步最新配置，队列仅保留工作线程交接所需容量。
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1),
                task -> new Thread(task, "采集接口同步线程"),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
