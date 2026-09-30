package com.example.simulator.model;

import com.example.simulator.enums.ProtocolProfile;
import com.example.simulator.enums.RunStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/** 模拟源持久化配置和详情视图。 */
public class SourceConfig {
    /** 模拟源主键。 */
    public Long id;
    /** 模拟源名称。 */
    public String sourceName;
    /** 协议类型，沿用已有协议编码。 */
    public Integer transferProtocol;
    /** 传输方式，沿用已有传输编码。 */
    public Integer transferType;
    /** 服务器本地文件路径。 */
    public String filePath;
    /** 目标地址。 */
    public String targetHost = "127.0.0.1";
    /** 目标端口。 */
    public Integer targetPort;
    /** 数据单元发送间隔。 */
    public Long sendIntervalMillis;
    /** 是否循环，沿用零否一是约定。 */
    public Integer loopEnabled;
    /** 用户备注。 */
    public String remark;
    /** 创建时间。 */
    public LocalDateTime createTime;
    /** 修改时间。 */
    public LocalDateTime updateTime;
    /** 仅PDXP使用的协议参数。 */
    public PdxpConfig pdxp;
    /** 最近运行的数据库状态，用于中文展示。 */
    @JsonIgnore
    public Integer latestStatus;
    /** 最近运行主键。 */
    public Long latestRunId;
    /** 最近运行已发送单元数。 */
    public Long sentUnitCount;
    /** 返回中文运行状态，未运行时不创建虚假记录。 */
    public String getRunStatus() {
        return latestStatus == null ? "未启动" : RunStatus.of(latestStatus).label;
    }
    /** 返回协议显示名称。 */
    public String getProtocolName() {
        return ProtocolProfile.of(transferProtocol).label;
    }
    /** 返回传输方式中文名称。 */
    public String getTransferTypeName() {
        return ProtocolProfile.of(transferProtocol).transportLabel;
    }
    /** 返回是否循环的中文名称。 */
    public String getLoopEnabledName() {
        return Integer.valueOf(1).equals(loopEnabled) ? "是" : "否";
    }
}

