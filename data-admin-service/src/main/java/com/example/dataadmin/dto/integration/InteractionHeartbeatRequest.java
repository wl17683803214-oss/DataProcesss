package com.example.dataadmin.dto.integration;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 交互系统心跳上报请求。 */
public class InteractionHeartbeatRequest {

    /** 当前系统标识。 */
    private String systemId;

    /** 心跳状态编码：0正常，1异常。 */
    private String status;

    /** 上报时间戳，当前按接口约定返回空。 */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Long reportTs;

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReportTs() {
        return reportTs;
    }

    public void setReportTs(Long reportTs) {
        this.reportTs = reportTs;
    }
}
