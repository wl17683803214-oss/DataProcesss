package com.example.common.greptime;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 仅由独立时序会话注册，禁止加入金仓的映射扫描。 */
public interface GreptimeMapper {
    /** 自动创建整帧表。 */
    void createFrameTable();
    /** 自动创建参数表。 */
    void createParameterTable();
    /** 幂等写入一条整帧。 */
    int insertFrame(TelemetryFrameRecord frame);
    /** 分批写入遥测参数。 */
    int insertParameters(@Param("rows") List<TelemetryParameterRecord> rows);
    /** 查询正常或异常整帧。 */
    List<TelemetryFrameRecord> frames(Map<String, Object> query);
    /** 统计相同条件下的整帧数量。 */
    long countFrames(Map<String, Object> query);
    /** 查询单个遥测代号的最新值。 */
    TelemetryParameterRecord latest(Map<String, Object> query);
    /** 查询单个遥测代号的最近一百二十个点。 */
    List<TelemetryParameterRecord> curve(Map<String, Object> query);
    /** 删除截止时间之前的整帧。 */
    int deleteFramesBefore(@Param("beforeTime") Timestamp beforeTime);
    /** 删除截止时间之前的参数。 */
    int deleteParametersBefore(@Param("beforeTime") Timestamp beforeTime);
}
