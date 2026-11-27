package com.example.dataprocess.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 当前运行中试验任务查询，查询语句统一维护在映射文件。 */
@Mapper
public interface CurrentExperimentTaskMapper {
    /** 按开始事件时间选择最新任务，相同时间按任务编号稳定排序。 */
    String findLatestRunningTaskId(@Param("status") Integer status);
}
