package com.example.dataadmin.service;

import com.example.common.appstore.AppStoreLogLevel;
import com.example.common.appstore.AppStoreReporter;
import com.example.dataadmin.entity.SysRuntimeLog;
import com.example.dataadmin.enums.SystemLogLevel;
import com.example.dataadmin.enums.SystemLogSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 从管理服务已记录的日志中选择需要上报的内容。 */
@Component
public class AppStoreSystemLogReporter {

    /** 普通信息只上报关键业务模块的操作，警告和错误不受来源限制。 */
    private static final Set<String> IMPORTANT_INFO_SOURCES = new HashSet<String>(Arrays.asList(
            SystemLogSource.COLLECTION.getValue(),
            SystemLogSource.PROCESSING.getValue(),
            SystemLogSource.CALIBRATION.getValue(),
            SystemLogSource.SATELLITE_CATALOG.getValue()));

    private final AppStoreReporter reporter;

    public AppStoreSystemLogReporter(AppStoreReporter reporter) {
        this.reporter = reporter;
    }

    /** 上报中文来源和日志摘要，操作人、请求参数和详细快照保留在本地。 */
    public void reportAfterCommit(SysRuntimeLog log) {
        AppStoreLogLevel level = SystemLogLevel.reportLevelOf(log.getLogLevel());
        String source = log.getLogSource();
        if (level == null || !StringUtils.hasText(log.getLogContent())
                || (level == AppStoreLogLevel.INFO && !IMPORTANT_INFO_SOURCES.contains(source))) {
            return;
        }

        // 用现有来源枚举的中文名称标记模块，提交成功后再进入发送队列。
        String sourceName = SystemLogSource.labelOf(source);
        String summary = sourceName == null ? log.getLogContent() : "【" + sourceName + "】" + log.getLogContent();
        reporter.submitLogAfterCommit(level, summary, log.getLogTime());
    }
}
