package com.example.dataadmin.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 数据处理任务表数据访问接口。
 *
 * SQL统一维护在 DataProcessTaskMapper.xml 中。
 */
public interface DataProcessTaskMapper {
    /** 按试验任务和任务状态统计有效任务数量。 */
    long countByStatus(
            @Param("taskId") String taskId,
            @Param("taskStatus") Integer taskStatus);
}
