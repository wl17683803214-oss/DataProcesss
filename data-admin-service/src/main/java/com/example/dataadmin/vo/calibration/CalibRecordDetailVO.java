package com.example.dataadmin.vo.calibration;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.CalibRecord;
import com.example.dataadmin.entity.CalibRecordDetail;

/** 校准记录汇总和分页野值详情。 */
public class CalibRecordDetailVO {
    /** 校准记录汇总。 */ private CalibRecord record;
    /** 分页野值详情。 */ private PageResult<CalibRecordDetail> details;

    public CalibRecord getRecord() { return record; }
    public void setRecord(CalibRecord record) { this.record = record; }
    public PageResult<CalibRecordDetail> getDetails() { return details; }
    public void setDetails(PageResult<CalibRecordDetail> details) {
        this.details = details;
    }
}
