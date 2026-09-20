package com.example.dataprocess.service;

import com.example.common.calibration.CalibrationRecordDetailCache;

import java.util.List;

/** 在一个数据库事务中写入一批校准样本。 */
public interface CalibrationPersistenceWriter {
    /** 保存曲线并同步累计校准记录数量。 */
    void write(List<CalibrationRecordDetailCache> details);
}
