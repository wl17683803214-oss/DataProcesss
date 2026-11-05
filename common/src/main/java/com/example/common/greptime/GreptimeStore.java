package com.example.common.greptime;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 独立时序数据访问组件，不参与金仓事务或分页拦截器。 */
public class GreptimeStore implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreptimeStore.class);
    private final GreptimeProperties properties;
    private final HikariDataSource dataSource;
    private final SqlSessionFactory factory;

    /** 加载独立映射；启用自动建表时启动阶段即检查连接和建表权限。 */
    public GreptimeStore(GreptimeProperties properties) throws IOException {
        properties.validate();
        this.properties = properties;
        // 第一步：禁用时不创建连接池，保留处理服务原有的仅处理不落库模式。
        if (!properties.isEnabled()) {
            dataSource = null;
            factory = null;
            return;
        }
        // 第二步：连接池封装在组件内部，避免影响金仓数据源的自动装配。
        HikariConfig pool = new HikariConfig();
        pool.setJdbcUrl(properties.getUrl());
        pool.setUsername(properties.getUsername());
        pool.setPassword(properties.getPassword());
        pool.setMaximumPoolSize(properties.getMaxPoolSize());
        pool.setMinimumIdle(0);
        pool.setConnectionTimeout(properties.getConnectionTimeoutMillis());
        pool.setInitializationFailTimeout(-1);
        pool.setAutoCommit(true);
        pool.setPoolName("telemetry-timeseries");
        dataSource = new HikariDataSource(pool);
        // 第三步：独立加载公共映射，不复用金仓的会话工厂和事务管理器。
        Configuration configuration = new Configuration(new Environment("telemetry",
                new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        configuration.setDefaultStatementTimeout(properties.getQueryTimeoutSeconds());
        String resource = "greptime/GreptimeMapper.xml";
        try (InputStream input = GreptimeStore.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("找不到遥测数据库映射文件");
            }
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            factory = new SqlSessionFactoryBuilder().build(configuration);
            // 第四步：两张表共用固定结构，重复启动不清空或覆盖已有表。
            if (properties.isAutoCreateTables()) {
                execute(mapper -> {
                    mapper.createFrameTable();
                    mapper.createParameterTable();
                    return null;
                });
            }
        } catch (IOException | RuntimeException exception) {
            dataSource.close();
            throw exception;
        }
    }

    /** 无跨表事务能力，失败时以完全相同的身份和时间有限重试整批数据。 */
    public void save(TelemetryFrameRecord frame, List<TelemetryParameterRecord> parameters) {
        // 第一步：处理时间和源时间由调用方固定，重试不产生额外时间点。
        for (int attempt = 1; attempt <= properties.getWriteAttempts(); attempt++) {
            try {
                execute(mapper -> {
                    // 第二步：先写参数，全部成功后写整帧，降低正常帧已出现但参数未齐的概率。
                    for (int start = 0; start < parameters.size(); start += properties.getBatchSize()) {
                        mapper.insertParameters(parameters.subList(start,
                                Math.min(start + properties.getBatchSize(), parameters.size())));
                    }
                    mapper.insertFrame(frame);
                    return null;
                });
                return;
            } catch (RuntimeException exception) {
                // 第三步：达到次数或线程关闭时交由已有异步入库告警记录最终失败。
                if (attempt == properties.getWriteAttempts() || Thread.currentThread().isInterrupted()) {
                    throw exception;
                }
                LOGGER.warn("遥测数据写入失败，准备重试，当前次数：{}，任务：{}，源时间：{}",
                        attempt, frame.getTaskId(), frame.getSourceTime());
            }
        }
    }

    /** 使用同一套绑定参数查询整帧。 */
    public List<TelemetryFrameRecord> frames(Map<String, Object> query) {
        return execute(mapper -> mapper.frames(query));
    }

    /** 统计整帧数量。 */
    public long countFrames(Map<String, Object> query) {
        return execute(mapper -> mapper.countFrames(query));
    }

    /** 查询参数最新值。 */
    public TelemetryParameterRecord latest(Map<String, Object> query) {
        return execute(mapper -> mapper.latest(query));
    }

    /** 查询参数曲线。 */
    public List<TelemetryParameterRecord> curve(Map<String, Object> query) {
        return execute(mapper -> mapper.curve(query));
    }

    /** 两张表独立清理，可在部分完成后安全重入。 */
    public void deleteBefore(long beforeTime) {
        if (beforeTime <= 0) {
            throw new IllegalArgumentException("遥测清理截止时间必须大于零");
        }
        execute(mapper -> {
            Timestamp cutoff = new Timestamp(beforeTime);
            mapper.deleteParametersBefore(cutoff);
            mapper.deleteFramesBefore(cutoff);
            return null;
        });
    }

    /** 每次调用独占会话，自动提交且不向不支持事务的时序服务发送提交或回滚。 */
    private <T> T execute(Function<GreptimeMapper, T> action) {
        if (factory == null) {
            throw new IllegalStateException("遥测数据库未启用");
        }
        try (SqlSession session = factory.openSession(true)) {
            return action.apply(session.getMapper(GreptimeMapper.class));
        }
    }

    /** 随服务关闭释放连接池。 */
    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
