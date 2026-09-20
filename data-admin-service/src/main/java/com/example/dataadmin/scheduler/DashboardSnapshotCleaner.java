package com.example.dataadmin.scheduler;

import com.example.dataadmin.mapper.DashboardMapper;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 每天逻辑删除超过保留期限的五秒统计记录，默认保留7天。 */
@Component
public class DashboardSnapshotCleaner {
  /** 快照保留天数。 */
  private static final int RETENTION_DAYS = 7;

  private final DashboardMapper mapper;

  public DashboardSnapshotCleaner(DashboardMapper mapper) {
    this.mapper = mapper;
  }

  /** 同一事务内清理采集、处理两张统计表中的过期数据。 */
  @Scheduled(cron = "0 45 3 * * *")
  @Transactional
  public void clean() {
    LocalDateTime before = LocalDateTime.now().minusDays(RETENTION_DAYS);
    mapper.deleteCollectionStatisticsBefore(before);
    mapper.deleteProcessingStatisticsBefore(before);
  }
}
