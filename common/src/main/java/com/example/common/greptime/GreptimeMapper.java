package com.example.common.greptime;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 仅由独立时序会话注册，禁止加入金仓的映射扫描。 */
public interface GreptimeMapper {
    /** 初始化配置指定的时序数据库。 */
    void createDatabase(@Param("databaseName") String databaseName);
    /** 创建持久化对象分表目录，供跨对象整帧查询定位。 */
    void createTableRegistry();
    /** 自动创建整帧表。 */
    void createFrameTable(@Param("tableName") String tableName);
    /** 自动创建参数表。 */
    void createParameterTable(@Param("tableName") String tableName);
    /** 记录任务、接口与对象两张分表的关联。 */
    void registerTables(Map<String, Object> route);
    /** 从持久目录读取任务下的整帧分表。 */
    List<String> registeredFrameTables(Map<String, Object> query);
    /** 查询当前数据库实际存在的候选分表。 */
    List<String> existingTables(@Param("databaseName") String databaseName, @Param("names") List<String> names);
    /** 枚举当前数据库的对象分表，清理不依赖业务对象是否已删除。 */
    List<String> shardTables(@Param("databaseName") String databaseName);
    /** 幂等写入一条整帧。 */
    int insertFrame(@Param("tableName") String tableName, @Param("frame") TelemetryFrameRecord frame);
    /** 分批写入遥测参数。 */
    int insertParameters(@Param("tableName") String tableName, @Param("rows") List<TelemetryParameterRecord> rows);
    /** 查询正常或异常整帧。 */
    List<TelemetryFrameRecord> frames(Map<String, Object> query);
    /** 一次查询所选参数在时间范围内各自的最新值。 */
    List<TelemetryParameterRecord> latestBatch(Map<String, Object> query);
    /** 一次查询所选参数在时间范围内的全部有效数值点。 */
    List<TelemetryParameterRecord> curves(Map<String, Object> query);
    /** 删除指定对象分表中截止时间之前的记录。 */
    int deleteBefore(@Param("tableName") String tableName, @Param("beforeTime") Timestamp beforeTime);
}
