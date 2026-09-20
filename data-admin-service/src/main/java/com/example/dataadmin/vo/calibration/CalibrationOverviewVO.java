package com.example.dataadmin.vo.calibration;

/** 数据校准页面野值数量总览。 */
public class CalibrationOverviewVO {

    /** 当前试验任务累计检出的野值数量。 */
    private Long outlierCount;

    public Long getOutlierCount() {
        return outlierCount;
    }

    public void setOutlierCount(Long outlierCount) {
        this.outlierCount = outlierCount;
    }
}
