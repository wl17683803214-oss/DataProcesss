package com.example.simulator.model;

import com.example.simulator.enums.RunStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.LocalDateTime;

/** 一次执行的持久化快照和发送统计。 */
public class RunRecord {
    /** 本次执行主键。 */
    public Long id;
    /** 模拟源主键。 */
    public Long sourceId;
    /** 持久化状态编码，对外由枚举转为中文。 */
    @JsonIgnore
    public Integer statusCode;
    /** 启动时的完整配置快照。 */
    @JsonRawValue
    public String configSnapshot;
    /** 启动时间。 */
    public LocalDateTime startTime;
    /** 结束时间。 */
    public LocalDateTime endTime;
    /** 已发送数据单元数量，包含FEP结束空单元。 */
    public long sentUnitCount;
    /** 已发送应用层协议字节数，FEP包含请求包。 */
    public long sentByteCount;
    /** 已完成文件交换或读取轮数。 */
    public long completedLoopCount;
    /** 最近发送时间。 */
    public LocalDateTime lastSendTime;
    /** 中文异常原因。 */
    public String errorMessage;
    /** 创建时间。 */
    public LocalDateTime createTime;
    /** 更新时间。 */
    public LocalDateTime updateTime;

    /** 状态只向调用方返回中文。 */
    public String getRunStatus() {
        return RunStatus.of(statusCode).label;
    }
}

