package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.SysRuntimeLog;
import com.example.dataadmin.vo.collection.CollectionEventOverviewVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志表数据访问接口。
 *
 * SQL统一维护在 SysRuntimeLogMapper.xml 中。
 */
public interface SysRuntimeLogMapper {
    /** 按页面筛选条件查询系统运行日志。 */
    List<SysRuntimeLog> findAll(
            @Param("taskId") String taskId,
            @Param("logLevel") String logLevel,
            @Param("logSource") String logSource,
            @Param("operatorId") String operatorId,
            @Param("keyword") String keyword,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    /** 按试验任务查询非空日志来源。 */
    List<String> findSourceNames(@Param("taskId") String taskId);
    /** 按试验任务ID查询业务数据列表。 */
    List<SysRuntimeLog> findAllByTaskId(@Param("taskId") String taskId);
    /** 按主键查询单条业务数据。 */
    SysRuntimeLog findById(@Param("id") Long id);
    /** 新增业务数据并回填主键。 */
    int insert(SysRuntimeLog entity);
    /** 按主键更新业务数据。 */
    int update(SysRuntimeLog entity);
    /** 按主键删除业务数据。 */
    int delete(@Param("id") Long id);
    /** 删除指定试验任务的全部运行日志。 */
    int deleteByTaskId(@Param("taskId") String taskId);

    /** 查询数据采集相关的最近事件。 */
    List<SysRuntimeLog> findCollectionEvents(
            @Param("taskId") String taskId,
            @Param("limit") Integer limit);

    /** 按事件级别统计数据采集相关事件。 */
    CollectionEventOverviewVO findCollectionEventOverview(
            @Param("taskId") String taskId);
}
