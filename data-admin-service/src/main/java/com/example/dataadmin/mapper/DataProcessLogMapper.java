package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.DataProcessLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 数据处理日志数据访问接口。 */
public interface DataProcessLogMapper {

    /** 按页面筛选条件查询日志，由 Service 使用 PageHelper 完成分页。 */
    List<DataProcessLog> findAll(
            @Param("taskId") String taskId,
            @Param("processTaskId") Long processTaskId,
            @Param("logLevel") Integer logLevel);

    /** 预留日志写入方法，实际产生日志的处理逻辑后续接入。 */
    int insert(DataProcessLog log);
}
