package com.example.dataadmin.service.monitor;

/** 独立的系统健康消息上报入口。 */
public interface SystemHealthReportService {
    /** 读取一次最新快照，向当前运行中的任务发送健康消息。 */
    void reportLatestSnapshot();
}
