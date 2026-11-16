package com.example.common.greptime;

import java.util.List;

/** 将对象分表的业务写入与查询、建表分离，具体传输由处理服务提供。 */
@FunctionalInterface
public interface GreptimeDataWriter {
    /** 方法成功返回表示参数与整帧均收到服务端写入确认，不表示跨表事务。 */
    void write(GreptimeTableRoute route, List<TelemetryFrameRecord> frames, List<TelemetryParameterRecord> parameters);
}
