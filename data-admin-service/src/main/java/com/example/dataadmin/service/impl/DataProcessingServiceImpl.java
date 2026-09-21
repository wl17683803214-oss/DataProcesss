package com.example.dataadmin.service.impl;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.DataProcessLog;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.DeviceSatellite;
import com.example.dataadmin.mapper.DeviceSatelliteMapper;
import com.example.dataadmin.mapper.IoTDBFrameQueryMapper;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.service.support.ParameterWorkbookParser;
import com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO;
import com.example.dataadmin.entity.ProcessingRuleConfig;
import com.example.dataadmin.entity.InvalidTelemetryFrame;
import com.example.dataadmin.enums.DataProcessLogLevel;
import com.example.dataadmin.enums.TelemetryFrameCheckResult;
import com.example.dataadmin.mapper.DataProcessLogMapper;
import com.example.dataadmin.mapper.DashboardMapper;
import com.example.dataadmin.mapper.CollectInterfaceConfigMapper;
import com.example.dataadmin.mapper.TelemetryParseRuleConfigMapper;
import com.example.dataadmin.mapper.ProcessingRuleConfigMapper;
import com.example.dataadmin.service.DataProcessingService;
import com.example.dataadmin.service.CollectInterfaceRuntimeSyncService;
import com.example.dataadmin.service.VisualizationParameterSyncService;
import com.example.dataadmin.vo.processing.DataProcessingOverviewVO;
import com.example.dataadmin.vo.processing.DataProcessingRealtimeVO;
import com.example.dataadmin.vo.processing.CollectInterfaceOptionVO;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.isession.pool.SessionDataSetWrapper;
import org.apache.iotdb.session.pool.SessionPool;
import org.apache.tsfile.utils.Binary;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.sql.Timestamp;

/**
 * 数据处理页面业务实现。
 *
 * 查询默认使用只读事务，新增、修改和删除操作单独开启写事务。
 */
@Service
@Transactional(readOnly = true)
public class DataProcessingServiceImpl implements DataProcessingService {

    private static final int IMPORT_BATCH_SIZE = 500;
    /** 任务下各采集接口实时处理速率Redis键前缀。 */
    private static final String PROCESSING_CURRENT_PREFIX =
            "dashboard:processing:current:";
    /** 数据处理服务CPU和内存运行指标Redis键。 */
    private static final String PROCESSING_RUNTIME_KEY =
            "dashboard:processing:runtime";
    /** 一个GB对应的字节数。 */
    private static final double BYTES_PER_GB = 1024D * 1024D * 1024D;

    /** 数据处理统计数据访问组件。 */
    private final DashboardMapper dashboardMapper;

    /** 采集接口筛选项数据访问组件。 */
    private final CollectInterfaceConfigMapper collectInterfaceMapper;

    /** 数据处理日志数据访问组件。 */
    private final DataProcessLogMapper logMapper;

    /** 遥测解析规则数据访问组件。 */
    private final TelemetryParseRuleConfigMapper parseRuleMapper;
    private final ProcessingRuleConfigMapper processingRuleConfigMapper;
    /** 采集接口运行配置同步服务。 */
    private final CollectInterfaceRuntimeSyncService runtimeSyncService;
    private final SessionPool iotdbSessionPool;
    /** 正常整帧查询的MyBatis映射组件。 */
    private final IoTDBFrameQueryMapper frameQueryMapper;
    /** 可视化参数筛选项同步服务。 */
    private final VisualizationParameterSyncService parameterSyncService;

    /** 设备卫星关联与替换锁。 */
    private final DeviceSatelliteMapper deviceSatelliteMapper;
    /** 工作簿读取和字段校验。 */
    private final ParameterWorkbookParser workbookParser;
    /** Redis字符串访问组件。 */
    private final StringRedisTemplate redisTemplate;
    /** 实时指标JSON读取组件。 */
    private final ObjectMapper objectMapper;

    public DataProcessingServiceImpl(
            DashboardMapper dashboardMapper,
            CollectInterfaceConfigMapper collectInterfaceMapper,
            DataProcessLogMapper logMapper,
            TelemetryParseRuleConfigMapper parseRuleMapper,
            ProcessingRuleConfigMapper processingRuleConfigMapper,
            CollectInterfaceRuntimeSyncService runtimeSyncService,
            SessionPool iotdbQuerySessionPool,
            VisualizationParameterSyncService parameterSyncService,
            DeviceSatelliteMapper deviceSatelliteMapper,
            ParameterWorkbookParser workbookParser,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            IoTDBFrameQueryMapper frameQueryMapper) {
        this.dashboardMapper = dashboardMapper;
        this.collectInterfaceMapper = collectInterfaceMapper;
        this.logMapper = logMapper;
        this.parseRuleMapper = parseRuleMapper;
        this.processingRuleConfigMapper = processingRuleConfigMapper;
        this.runtimeSyncService = runtimeSyncService;
        this.iotdbSessionPool = iotdbQuerySessionPool;
        this.parameterSyncService = parameterSyncService;
        this.deviceSatelliteMapper = deviceSatelliteMapper;
        this.workbookParser = workbookParser;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.frameQueryMapper = frameQueryMapper;
    }

    /** 查询页面筛选框使用的已启用采集接口。 */
    @Override
    public List<CollectInterfaceOptionVO> listEnabledCollectInterfaces() {
        // 仅返回接口主键和名称，避免向页面暴露无关配置字段。
        return collectInterfaceMapper.findEnabledOptions();
    }

    /** 查询页面顶部统计卡片。 */
    @Override
    public DataProcessingOverviewVO getOverview(String taskId) {
        requireTaskId(taskId);
        // 直接从五秒统计表汇总，空结果由SQL统一转换为零。
        return dashboardMapper.findProcessingOverview(taskId);
    }

    /** 查询实时处理指标。 */
    @Override
    public DataProcessingRealtimeVO getRealtime(String taskId) {
        requireTaskId(taskId);
        DataProcessingRealtimeVO result = new DataProcessingRealtimeVO();
        try {
            // 第一步：汇总当前任务下所有接口上一秒成功处理的PDXP条数。
            long processRate = processingRate(taskId);
            result.setProcessRate(processRate + " 条/秒");
            // 第二步：读取数据处理服务最新的进程CPU和JVM内存指标。
            applyRuntimeMetric(result);
        } catch (RuntimeException ignored) {
            // Redis暂时不可用或缓存内容异常时返回带单位的零值，保证页面可用。
        }
        return result;
    }

    /** 汇总任务下所有采集接口上一秒的成功处理条数。 */
    private long processingRate(String taskId) {
        Map<Object, Object> metrics = redisTemplate.opsForHash().entries(
                PROCESSING_CURRENT_PREFIX + taskId);
        long total = 0L;
        for (Object value : metrics.values()) {
            try {
                // 每个接口独立解析，单条异常数据不影响其他接口的统计结果。
                JsonNode metric = objectMapper.readTree(String.valueOf(value));
                total += Math.max(0L, metric.path("processRate").asLong(0L));
            } catch (Exception ignored) {
                // 非当前统计组件写入的无效字段不参与本次汇总。
            }
        }
        return total;
    }

    /** 把数据处理服务运行指标换算为带单位的页面字段。 */
    private void applyRuntimeMetric(DataProcessingRealtimeVO result) {
        String value = redisTemplate.opsForValue().get(PROCESSING_RUNTIME_KEY);
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            JsonNode metric = objectMapper.readTree(value);
            // CPU统一保留两位小数并直接追加百分号。
            double cpuUsage = Math.max(
                    0D, metric.path("cpuUsagePercent").asDouble(0D));
            result.setCpuUsage(String.format(
                    Locale.ROOT, "%.2f%%", cpuUsage));
            // JVM已使用字节数换算为GB后保留两位小数。
            long memoryBytes = Math.max(
                    0L, metric.path("memoryUsageBytes").asLong(0L));
            result.setMemoryUsage(String.format(
                    Locale.ROOT,
                    "%.2f GB",
                    memoryBytes / BYTES_PER_GB));
        } catch (Exception ignored) {
            // 缓存格式异常时沿用VO中的带单位零值。
        }
    }

    @Override
    public List<RealtimeTelemetryFrameVO> listRealtimeTelemetryFrames(
            String taskId,
            Long interfaceId) {
        // 第一步：任务编号必须明确，防止查询到其他试验任务的数据。
        String normalizedTaskId = requireIoTDBTaskId(taskId);
        // 第二步：复用字面量转义，由MyBatis映射生成带可选接口条件的查询。
        String sql = frameQueryMapper.realtimeFrames(
                iotdbTextLiteral(normalizedTaskId), interfaceId);
        // 第三步：查询完整原始帧及新增的卫星名称、通道编码。
        List<RealtimeTelemetryFrameVO> result = new ArrayList<>();
        try (SessionDataSetWrapper dataSet =
                     iotdbSessionPool.executeQueryStatement(sql)) {
            SessionDataSet.DataIterator iterator = dataSet.iterator();
            while (iterator.next()) {
                Binary raw = iterator.getBlob("raw");
                RealtimeTelemetryFrameVO item =
                        new RealtimeTelemetryFrameVO();
                item.setInterfaceId(nullableLong(iterator, "interfaceId"));
                Timestamp time = iterator.getTimestamp("Time");
                item.setTime(time == null ? null : time.toLocalDateTime());
                item.setSatelliteCode(iterator.getString("satelliteCode"));
                // 旧设备可能尚未创建新测点，列缺失或值为空时均返回空值。
                item.setSatelliteName(dataSet.getColumnNames().contains("satelliteName")
                        ? iterator.getString("satelliteName") : null);
                item.setChannelCode(dataSet.getColumnNames().contains("channelCode")
                        ? iterator.getString("channelCode") : null);
                item.setChannelName(iterator.getString("channelName"));
                item.setRawLength(iterator.getInt("rawLength"));
                item.setRawFrame(toHex(raw == null ? null : raw.getValues()));

                // 新结构使用布尔检查结果，空值按未知状态返回。
                TelemetryFrameCheckResult checkResult;
                if (iterator.isNull("frameCheckStatus")) {
                    checkResult = TelemetryFrameCheckResult.UNKNOWN;
                } else {
                    checkResult = iterator.getBoolean("frameCheckStatus")
                            ? TelemetryFrameCheckResult.CORRECT
                            : TelemetryFrameCheckResult.ERROR;
                }
                item.setCheckResult(checkResult.getCode());
                item.setCheckResultName(checkResult.getDescription());
                result.add(item);
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("查询IoTDB实时遥测数据失败", ex);
        }
    }

    @Override
    public List<ProcessedTelemetryVO> listProcessedTelemetry(
            String taskId,
            Long interfaceId) {
        // 第一步：任务编号必须明确，防止查询到其他试验任务的数据。
        String normalizedTaskId = requireIoTDBTaskId(taskId);
        // 第二步：处理后数据排除空参数值，并追加任务和可选采集接口条件。
        String filter = interfaceFilter(
                normalizedTaskId, interfaceId, true);
        // 第三步：查询符合条件的全部处理后参数，不再执行分页和总数统计。
        String sql = "select interfaceId, satelliteCode, tmName, value, stateName, "
                + "stateIndex, deduplication, processedAt "
                + "from root.db.*.tms.*.*.*" + filter
                + " order by time desc align by device";
        List<ProcessedTelemetryVO> records = new ArrayList<>();
        try (SessionDataSetWrapper dataSet =
                     iotdbSessionPool.executeQueryStatement(sql)) {
            SessionDataSet.DataIterator iterator = dataSet.iterator();
            while (iterator.next()) {
                ProcessedTelemetryVO item = new ProcessedTelemetryVO();
                item.setInterfaceId(nullableLong(iterator, "interfaceId"));
                item.setSatelliteId(iterator.getString("satelliteCode"));
                item.setParameter(iterator.getString("tmName"));
                item.setParameterValue(normalizeIoTDBValue(
                        iterator.getObject("value")));
                item.setStatus(iterator.getString("stateName"));
                item.setStateIndex(nullableInteger(iterator, "stateIndex"));
                item.setDeduplication(iterator.getString("deduplication"));
                long processedAt = iterator.getLong("processedAt");
                item.setProcessTime(
                        new Timestamp(processedAt).toLocalDateTime());
                records.add(item);
            }
            return records;
        } catch (Exception ex) {
            throw new IllegalStateException("查询IoTDB处理后数据失败", ex);
        }
    }

    private static Object normalizeIoTDBValue(Object value) {
        if (value instanceof Binary) {
            return ((Binary) value).getStringValue(StandardCharsets.UTF_8);
        }
        return value;
    }

    @Override
    public List<InvalidTelemetryFrame> listInvalidTelemetryFrames(
            String taskId,
            Long interfaceId,
            String satelliteCode,
            String channelCode) {
        // 第一步：任务编号必须明确，防止查询到其他试验任务的数据。
        String normalizedTaskId = requireIoTDBTaskId(taskId);
        // 第二步：异常帧和正常帧共用_frame节点，通过检查结果编码筛选异常帧。
        String filter = invalidFrameFilter(
                normalizedTaskId, interfaceId, satelliteCode, channelCode);
        String sql = "select interfaceId, satelliteCode, channelCode, checkResult, "
                + "rawLength, raw, uniqueCode "
                + "from root.db.*.tms.*.*._frame" + filter
                + " order by time desc align by device";

        // 第三步：将IoTDB查询结果转换为原异常帧接口的数据结构。
        List<InvalidTelemetryFrame> records = new ArrayList<>();
        try (SessionDataSetWrapper dataSet =
                     iotdbSessionPool.executeQueryStatement(sql)) {
            SessionDataSet.DataIterator iterator = dataSet.iterator();
            while (iterator.next()) {
                InvalidTelemetryFrame record = new InvalidTelemetryFrame();
                record.setInterfaceId(nullableLong(iterator, "interfaceId"));
                Timestamp time = iterator.getTimestamp("Time");
                record.setReceiveTime(
                        time == null ? null : time.toLocalDateTime());
                record.setSatelliteCode(iterator.getString("satelliteCode"));
                record.setChannelCode(iterator.getString("channelCode"));

                // 查询返回的枚举字段统一使用枚举中的中文名称。
                TelemetryFrameCheckResult checkResult =
                        TelemetryFrameCheckResult.fromCode(
                                iterator.getInt("checkResult"));
                record.setCheckResult(checkResult.getCode());
                record.setCheckResultName(checkResult.getDescription());
                record.setRawLength(iterator.getInt("rawLength"));
                Binary raw = iterator.getBlob("raw");
                record.setRawFrame(toHex(
                        raw == null ? null : raw.getValues()));
                record.setUniqueCode(iterator.getString("uniqueCode"));
                records.add(record);
            }

            return records;
        } catch (Exception exception) {
            throw new IllegalStateException("查询IoTDB异常遥测帧失败", exception);
        }
    }

    /** 生成异常帧查询条件，并对文本条件进行转义。 */
    private String invalidFrameFilter(
            String taskId,
            Long interfaceId,
            String satelliteCode,
            String channelCode) {
        StringBuilder filter = new StringBuilder(" where taskId = ")
                .append(iotdbTextLiteral(taskId))
                .append(" and checkResult = 2");
        if (interfaceId != null) {
            filter.append(" and interfaceId = ").append(interfaceId);
        }
        String safeSatelliteCode = trimToNull(satelliteCode);
        String safeChannelCode = trimToNull(channelCode);
        if (safeSatelliteCode != null) {
            filter.append(" and satelliteCode = ")
                    .append(iotdbTextLiteral(safeSatelliteCode));
        }
        if (safeChannelCode != null) {
            filter.append(" and channelCode = ")
                    .append(iotdbTextLiteral(safeChannelCode));
        }
        return filter.toString();
    }

    /** 生成采集接口筛选条件，处理后参数查询同时排除空值。 */
    private String interfaceFilter(
            String taskId,
            Long interfaceId,
            boolean requireValue) {
        StringBuilder filter = new StringBuilder(" where taskId = ")
                .append(iotdbTextLiteral(taskId));
        if (requireValue) {
            filter.append(" and value is not null");
        }
        if (interfaceId != null) {
            filter.append(" and interfaceId = ").append(interfaceId);
        }
        return filter.toString();
    }

    /** 校验并规范化IoTDB查询使用的外部任务编号。 */
    private String requireIoTDBTaskId(String taskId) {
        String normalizedTaskId = trimToNull(taskId);
        if (normalizedTaskId == null) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        return normalizedTaskId;
    }

    /** 读取允许为空的IoTDB长整型测点。 */
    private Long nullableLong(
            SessionDataSet.DataIterator iterator,
            String columnName) throws Exception {
        return iterator.isNull(columnName)
                ? null : iterator.getLong(columnName);
    }

    /** 读取允许为空的IoTDB整型测点。 */
    private Integer nullableInteger(
            SessionDataSet.DataIterator iterator,
            String columnName) throws Exception {
        return iterator.isNull(columnName)
                ? null : iterator.getInt(columnName);
    }

    /** 将查询文本转换为安全的IoTDB字符串字面量。 */
    private String iotdbTextLiteral(String value) {
        String escaped = value.replace("\\", "\\\\")
                .replace("'", "\\'");
        return "'" + escaped + "'";
    }

    private static String toHex(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(data.length * 3 - 1);
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(String.format("%02X", data[i] & 0xFF));
        }
        return result.toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public ProcessingRuleConfig getProcessingRuleConfig(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        ProcessingRuleConfig config = processingRuleConfigMapper.findByTaskId(taskId);
        if (config != null) {
            return config;
        }
        config = new ProcessingRuleConfig();
        config.setTaskId(taskId);
        config.setParameterRangeCheckEnabled(0);
        processingRuleConfigMapper.insert(config);
        return processingRuleConfigMapper.findByTaskId(taskId);
    }

    @Override
    @Transactional
    public ProcessingRuleConfig saveProcessingRuleConfig(ProcessingRuleConfig config) {
        if (config == null || config.getTaskId() == null || config.getTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        if (processingRuleConfigMapper.findByTaskId(config.getTaskId()) == null) {
            processingRuleConfigMapper.insert(config);
        } else {
            processingRuleConfigMapper.update(config);
        }
        // 事务提交后通知数据处理模块刷新当前接口的规则开关快照。
        runtimeSyncService.syncAfterCommit();
        return processingRuleConfigMapper.findByTaskId(config.getTaskId());
    }

    /** 使用PageHelper分页查询处理日志。 */
    @Override
    public PageResult<DataProcessLog> pageProcessLogs(
            String taskId,
            Long processTaskId,
            Integer logLevel,
            Integer pageNum,
            Integer pageSize) {
        if (!DataProcessLogLevel.isValid(logLevel)) {
            throw new IllegalArgumentException("日志级别只能为1、2或3");
        }
        int safePageNum = pageNum == null ? 1 : Math.max(1, pageNum);
        int safePageSize = pageSize == null ? 20
                : Math.min(100, Math.max(1, pageSize));

        // PageHelper只分页紧随其后的第一条MyBatis查询，中间不能插入其他查询。
        PageHelper.startPage(safePageNum, safePageSize);
        List<DataProcessLog> records = logMapper.findAll(
                taskId, processTaskId, logLevel);
        PageInfo<DataProcessLog> pageInfo = new PageInfo<>(records);

        return new PageResult<DataProcessLog>(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                records);
    }

    /**
     * 预留日志写入方法。
     * 当前页面不会主动调用，后续由真实数据处理链路在关键处理节点调用。
     */
    @Override
    @Transactional
    public Long createProcessLog(DataProcessLog log) {
        if (log == null || log.getTaskId() == null
                || log.getTaskId().trim().isEmpty()
                || log.getLogContent() == null
                || log.getLogContent().trim().isEmpty()) {
            throw new IllegalArgumentException("日志任务和日志内容不能为空");
        }
        if (!DataProcessLogLevel.isValid(log.getLogLevel())) {
            throw new IllegalArgumentException("日志级别只能为1、2或3");
        }
        if (log.getLogTime() == null) {
            log.setLogTime(LocalDateTime.now());
        }
        if (log.getLogLevel() == null) {
            log.setLogLevel(DataProcessLogLevel.INFO.getCode());
        }
        logMapper.insert(log);
        return log.getId();
    }

    /** 查询当前任务指定类型的设备卫星筛选项。 */
    @Override
    public List<DeviceSatelliteOptionVO> listDeviceSatellites(String taskId, String type) {
        requireTaskId(taskId);
        // 类型先去除首尾空白，未传或空字符串时不参与查询筛选。
        String normalizedType = type == null ? null : type.trim();
        if (normalizedType != null && !normalizedType.isEmpty()) {
            DeviceSatelliteType.requireValid(normalizedType);
        }
        return deviceSatelliteMapper.findOptions(taskId, normalizedType);
    }

    /** 参数列表必须同时限定任务与设备卫星。 */
    @Override
    public List<TelemetryParseRuleConfig> listParseRules(String taskId, Long deviceSatelliteId) {
        requireDevice(taskId, deviceSatelliteId);
        return parseRuleMapper.findAllByTaskId(taskId, deviceSatelliteId);
    }

    /** 先校验全部Excel页签或TXT内容，再原子替换当前任务内的同类型设备及参数。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importParseRules(
            String taskId,
            String type,
            String fileName,
            byte[] content) {
        requireTaskId(taskId);
        DeviceSatelliteType.requireValid(type);
        Map<String, List<TelemetryParseRuleConfig>> sheets =
                workbookParser.parse(taskId, fileName, content);
        // 第一步：锁定替换过程，防止并发导入或编辑留下多套有效记录。
        lockTask(taskId);
        parseRuleMapper.deleteByType(taskId, type);
        deviceSatelliteMapper.deleteByType(taskId, type);
        int count = 0;
        // 第二步：每页先生成设备主键，再批量保存这一页的全部参数。
        for (Map.Entry<String, List<TelemetryParseRuleConfig>> sheet : sheets.entrySet()) {
            DeviceSatellite device = new DeviceSatellite();
            device.setTaskId(taskId);
            device.setType(type);
            device.setCode(sheet.getKey());
            device.setName(sheet.getKey());
            deviceSatelliteMapper.insert(device);
            List<TelemetryParseRuleConfig> rules = sheet.getValue();
            for (TelemetryParseRuleConfig rule : rules) {
                rule.setDeviceSatelliteId(device.getId());
            }
            for (int start = 0; start < rules.size(); start += IMPORT_BATCH_SIZE) {
                parseRuleMapper.batchInsert(rules.subList(start, Math.min(start + IMPORT_BATCH_SIZE, rules.size())));
            }
            count += rules.size();
        }
        // 第三步：同步当前任务的可视化配置，任何一步失败都回滚本次替换。
        parameterSyncService.synchronizeTaskParameters(taskId);
        return count;
    }

    /** 设备卫星必须属于当前任务且未删除。 */
    private void requireDevice(String taskId, Long deviceSatelliteId) {
        requireTaskId(taskId);
        if (deviceSatelliteId == null || deviceSatelliteId <= 0
                || deviceSatelliteMapper.findActive(taskId, deviceSatelliteId) == null) {
            throw new IllegalArgumentException("当前任务下不存在有效的设备卫星");
        }
    }

    /** 通过已有任务行串行化当前任务的写入，不锁定其他任务。 */
    private void lockTask(String taskId) {
        if (deviceSatelliteMapper.lockForReplacement(taskId) == null) {
            throw new IllegalArgumentException("试验任务不存在");
        }
    }

    /** 所有参数操作必须明确任务范围。 */
    private void requireTaskId(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
    }
}
