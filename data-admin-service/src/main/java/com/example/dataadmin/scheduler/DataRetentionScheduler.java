package com.example.dataadmin.scheduler;

import com.example.common.retention.DataRetentionPolicy;
import com.example.dataadmin.enums.DataRetentionCategory;
import com.example.dataadmin.service.DataRetentionCleaner;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每小时分批清理已配置保留天数的业务数据，单轮上限避免持续占用数据库。 */
@Component
public class DataRetentionScheduler {
    /** 单批最多删除一千条，超过单轮上限的数据交给下次任务继续处理。 */
    private static final int BATCH_SIZE = 1000;
    private static final int MAX_BATCHES = 100;
    private final DataRetentionPolicy policy;
    private final DataRetentionCleaner cleaner;

    public DataRetentionScheduler(DataRetentionPolicy policy, DataRetentionCleaner cleaner) {
        this.policy = policy;
        this.cleaner = cleaner;
    }

    /** 不在采集线程执行清理；每类异常独立处理，下一轮重新尝试。 */
    @Scheduled(cron = "0 15 * * * *")
    public void clean() {
        // 第一步：各分类独立读取配置，未填天数时不执行任何删除SQL。
        for (DataRetentionCategory category : DataRetentionCategory.values()) {
            Integer days = category.retentionDays(policy);
            if (days == null) { continue; }
            try {
                LocalDateTime before = LocalDateTime.now().minusDays(days);
                // 第二步：保持单批与单轮上限，未清理完的数据交给下轮继续处理。
                for (int batch = 0; batch < MAX_BATCHES; batch++) {
                    int deleted = cleaner.cleanBatch(category, before, BATCH_SIZE);
                    if (deleted < BATCH_SIZE) {
                        break;
                    }
                }
            } catch (Exception exception) {
                // 单类清理失败不打印日志，继续其他分类并在下轮重试。
            }
        }
    }
}
