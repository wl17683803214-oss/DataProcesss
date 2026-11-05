package com.example.dataprocess.service;

import com.example.dataprocess.entity.CollectInterfaceStatisticsRecord;
import com.example.dataprocess.entity.DataProcessingStatisticsRecord;

import java.util.List;

/** 保存采集累计量和采集、处理五秒统计。 */
public interface InterfaceStatisticsWriter {

    /** 将匹配已启用采集接口的HTTP请求计为一次采集。 */
    void recordHttpCollection(String receivePath);

    /** 保存已结束五秒窗口中的全部统计记录。 */
    void write(
            List<CollectInterfaceStatisticsRecord> collectionRecords,
            List<DataProcessingStatisticsRecord> processingRecords);
}
