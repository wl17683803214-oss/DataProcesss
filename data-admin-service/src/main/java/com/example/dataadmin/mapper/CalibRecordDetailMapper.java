package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.CalibRecordDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 校准记录野值详情数据访问接口。 */
public interface CalibRecordDetailMapper {
    /** 按任务和校准通道查询未删除的野值详情。 */
    List<CalibRecordDetail> findByTaskIdAndChannelId(
            @Param("taskId") String taskId,
            @Param("channelId") Long channelId);
    /** 按校准通道逻辑删除全部野值详情。 */
    int deleteByChannelId(@Param("channelId") Long channelId);
}
