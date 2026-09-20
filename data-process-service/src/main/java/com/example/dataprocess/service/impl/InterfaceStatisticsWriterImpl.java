package com.example.dataprocess.service.impl;

import com.example.dataprocess.entity.CollectInterfaceStatisticsRecord;
import com.example.dataprocess.entity.DataProcessingStatisticsRecord;
import com.example.dataprocess.mapper.InterfaceStatisticsMapper;
import com.example.dataprocess.service.InterfaceStatisticsWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 采集和处理统计事务写入实现。 */
@Service
public class InterfaceStatisticsWriterImpl implements InterfaceStatisticsWriter {

    /** 统计数据访问组件。 */
    private final InterfaceStatisticsMapper mapper;

    public InterfaceStatisticsWriterImpl(InterfaceStatisticsMapper mapper) {
        this.mapper = mapper;
    }

    /** 两张统计表在同一事务中写入，防止同一窗口只完成一半。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void write(
            List<CollectInterfaceStatisticsRecord> collectionRecords,
            List<DataProcessingStatisticsRecord> processingRecords) {
        if (collectionRecords != null && !collectionRecords.isEmpty()) {
            mapper.saveCollectionStatistics(collectionRecords);
        }
        if (processingRecords != null && !processingRecords.isEmpty()) {
            mapper.saveProcessingStatistics(processingRecords);
        }
    }
}
