package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.CalibRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 校准记录表数据访问接口。
 *
 * SQL统一维护在 CalibRecordMapper.xml 中。
 */
public interface CalibRecordMapper {
    /** 按试验任务汇总全部有效校准记录的野值数量。 */
    long sumOutlierCount(@Param("taskId") String taskId);
    /** 按试验任务ID查询业务数据列表。 */
    List<CalibRecord> findAllByTaskId(@Param("taskId") String taskId);
    /** 按任务和主键查询单条业务数据。 */
    CalibRecord findByIdAndTaskId(
            @Param("id") Long id,
            @Param("taskId") String taskId);
    /** 新增业务数据并回填主键。 */
    int insert(CalibRecord entity);
    /** 按校准通道同步汇总记录中的任务和通道名称。 */
    int updateSummaryByChannelId(
            @Param("channelId") Long channelId,
            @Param("taskId") String taskId,
            @Param("channelName") String channelName);
    /** 按校准通道逻辑删除汇总记录。 */
    int deleteByChannelId(@Param("channelId") Long channelId);
}
