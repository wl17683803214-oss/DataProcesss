package com.example.dataadmin.vo.collection;

/**
 * 采集接口事件级别统计。
 */
public class CollectionEventOverviewVO {

    /** 信息事件数。 */
    private Long infoTotal;

    /** 警告事件数。 */
    private Long warningTotal;

    /** 错误事件数。 */
    private Long errorTotal;

    /** 严重事件数。 */
    private Long criticalTotal;

    public Long getInfoTotal() {
        return infoTotal;
    }

    public void setInfoTotal(Long infoTotal) {
        this.infoTotal = infoTotal;
    }

    public Long getWarningTotal() {
        return warningTotal;
    }

    public void setWarningTotal(Long warningTotal) {
        this.warningTotal = warningTotal;
    }

    public Long getErrorTotal() {
        return errorTotal;
    }

    public void setErrorTotal(Long errorTotal) {
        this.errorTotal = errorTotal;
    }

    public Long getCriticalTotal() {
        return criticalTotal;
    }

    public void setCriticalTotal(Long criticalTotal) {
        this.criticalTotal = criticalTotal;
    }
}
