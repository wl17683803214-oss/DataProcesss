package com.example.dataprocess.scheduler;

import com.example.common.greptime.GreptimeStore;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 按业务日统计各遥测参数的每种状态。 */
@Component
@ConditionalOnProperty(prefix = "greptime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DailyTelemetryStatisticsScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyTelemetryStatisticsScheduler.class);
    private final GreptimeStore store;
    private final boolean enabled;
    private final ZoneId zone;

    public DailyTelemetryStatisticsScheduler(GreptimeStore store,
            @Value("${telemetry.daily-statistics.enabled:true}") boolean enabled,
            @Value("${telemetry.daily-statistics.zone:Asia/Shanghai}") String zone) {
        this.store = store;
        this.enabled = enabled;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${telemetry.daily-statistics.cron:0 0 1 * * ?}",
            zone = "${telemetry.daily-statistics.zone:Asia/Shanghai}")
    public void calculateYesterday() {
        if (!enabled) {
            return;
        }
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        try {
            Timestamp start = Timestamp.from(yesterday.atStartOfDay(zone).toInstant());
            Timestamp end = Timestamp.from(today.atStartOfDay(zone).toInstant());
            int rows = store.calculateDailyStatistics(start, end);
            LOGGER.info("遥测每日统计完成，统计日期={}，记录数={}", yesterday, rows);
        } catch (RuntimeException exception) {
            LOGGER.error("遥测每日统计失败，统计日期={}", yesterday, exception);
        }
    }
}
