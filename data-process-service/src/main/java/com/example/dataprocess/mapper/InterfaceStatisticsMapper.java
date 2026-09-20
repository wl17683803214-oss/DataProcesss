package com.example.dataprocess.mapper;

import com.example.dataprocess.entity.CollectInterfaceStatisticsRecord;
import com.example.dataprocess.entity.DataProcessingStatisticsRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 采集和处理五秒统计数据访问接口。 */
public interface InterfaceStatisticsMapper {

    /** 批量新增或覆盖采集统计窗口。 */
    int saveCollectionStatistics(
            @Param("records") List<CollectInterfaceStatisticsRecord> records);

    /** 批量新增或覆盖处理统计窗口。 */
    int saveProcessingStatistics(
            @Param("records") List<DataProcessingStatisticsRecord> records);
}
