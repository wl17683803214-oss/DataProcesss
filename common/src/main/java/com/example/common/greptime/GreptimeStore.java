package com.example.common.greptime;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

/** 对象分表的独立访问组件，参数与整帧分别保存，全部语句由公共映射执行。 */
public class GreptimeStore implements AutoCloseable {
    /** 入库失败继续使用已有中文告警链路。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(GreptimeStore.class);
    /** 当前服务的独立连接配置。 */
    private final GreptimeProperties properties;
    /** 管理服务仅查询，处理服务注入独立的批量写入组件。 */
    private final GreptimeDataWriter writer;
    /** 目录和元数据查询限定的数据库。 */
    private final String databaseName;
    /** 不注册到业务库事务的数据源。 */
    private final HikariDataSource dataSource;
    /** 只加载公共时序映射的会话工厂。 */
    private final SqlSessionFactory factory;
    /** 只缓存实际存在的分表，不缓存不存在结果，避免新对象不可见。 */
    private final Set<String> existingTables = ConcurrentHashMap.newKeySet();
    /** 本实例已经完成建表的对象参数表。 */
    private final Set<String> initializedObjects = ConcurrentHashMap.newKeySet();
    /** 本实例已经登记的任务、接口和对象组合。 */
    private final Set<List<Object>> registeredSources = ConcurrentHashMap.newKeySet();
    /** 只在首次建表或登记时串行，普通写入不持有此锁。 */
    private final Object initializationLock = new Object();

    /** 启用自动创建时，先创建目标数据库和目录，对象表延迟到首次写入。 */
    public GreptimeStore(GreptimeProperties properties) throws IOException {
        this(properties, null);
    }

    /** 复用相同目录和建表逻辑，业务数据交给指定传输组件写入。 */
    public GreptimeStore(GreptimeProperties properties, GreptimeDataWriter writer) throws IOException {
        properties.validate();
        this.properties = properties;
        this.writer = writer;
        this.databaseName = databaseName(properties.getUrl());
        // 第一步：关闭时不创建连接，沿用既有仅处理不落库模式。
        if (!properties.isEnabled()) {
            dataSource = null;
            factory = null;
            return;
        }
        // 第二步：连接目标数据库，启动时不预先创建任何旧公共表。
        dataSource = pool(properties.getUrl(), properties.getMaxPoolSize(), "telemetry-timeseries");
        try {
            factory = sessionFactory(dataSource);
            if (properties.isAutoCreateTables()) {
                initializeDatabase();
                execute(mapper -> {
                    mapper.createTableRegistry();
                    mapper.createDailyStatisticsTable();
                    return null;
                });
            }
        } catch (IOException | RuntimeException exception) {
            dataSource.close();
            throw exception;
        }
    }

    /** 首次建表和目录登记完成后，按稳定身份和时间重试同一批数据。 */
    public void save(TelemetryFrameRecord frame, List<TelemetryParameterRecord> parameters) {
        // 单帧验证和离线入口继续复用同一个跨帧写入实现。
        saveBatch(Collections.singletonList(frame), parameters);
    }

    /** 同任务同对象的多帧共用一次写入流程，记录时间保持各帧原始值。 */
    public void saveBatch(List<TelemetryFrameRecord> frames, List<TelemetryParameterRecord> parameters) {
        // 只读服务和关闭存储的部署不能误调用写入入口。
        if (!properties.isEnabled() || writer == null) {
            throw new IllegalStateException("遥测数据库未启用或当前组件未配置写入器");
        }
        // 第一步：先校验全部帧和参数，避免错误路由导致跨对象或跨任务污染。
        if (frames == null || frames.isEmpty() || parameters == null) {
            throw new IllegalArgumentException("遥测批量写入必须包含整帧列表和参数列表");
        }
        TelemetryFrameRecord frame = frames.get(0);
        GreptimeTableRoute route = GreptimeTableRoute.of(frame.getSourceType(), frame.getSourceIdentity());
        for (TelemetryFrameRecord row : frames) {
            if (!Objects.equals(frame.getTaskId(), row.getTaskId())
                    || !Objects.equals(frame.getSourceType(), row.getSourceType())
                    || !Objects.equals(frame.getSourceIdentity(), row.getSourceIdentity())) {
                throw new IllegalArgumentException("同批整帧的任务或对象身份不一致");
            }
        }
        for (TelemetryParameterRecord row : parameters) {
            if (!Objects.equals(frame.getTaskId(), row.getTaskId())
                    || !Objects.equals(frame.getSourceType(), row.getSourceType())
                    || !Objects.equals(frame.getSourceIdentity(), row.getSourceIdentity())) {
                throw new IllegalArgumentException("同批遥测参数与整帧的任务或对象身份不一致");
            }
        }
        for (int attempt = 1; attempt <= properties.getWriteAttempts(); attempt++) {
            try {
                // 第二步：仅首次写入使用查询连接登记对象，普通写入不再申请数据库连接。
                Map<List<Object>, TelemetryFrameRecord> missingSources = new LinkedHashMap<>();
                for (TelemetryFrameRecord row : frames) {
                    List<Object> sourceKey = Arrays.asList(row.getTaskId(), row.getInterfaceId(),
                            row.getSourceType(), row.getSourceIdentity());
                    if (!registeredSources.contains(sourceKey)) {
                        missingSources.putIfAbsent(sourceKey, row);
                    }
                }
                if ((properties.isAutoCreateTables() && !initializedObjects.contains(route.parameterTable()))
                        || !missingSources.isEmpty()) {
                    execute(mapper -> {
                        initializeObject(mapper, route);
                        for (TelemetryFrameRecord row : missingSources.values()) {
                            registerSource(mapper, route, row);
                        }
                        return null;
                    });
                }
                // 第三步：等待批量写入器的服务端确认，保持现有入库耗时和告警口径。
                writer.write(route, frames, parameters);
                return;
            } catch (RuntimeException exception) {
                // 第四步：达到重试上限后交给已有异步告警，不改变固定入库时间。
                if (attempt == properties.getWriteAttempts() || Thread.currentThread().isInterrupted()) {
                    throw exception;
                }
                LOGGER.warn("遥测分表写入失败，准备重试，当前次数：{}，任务：{}，对象：{}",
                        attempt, frame.getTaskId(), frame.getSourceIdentity());
            }
        }
    }

    /** 从任务目录定位所有对象整帧表，再以一条数据语句统一排序取一百条。 */
    public List<TelemetryFrameRecord> frames(Map<String, Object> query) {
        List<String> names = execute(mapper -> mapper.registeredFrameTables(query));
        List<Map<String, Object>> tables = new ArrayList<>();
        for (String name : names) {
            // 目录中的值也经过名称校验，不依赖当前业务对象是否已删除。
            Map<String, Object> table = new HashMap<>();
            table.put("tableName", GreptimeTableRoute.requireTable(name));
            tables.add(table);
        }
        Map<String, Object> routed = existingQuery(query, tables);
        if (((List<?>) routed.get("tables")).isEmpty()) {
            return Collections.emptyList();
        }
        return execute(mapper -> mapper.frames(routed));
    }

    /** 一条联合数据语句读取各对象参数的最新值，未创建的表不参与查询。 */
    public List<TelemetryParameterRecord> latestBatch(Map<String, Object> query) {
        Map<String, Object> routed = parameterQuery(query);
        if (((List<?>) routed.get("tables")).isEmpty()) {
            return Collections.emptyList();
        }
        return execute(mapper -> mapper.latestBatch(routed));
    }

    /** 一条联合数据语句读取各对象曲线，管理服务组装原有时间数值数组。 */
    public List<TelemetryParameterRecord> curves(Map<String, Object> query) {
        Map<String, Object> routed = parameterQuery(query);
        if (((List<?>) routed.get("tables")).isEmpty()) {
            return Collections.emptyList();
        }
        return execute(mapper -> mapper.curves(routed));
    }

    /** 清理实际存在的全部对象分表，旧公共表和持久目录保持原状。 */
    public void deleteBefore(long beforeTime) {
        if (beforeTime <= 0) {
            throw new IllegalArgumentException("遥测清理截止时间必须大于零");
        }
        List<String> names = execute(mapper -> mapper.shardTables(databaseName));
        RuntimeException failure = null;
        for (String name : names) {
            if (!GreptimeTableRoute.isShardTable(name)) {
                continue;
            }
            try {
                // 每张表独立执行，单表失败不阻止其他对象清理，下次任务可重试。
                execute(mapper -> mapper.deleteBefore(name, new Timestamp(beforeTime)));
            } catch (RuntimeException exception) {
                // 保留失败状态供调用方判断，清理流程不再主动打印日志。
                failure = exception;
            }
        }
        if (failure != null) {
            throw new IllegalStateException("部分遥测分表清理失败，下轮继续重试", failure);
        }
    }

    /** 按对象把勾选条件归并到各自分表，减少联合查询的分支数量。 */
    public int calculateDailyStatistics(Timestamp startTime, Timestamp endTime) {
        if (startTime == null || endTime == null || !startTime.before(endTime)) {
            throw new IllegalArgumentException("统计时间范围不正确");
        }
        List<String> primaryKey = execute(mapper -> mapper.dailyStatisticsPrimaryKey(databaseName));
        if (!primaryKey.equals(Arrays.asList("task_id", "satellite_code", "telemetry_code", "telemetry_status"))) {
            throw new IllegalStateException("每日统计表主键不正确，请先迁移旧表，避免不同状态的结果互相覆盖");
        }
        Map<List<String>, DailyTelemetryStatistics> merged = new LinkedHashMap<>();
        for (String table : execute(mapper -> mapper.shardTables(databaseName))) {
            if (!GreptimeTableRoute.isShardTable(table) || !table.startsWith("telemetry_parameter_")) {
                continue;
            }
            List<DailyTelemetryStatistics> rows = execute(mapper -> mapper.dailyStatistics(table, startTime, endTime));
            for (DailyTelemetryStatistics row : rows) {
                DailyTelemetryStatistics existing = merged.putIfAbsent(row.key(), row);
                if (existing != null) {
                    existing.merge(row);
                }
            }
        }
        List<DailyTelemetryStatistics> rows = new ArrayList<>(merged.values());
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());
        for (int offset = 0; offset < rows.size(); offset += 500) {
            List<DailyTelemetryStatistics> batch = rows.subList(offset, Math.min(offset + 500, rows.size()));
            execute(mapper -> {
                mapper.insertDailyStatistics(startTime, createdAt, batch);
                return null;
            });
        }
        return rows.size();
    }

    private Map<String, Object> parameterQuery(Map<String, Object> query) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        Object filters = query.get("filters");
        if (filters instanceof List) {
            for (Object item : (List<?>) filters) {
                if (!(item instanceof Map)) {
                    throw new IllegalArgumentException("遥测参数筛选条件不完整");
                }
                Map<?, ?> filter = (Map<?, ?>) item;
                String name = GreptimeTableRoute.of((String) filter.get("sourceType"),
                        (String) filter.get("sourceIdentity")).parameterTable();
                // 先创建对象分支，再把该对象的勾选代号放入同一组条件。
                Map<String, Object> table = grouped.computeIfAbsent(name, ignored -> {
                    Map<String, Object> branch = new HashMap<>();
                    branch.put("tableName", name);
                    branch.put("filters", new ArrayList<Map<?, ?>>());
                    return branch;
                });
                addFilter(table, filter);
            }
        }
        return existingQuery(query, new ArrayList<>(grouped.values()));
    }

    /** 内部列表只接收已经校验为映射的筛选项。 */
    @SuppressWarnings("unchecked")
    private void addFilter(Map<String, Object> table, Map<?, ?> filter) {
        ((List<Map<?, ?>>) table.get("filters")).add(filter);
    }

    /** 只缓存存在的对象表，缺表时每次重新探测，让其他实例新建的表立即可见。 */
    private Map<String, Object> existingQuery(Map<String, Object> query, List<Map<String, Object>> tables) {
        List<String> missing = new ArrayList<>();
        for (Map<String, Object> table : tables) {
            String name = GreptimeTableRoute.requireTable((String) table.get("tableName"));
            if (!existingTables.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            existingTables.addAll(execute(mapper -> mapper.existingTables(databaseName, missing)));
        }
        List<Map<String, Object>> available = new ArrayList<>();
        for (Map<String, Object> table : tables) {
            if (existingTables.contains(table.get("tableName"))) {
                available.add(table);
            }
        }
        Map<String, Object> routed = new HashMap<>(query);
        routed.put("tables", available);
        return routed;
    }

    /** 并发首次写入只在本实例建表一次，跨实例依靠数据库的幂等建表。 */
    private void initializeObject(GreptimeMapper mapper, GreptimeTableRoute route) {
        if (!properties.isAutoCreateTables() || initializedObjects.contains(route.parameterTable())) {
            return;
        }
        synchronized (initializationLock) {
            if (!initializedObjects.contains(route.parameterTable())) {
                mapper.createParameterTable(route.parameterTable());
                mapper.createFrameTable(route.frameTable());
                existingTables.add(route.parameterTable());
                existingTables.add(route.frameTable());
                initializedObjects.add(route.parameterTable());
            }
        }
    }

    /** 持久目录按任务和接口登记，同一卫星跨任务仍复用同一对物理表。 */
    private void registerSource(GreptimeMapper mapper, GreptimeTableRoute route, TelemetryFrameRecord frame) {
        List<Object> key = Arrays.asList(frame.getTaskId(), frame.getInterfaceId(),
                frame.getSourceType(), frame.getSourceIdentity());
        if (registeredSources.contains(key)) {
            return;
        }
        synchronized (initializationLock) {
            if (!registeredSources.contains(key)) {
                Map<String, Object> registration = new HashMap<>();
                // 固定时间键保证目录在实例重启和重复登记时仍为同一行。
                registration.put("registryTime", new Timestamp(0L));
                registration.put("taskId", frame.getTaskId());
                registration.put("interfaceId", frame.getInterfaceId());
                registration.put("sourceType", frame.getSourceType());
                registration.put("sourceIdentity", frame.getSourceIdentity());
                registration.put("parameterTable", route.parameterTable());
                registration.put("frameTable", route.frameTable());
                mapper.registerTables(registration);
                registeredSources.add(key);
            }
        }
    }

    /** 使用不指定数据库的连接初始化目标库，不向旧公共表写入任何数据。 */
    private void initializeDatabase() throws IOException {
        String url = properties.getUrl();
        int path = url.indexOf('/', url.indexOf("://") + 3);
        int options = url.indexOf('?', path);
        String bootstrapUrl = url.substring(0, path + 1) + (options < 0 ? "" : url.substring(options));
        try (HikariDataSource bootstrap = pool(bootstrapUrl, 1, "telemetry-database-init")) {
            try (SqlSession session = sessionFactory(bootstrap).openSession(true)) {
                session.getMapper(GreptimeMapper.class).createDatabase(databaseName);
            }
        }
    }

    /** 从连接路径读取数据库名，元数据查询与实际连接保持一致。 */
    private String databaseName(String url) {
        String path = URI.create(url.substring("jdbc:".length())).getPath();
        if (path == null || !path.matches("/[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("遥测连接必须明确配置有效数据库名称");
        }
        return path.substring(1);
    }

    /** 创建独立连接池，初始化连接和业务读写共用相同认证信息。 */
    private HikariDataSource pool(String url, int maximum, String name) {
        HikariConfig pool = new HikariConfig();
        pool.setJdbcUrl(url);
        pool.setUsername(properties.getUsername());
        pool.setPassword(properties.getPassword());
        pool.setMaximumPoolSize(maximum);
        pool.setMinimumIdle(0);
        pool.setConnectionTimeout(properties.getConnectionTimeoutMillis());
        pool.setInitializationFailTimeout(-1);
        pool.setAutoCommit(true);
        pool.setPoolName(name);
        return new HikariDataSource(pool);
    }

    /** 加载同一份映射，避免建库连接绕过既有访问规范。 */
    private SqlSessionFactory sessionFactory(HikariDataSource source) throws IOException {
        Configuration configuration = new Configuration(new Environment("telemetry",
                new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        configuration.setDefaultStatementTimeout(properties.getQueryTimeoutSeconds());
        String resource = "greptime/GreptimeMapper.xml";
        try (InputStream input = GreptimeStore.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("找不到遥测数据库映射文件");
            }
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            return new SqlSessionFactoryBuilder().build(configuration);
        }
    }

    /** 每次数据操作独占会话，不向时序服务提交跨表事务。 */
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

