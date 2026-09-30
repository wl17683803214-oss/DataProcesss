package com.example.simulator.model;

/** 启动准备结果，配置和运行记录共同交给运行管理器。 */
public class PreparedRun {
    /** 本次发送使用的独立配置。 */
    public final SourceConfig config;
    /** 已创建的本次运行记录。 */
    public final RunRecord record;

    public PreparedRun(SourceConfig config, RunRecord record) {
        // 保存本次准备结果，避免运行管理器依赖服务内部类型。
        this.config = config;
        this.record = record;
    }
}
