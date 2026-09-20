package com.example.dataprocess.service;

import com.example.dataprocess.entity.CollectInterfaceStatisticsRecord;
import com.example.dataprocess.entity.DataProcessingStatisticsRecord;

import java.util.List;

/** 在一个事务中保存同一批次的采集和处理统计。 */
public interface InterfaceStatisticsWriter {

    /** 保存已结束五秒窗口中的全部统计记录。 */
    void write(
            List<CollectInterfaceStatisticsRecord> collectionRecords,
            List<DataProcessingStatisticsRecord> processingRecords);
}
