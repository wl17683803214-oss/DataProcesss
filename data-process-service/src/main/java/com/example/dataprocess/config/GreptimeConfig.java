package com.example.dataprocess.config;

import com.example.common.greptime.GreptimeProperties;
import com.example.common.greptime.GreptimeStore;
import com.example.dataprocess.alarm.DataAlarmRecorder;
import com.example.dataprocess.service.GreptimeBulkWriter;
import com.example.dataprocess.service.GreptimeWriteBuffer;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 独立时序连接和自动建表配置，不替代金仓业务数据源。 */
@Configuration
public class GreptimeConfig {
    /** 读取明确配置值，不依赖环境变量占位表达式。 */
    @Bean
    @ConfigurationProperties(prefix = "greptime")
    public GreptimeProperties greptimeProperties() {
        return new GreptimeProperties();
    }

    /** 批量写入额度使用独立配置，查询连接不承担参数数据传输。 */
    @Bean
    @ConfigurationProperties(prefix = "greptime.bulk")
    public GreptimeBulkProperties greptimeBulkProperties() {
        return new GreptimeBulkProperties();
    }

    /** 批量客户端只在处理服务创建，管理服务不加载写入依赖。 */
    @Bean(destroyMethod = "close")
    public GreptimeBulkWriter greptimeBulkWriter(GreptimeProperties storage, GreptimeBulkProperties bulk) {
        return new GreptimeBulkWriter(storage, bulk);
    }

    /** 启用时创建缺失表和目录，查询与建表复用原有映射。 */
    @Bean(destroyMethod = "close")
    public GreptimeStore greptimeStore(GreptimeProperties properties, GreptimeBulkWriter writer) throws IOException {
        return new GreptimeStore(properties, writer);
    }

    /** 存储启用时创建合并缓冲，销毁顺序保证缓冲先刷完再关闭数据库。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "greptime", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GreptimeWriteBuffer greptimeWriteBuffer(GreptimeStore store, GreptimeBulkProperties bulk,
            DataAlarmRecorder alarmRecorder, TimingLogProperties timingLog) {
        // 合并线程和帧处理使用同一份日志配置。
        return new GreptimeWriteBuffer(store, bulk, alarmRecorder, timingLog);
    }
}
