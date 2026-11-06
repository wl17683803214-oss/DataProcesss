package com.example.dataadmin.service.support;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/** 四个遥测查询接口共用的时间范围，缺省查询结束时间之前十五分钟。 */
public final class TelemetryQueryTimeRange {
    /** 默认查询窗口的分钟数。 */
    private static final int DEFAULT_MINUTES = 15;
    /** 包含在查询范围内的开始时间。 */
    private final LocalDateTime startTime;
    /** 包含在查询范围内的结束时间。 */
    private final LocalDateTime endTime;

    /** 保存已经校验的时间边界，避免同次查询重复取当前时间。 */
    private TelemetryQueryTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** 补齐可选时间，并拒绝开始时间晚于结束时间的范围。 */
    public static TelemetryQueryTimeRange resolve(LocalDateTime startTime, LocalDateTime endTime) {
        // 第一步：仅在缺少结束时间时取一次当前时间。
        LocalDateTime resolvedEnd = endTime == null ? LocalDateTime.now() : endTime;
        // 第二步：缺少开始时间时，以最终结束时间向前推十五分钟。
        LocalDateTime resolvedStart = startTime == null ? resolvedEnd.minusMinutes(DEFAULT_MINUTES) : startTime;
        // 第三步：校验先后顺序，允许查询同一时刻的记录。
        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
        return new TelemetryQueryTimeRange(resolvedStart, resolvedEnd);
    }

    /** 转换为与现有存储一致的时间戳，交给映射绑定参数。 */
    public void applyTo(Map<String, Object> query) {
        // 两个边界共用已固定的本次查询范围，不在执行数据库操作时重新计时。
        query.put("startTime", Timestamp.valueOf(startTime));
        query.put("endTime", Timestamp.valueOf(endTime));
    }
}
