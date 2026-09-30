package com.example.simulator.mapper;

import com.example.simulator.model.*;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 所有数据库访问统一通过映射文件完成。 */
public interface SimulatorMapper {
    /** 在独立数据库会话上获取服务锁，防止不同主机同时管理同一组记录。 */
    boolean acquireServiceLock();
    /** 归还连接池之前显式释放会话锁。 */
    boolean releaseServiceLock();
    /** 读取配置，锁定版本供事务更新使用。 */
    SourceConfig lockSource(@Param("id") long id);
    /** 查询配置详情。 */
    SourceConfig findSource(@Param("id") long id);
    /** 查询协议参数。 */
    PdxpConfig findPdxp(@Param("id") long id);
    /** 查询活动执行数量。 */
    int activeCount(@Param("id") long id);
    /** 保存新配置。 */
    void insertSource(SourceConfig source);
    /** 更新可编辑配置。 */
    void updateSource(SourceConfig source);
    /** 保存协议参数。 */
    void insertPdxp(PdxpConfig config);
    /** 更新协议参数。 */
    void updatePdxp(PdxpConfig config);
    /** 逻辑删除配置。 */
    void deleteSource(@Param("id") long id);
    /** 分页查询配置和最近运行。 */
    List<SourceConfig> pageSources(@Param("name") String name, @Param("status") Integer status,
                                  @Param("offset") long offset, @Param("size") int size);
    /** 统计满足筛选条件的配置数量。 */
    long countSources(@Param("name") String name, @Param("status") Integer status);
    /** 新建运行记录。 */
    void insertRun(RunRecord run);
    /** 保存一次统计快照和运行状态。 */
    void updateRun(RunRecord run);
    /** 读取单次执行。 */
    RunRecord findRun(@Param("id") long id);
    /** 查询历史记录。 */
    List<RunRecord> pageRuns(@Param("id") long id, @Param("offset") long offset, @Param("size") int size);
    /** 统计历史数量。 */
    long countRuns(@Param("id") long id);
    /** 服务恢复时结束上次未退出的运行。 */
    void recoverRuns();
}

