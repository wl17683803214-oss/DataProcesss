package com.example.simulator.service;

import com.example.simulator.model.PreparedRun;

/** 模拟源启动准备接口，不承担网络发送。 */
public interface RunService {
    /** 在短事务中校验配置和文件，保存快照与运行记录后返回准备结果。 */
    PreparedRun prepare(long sourceId) throws Exception;
}
