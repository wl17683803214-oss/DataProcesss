package com.example.dataadmin.service.impl;

import com.example.common.response.PageResult;
import com.example.common.enums.TelemetryFrameStatus;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryIdentity;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;
import com.example.dataadmin.entity.DeviceSatellite;
import com.example.dataadmin.entity.TelemetrySystemConfig;
import com.example.dataadmin.entity.CollectProtocolConfig;
import com.example.dataadmin.dto.processing.TelemetryParseRuleBatchUpdateRequest;
import com.example.dataadmin.mapper.TelemetrySystemConfigMapper;
import com.example.dataadmin.mapper.CollectProtocolConfigMapper;
import com.example.dataadmin.mapper.ProcessedTelemetryFilterSelectionMapper;
import com.example.dataadmin.mapper.DeviceSatelliteMapper;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.enums.EnumData;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.service.support.ParameterWorkbookParser;
import com.example.dataadmin.service.support.SystemResourceMetrics;
import com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO;
import com.example.dataadmin.entity.ProcessingRuleConfig;
import com.example.dataadmin.entity.InvalidTelemetryFrame;
import com.example.dataadmin.mapper.DashboardMapper;
import com.example.dataadmin.mapper.CollectInterfaceConfigMapper;
import com.example.dataadmin.mapper.TelemetryParseRuleConfigMapper;
import com.example.dataadmin.mapper.ProcessingRuleConfigMapper;
import com.example.dataadmin.service.DataProcessingService;
import com.example.dataadmin.service.ProcessedTelemetryQueryService;
import com.example.dataadmin.service.CollectInterfaceRuntimeSyncService;
import com.example.dataadmin.service.VisualizationParameterSyncService;
import com.example.dataadmin.vo.processing.DataProcessingOverviewVO;
import com.example.dataadmin.vo.processing.DataProcessingRealtimeVO;
import com.example.dataadmin.vo.processing.CollectInterfaceOptionVO;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
    /** GreptimeDB页面查询允许的最大单页数量。 */
    private static final int MAX_TELEMETRY_PAGE_SIZE = 200;

    /** 数据处理统计数据访问组件。 */
    private final DashboardMapper dashboardMapper;

    /** 采集接口筛选项数据访问组件。 */
    private final CollectInterfaceConfigMapper collectInterfaceMapper;


    /** 遥测解析规则数据访问组件。 */
    private final TelemetryParseRuleConfigMapper parseRuleMapper;
    private final ProcessingRuleConfigMapper processingRuleConfigMapper;
    /** 采集接口运行配置同步服务。 */
    private final CollectInterfaceRuntimeSyncService runtimeSyncService;
    /** 独立遥测数据访问组件，不参与金仓事务。 */
    private final GreptimeStore telemetryStore;
    /** 已勾选参数的独立并发查询组件。 */
    private final ProcessedTelemetryQueryService processedTelemetryQueryService;
    /** 可视化参数筛选项同步服务。 */
    private final VisualizationParameterSyncService parameterSyncService;

    /** 设备卫星关联与替换锁。 */
    private final DeviceSatelliteMapper deviceSatelliteMapper;
    /** 所属系统层级访问组件。 */
    private final TelemetrySystemConfigMapper systemMapper;
    /** 导入时核对仍被协议配置引用的设备。 */
    private final CollectProtocolConfigMapper protocolMapper;
    /** 筛选勾选状态在重新导入后按遥测代号保留。 */
    private final ProcessedTelemetryFilterSelectionMapper selectionMapper;
    /** 工作簿读取和字段校验。 */
    private final ParameterWorkbookParser workbookParser;
    /** Redis字符串访问组件。 */
    private final StringRedisTemplate redisTemplate;
    /** 与系统告警共用的机器CPU和物理内存采集组件。 */
    @Autowired
    private SystemResourceMetrics systemResourceMetrics;
    /** 实时指标JSON读取组件。 */
    private final ObjectMapper objectMapper;

    public DataProcessingServiceImpl(
            DashboardMapper dashboardMapper,
            CollectInterfaceConfigMapper collectInterfaceMapper,
            TelemetryParseRuleConfigMapper parseRuleMapper,
            ProcessingRuleConfigMapper processingRuleConfigMapper,
            CollectInterfaceRuntimeSyncService runtimeSyncService,
            GreptimeStore telemetryStore,
            VisualizationParameterSyncService parameterSyncService,
            DeviceSatelliteMapper deviceSatelliteMapper,
            TelemetrySystemConfigMapper systemMapper,
            CollectProtocolConfigMapper protocolMapper,
            ProcessedTelemetryFilterSelectionMapper selectionMapper,
            ParameterWorkbookParser workbookParser,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ProcessedTelemetryQueryService processedTelemetryQueryService) {
        this.dashboardMapper = dashboardMapper;
        this.collectInterfaceMapper = collectInterfaceMapper;
        this.parseRuleMapper = parseRuleMapper;
        this.processingRuleConfigMapper = processingRuleConfigMapper;
        this.runtimeSyncService = runtimeSyncService;
        this.telemetryStore = telemetryStore;
        this.parameterSyncService = parameterSyncService;
        this.deviceSatelliteMapper = deviceSatelliteMapper;
        this.systemMapper = systemMapper;
        this.protocolMapper = protocolMapper;
        this.selectionMapper = selectionMapper;
        this.workbookParser = workbookParser;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processedTelemetryQueryService = processedTelemetryQueryService;
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
            // 第一步：汇总当前任务下所有接口按实际采样间隔计算的PDXP帧速率。
            double processRate = processingRate(taskId);
            // 展示时统一四舍五入，整数速率不追加无意义的小数位。
            result.setProcessRate(BigDecimal.valueOf(processRate)
                    .setScale(2, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString() + " 条/秒");
        } catch (RuntimeException ignored) {
            // Redis暂时不可用或缓存内容异常时返回带单位的零值，保证页面可用。
        }
        // 第二步：机器资源独立采集，Redis异常也不影响CPU和内存的展示。
        applyRuntimeMetric(result);
        return result;
    }

    /** 汇总任务下所有采集接口按实际采样间隔计算的成功处理帧速率。 */
    private double processingRate(String taskId) {
        Map<Object, Object> metrics = redisTemplate.opsForHash().entries(
                PROCESSING_CURRENT_PREFIX + taskId);
        double total = 0D;
        for (Object value : metrics.values()) {
            try {
                // 每个接口独立解析，单条异常数据不影响其他接口的统计结果。
                JsonNode metric = objectMapper.readTree(String.valueOf(value));
                total += Math.max(0D, metric.path("processRate").asDouble(0D));
            } catch (Exception ignored) {
                // 非当前统计组件写入的无效字段不参与本次汇总。
            }
        }
        return total;
    }

    /** 将与系统告警同口径的机器资源指标格式化为页面字段。 */
    private void applyRuntimeMetric(DataProcessingRealtimeVO result) {
        // 第一步：读取机器整体CPU使用率，保留两位小数并追加百分号。
        result.setCpuUsage(String.format(
                Locale.ROOT, "%.2f%%", systemResourceMetrics.readCpuUsage()));
        // 第二步：读取机器物理内存使用率，保持与系统告警一致的计算口径。
        result.setMemoryUsage(String.format(
                Locale.ROOT, "%.2f%%", systemResourceMetrics.readMemoryUsage()));
    }

    /** 将数据库记录转换为既有正常帧响应，保持字段和分页规则。 */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResult<RealtimeTelemetryFrameVO> pageRealtimeTelemetryFrames(
            String taskId, Long interfaceId, Integer pageNum, Integer pageSize) {
        // 第一步：校验条件，公共映射使用绑定变量限定任务和接口。
        TelemetryPage page = requireTelemetryPage(pageNum, pageSize);
        Map<String, Object> query = frameQuery(taskId, interfaceId, TelemetryFrameStatus.NORMAL, page);
        List<RealtimeTelemetryFrameVO> records = new ArrayList<>();
        // 第二步：从原始字节派生长度和十六进制文本，接口继续显示中文检查结果。
        for (TelemetryFrameRecord row : telemetryStore.frames(query)) {
            RealtimeTelemetryFrameVO record = new RealtimeTelemetryFrameVO();
            record.setDeviceSatelliteId(TelemetryIdentity.deviceId(row.getSourceType(), row.getSourceIdentity()));
            record.setTime(row.getSourceTime() == null ? null : row.getSourceTime().toLocalDateTime());
            record.setDeviceSatelliteName(row.getDeviceSatelliteName());
            record.setChannelName(row.getChannelName());
            record.setRawLength(row.getRaw() == null ? 0 : row.getRaw().length);
            record.setRawFrame(toHex(row.getRaw()));
            record.setCheckResultName(TelemetryFrameStatus.labelOf(row.getCheckStatus()));
            records.add(record);
        }
        // 第三步：计数复用同一套筛选条件，不受金仓分页插件影响。
        return new PageResult<>(page.pageNum, page.pageSize, telemetryStore.countFrames(query), records);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ProcessedTelemetryVO> listProcessedTelemetry(String taskId,
            Integer selectionType, Long targetId) {
        return processedTelemetryQueryService.latest(
                requireTelemetryTaskId(taskId), selectionType, targetId);
    }

    /** 与最新值使用同一批勾选参数，分别查询各参数的最近曲线点。 */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ProcessedTelemetryCurveVO> listProcessedTelemetryCurves(String taskId,
            Integer selectionType, Long targetId) {
        return processedTelemetryQueryService.curves(
                requireTelemetryTaskId(taskId), selectionType, targetId);
    }
    /** 异常帧继续使用原分页接口和响应字段。 */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResult<InvalidTelemetryFrame> pageInvalidTelemetryFrames(
            String taskId, Long interfaceId, Integer pageNum, Integer pageSize) {
        // 第一步：正常和异常整帧共用相同的身份与分页校验。
        TelemetryPage page = requireTelemetryPage(pageNum, pageSize);
        Map<String, Object> query = frameQuery(taskId, interfaceId, TelemetryFrameStatus.ABNORMAL, page);
        List<InvalidTelemetryFrame> records = new ArrayList<>();
        // 第二步：远程帧不伪造本地设备主键，检查编码通过已有枚举转换为中文。
        for (TelemetryFrameRecord row : telemetryStore.frames(query)) {
            InvalidTelemetryFrame record = new InvalidTelemetryFrame();
            record.setDeviceSatelliteId(TelemetryIdentity.deviceId(row.getSourceType(), row.getSourceIdentity()));
            record.setReceiveTime(row.getSourceTime() == null ? null : row.getSourceTime().toLocalDateTime());
            record.setDeviceSatelliteName(row.getDeviceSatelliteName());
            record.setChannelName(row.getChannelName());
            record.setCheckResultName(TelemetryFrameStatus.labelOf(row.getCheckStatus()));
            record.setRawLength(row.getRaw() == null ? 0 : row.getRaw().length);
            record.setRawFrame(toHex(row.getRaw()));
            records.add(record);
        }
        return new PageResult<>(page.pageNum, page.pageSize, telemetryStore.countFrames(query), records);
    }

    /** 构造绑定参数，接口不再接收或暴露数据库路径。 */
    private Map<String, Object> frameQuery(String taskId, Long interfaceId,
            TelemetryFrameStatus status, TelemetryPage page) {
        // 可选接口存在时必须是真实主键；任务必填，禁止扩大查询范围。
        if (interfaceId != null && interfaceId <= 0) {
            throw new IllegalArgumentException("采集接口主键必须大于零");
        }
        Map<String, Object> query = new HashMap<>();
        query.put("taskId", requireTelemetryTaskId(taskId));
        query.put("interfaceId", interfaceId);
        query.put("checkStatus", status.getValue());
        query.put("limit", page.pageSize);
        query.put("offset", page.offset);
        return query;
    }

    /** 校验GreptimeDB页面查询的页码和每页数量。 */
    private TelemetryPage requireTelemetryPage(
            Integer pageNum,
            Integer pageSize) {
        int normalizedPageNum = pageNum == null ? 1 : pageNum;
        int normalizedPageSize = pageSize == null ? 20 : pageSize;
        if (normalizedPageNum < 1) {
            throw new IllegalArgumentException("页码不能小于1");
        }
        if (normalizedPageSize < 1
                || normalizedPageSize > MAX_TELEMETRY_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在1到200之间");
        }
        // 使用长整型计算偏移量，避免较大页码发生整数溢出。
        long offset = (long) (normalizedPageNum - 1)
                * normalizedPageSize;
        return new TelemetryPage(
                normalizedPageNum, normalizedPageSize, offset);
    }

    /** 校验并规范化GreptimeDB查询使用的外部任务编号。 */
    private String requireTelemetryTaskId(String taskId) {
        String normalizedTaskId = trimToNull(taskId);
        if (normalizedTaskId == null) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        return normalizedTaskId;
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

    /** GreptimeDB页面查询使用的已校验分页参数。 */
    private static final class TelemetryPage {
        /** 当前页码。 */
        private final int pageNum;
        /** 每页记录数。 */
        private final int pageSize;
        /** 当前页起始偏移量。 */
        private final long offset;

        /** 保存一次查询所需的分页参数。 */
        private TelemetryPage(int pageNum, int pageSize, long offset) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.offset = offset;
        }
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

    /** 参数解析配置使用数据库分页，指定系统时只查询该系统直属参数。 */
    @Override
    public PageResult<TelemetryParseRuleConfig> pageParseRules(String taskId,
            Long deviceSatelliteId, Long systemId, Integer pageNum, Integer pageSize) {
        requireDevice(taskId, deviceSatelliteId);
        if (systemId != null && systemMapper.findActive(taskId, deviceSatelliteId, systemId) == null) {
            throw new IllegalArgumentException("当前设备下不存在有效的所属系统");
        }
        int normalizedPageNum = pageNum == null ? 1 : pageNum;
        int normalizedPageSize = pageSize == null ? 20 : pageSize;
        if (normalizedPageNum < 1 || normalizedPageSize < 1 || normalizedPageSize > 200) {
            throw new IllegalArgumentException("页码或每页数量不正确");
        }
        long total = parseRuleMapper.countPage(taskId, deviceSatelliteId, systemId);
        List<TelemetryParseRuleConfig> records = total == 0
                ? new ArrayList<>() : parseRuleMapper.findPage(taskId, deviceSatelliteId,
                        systemId, normalizedPageSize,
                        (long) (normalizedPageNum - 1) * normalizedPageSize);
        return new PageResult<>(normalizedPageNum, normalizedPageSize, total, records);
    }

    /** 先验证整批最终数据，再在同一事务中修改参数和勾选记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateParseRules(TelemetryParseRuleBatchUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("批量修改请求不能为空");
        }
        String taskId = request.getTaskId();
        requireTaskId(taskId);
        List<TelemetryParseRuleBatchUpdateRequest.Item> items = request.getItems();
        if (items == null || items.isEmpty() || items.size() > 200) {
            throw new IllegalArgumentException("单次必须修改1到200条遥测参数");
        }
        // 第一步：按任务串行化导入与编辑，并一次加载本批次原始记录。
        lockTask(taskId);
        List<Long> ids = new ArrayList<>(items.size());
        Set<Long> distinctIds = new HashSet<>();
        for (TelemetryParseRuleBatchUpdateRequest.Item item : items) {
            if (item == null || item.getId() == null || item.getDeviceSatelliteId() == null
                    || !distinctIds.add(item.getId())) {
                throw new IllegalArgumentException("参数主键或设备主键无效，或批次中存在重复参数");
            }
            ids.add(item.getId());
        }
        Map<Long, TelemetryParseRuleConfig> originalById = new HashMap<>();
        for (TelemetryParseRuleConfig original : parseRuleMapper.findActiveByIds(taskId, ids)) {
            originalById.put(original.getId(), original);
        }
        if (originalById.size() != items.size()) {
            throw new IllegalArgumentException("批次中存在不属于当前任务的有效参数");
        }

        // 第二步：复用导入校验，并从当前设备系统树生成完整所属系统路径。
        Map<Long, Map<Long, TelemetrySystemConfig>> systemsByDevice = new HashMap<>();
        Map<Long, List<TelemetryParseRuleConfig>> updatesByDevice = new HashMap<>();
        for (TelemetryParseRuleBatchUpdateRequest.Item item : items) {
            TelemetryParseRuleConfig original = originalById.get(item.getId());
            if (!item.getDeviceSatelliteId().equals(original.getDeviceSatelliteId())) {
                throw new IllegalArgumentException("参数不能移动到其他设备卫星：" + item.getId());
            }
            TelemetryParseRuleConfig update = new TelemetryParseRuleConfig();
            BeanUtils.copyProperties(item, update);
            update.setTaskId(taskId);
            update.setSystemName(systemPath(taskId, update.getDeviceSatelliteId(),
                    update.getSystemId(), systemsByDevice));
            workbookParser.validate(update);
            updatesByDevice.computeIfAbsent(update.getDeviceSatelliteId(),
                    ignored -> new ArrayList<>()).add(update);
        }

        // 第三步：检查批内重复值及批外已有值，避免数据库在写入中间态报唯一键冲突。
        for (Map.Entry<Long, List<TelemetryParseRuleConfig>> group : updatesByDevice.entrySet()) {
            List<Long> batchIds = new ArrayList<>();
            List<String> tableIndexes = new ArrayList<>();
            List<String> telemetryCodes = new ArrayList<>();
            Set<String> uniqueIndexes = new HashSet<>();
            Set<String> uniqueCodes = new HashSet<>();
            for (TelemetryParseRuleConfig update : group.getValue()) {
                batchIds.add(update.getId());
                tableIndexes.add(update.getTableIndex());
                telemetryCodes.add(update.getTelemetryCode());
                if (!uniqueIndexes.add(update.getTableIndex())
                        || !uniqueCodes.add(update.getTelemetryCode())) {
                    throw new IllegalArgumentException("同一设备卫星的序号或遥测代号不能重复");
                }
            }
            if (parseRuleMapper.findBatchConflict(taskId, group.getKey(), batchIds,
                    tableIndexes, telemetryCodes) != null) {
                throw new IllegalArgumentException("修改后的序号或遥测代号与已有参数重复");
            }
        }

        // 第四步：先为整批记录腾出唯一键，再写入每条记录的最终业务字段。
        Map<Long, List<ProcessedTelemetryFilterSelection>> selectedByDevice = new HashMap<>();
        for (Long deviceId : updatesByDevice.keySet()) {
            selectedByDevice.put(deviceId,
                    selectionMapper.findSelectedByDevice(taskId, deviceId));
        }
        for (TelemetryParseRuleBatchUpdateRequest.Item item : items) {
            TelemetryParseRuleConfig temporary = new TelemetryParseRuleConfig();
            temporary.setId(item.getId());
            temporary.setDeviceSatelliteId(item.getDeviceSatelliteId());
            String suffix = UUID.randomUUID().toString();
            temporary.setTableIndex("__batch_index_" + suffix);
            temporary.setTelemetryCode("__batch_code_" + suffix);
            if (parseRuleMapper.updateTemporaryKeys(taskId, temporary) != 1) {
                throw new IllegalStateException("暂存遥测参数唯一键失败：" + item.getId());
            }
        }
        for (List<TelemetryParseRuleConfig> group : updatesByDevice.values()) {
            for (TelemetryParseRuleConfig update : group) {
                if (parseRuleMapper.updateOne(taskId, update) != 1) {
                    throw new IllegalStateException("修改遥测参数失败：" + update.getId());
                }
            }
        }

        // 第五步：清除变化前后的代号勾选，再把原参数的勾选状态关联到新代号。
        Map<TelemetryParseRuleConfig, List<ProcessedTelemetryFilterSelection>> selectedRenames =
                new LinkedHashMap<>();
        for (List<TelemetryParseRuleConfig> group : updatesByDevice.values()) {
            for (TelemetryParseRuleConfig update : group) {
                String oldCode = originalById.get(update.getId()).getTelemetryCode();
                if (!oldCode.equals(update.getTelemetryCode())) {
                    selectionMapper.markDeleted(taskId, update.getDeviceSatelliteId(), oldCode);
                    selectionMapper.markDeleted(taskId, update.getDeviceSatelliteId(),
                            update.getTelemetryCode());
                    for (ProcessedTelemetryFilterSelection selected
                            : selectedByDevice.get(update.getDeviceSatelliteId())) {
                        if (oldCode.equals(selected.getTelemetryCode())) {
                            selectedRenames.computeIfAbsent(update,
                                    ignored -> new ArrayList<>()).add(selected);
                        }
                    }
                }
            }
        }
        for (Map.Entry<TelemetryParseRuleConfig,
                List<ProcessedTelemetryFilterSelection>> rename : selectedRenames.entrySet()) {
            TelemetryParseRuleConfig update = rename.getKey();
            for (ProcessedTelemetryFilterSelection selected : rename.getValue()) {
                selectionMapper.save(taskId, update.getDeviceSatelliteId(),
                        update.getTelemetryCode(), selected.getSelectionType(),
                        selected.getTargetId(), 0);
            }
        }
        // 第六步：只刷新本批可视化筛选项；运行中的采集快照保持不变。
        parameterSyncService.synchronizeChangedParameters(taskId, ids);
        return items.size();
    }

    /** 根据选中的系统主键恢复从根系统到当前系统的完整名称路径。 */
    private String systemPath(String taskId, Long deviceId, Long systemId,
            Map<Long, Map<Long, TelemetrySystemConfig>> systemsByDevice) {
        if (systemId == null) {
            return null;
        }
        if (systemId <= 0) {
            throw new IllegalArgumentException("所属系统主键不正确");
        }
        Map<Long, TelemetrySystemConfig> systems = systemsByDevice.computeIfAbsent(deviceId,
                id -> {
                    Map<Long, TelemetrySystemConfig> values = new HashMap<>();
                    for (TelemetrySystemConfig system : systemMapper.findByDevice(taskId, id)) {
                        values.put(system.getId(), system);
                    }
                    return values;
                });
        List<String> names = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        long currentId = systemId;
        while (currentId != 0) {
            TelemetrySystemConfig system = systems.get(currentId);
            if (system == null || !visited.add(currentId)) {
                throw new IllegalArgumentException("当前设备下不存在有效的所属系统：" + systemId);
            }
            names.add(system.getSystemName());
            currentId = system.getParentId();
        }
        Collections.reverse(names);
        return String.join("\\", names);
    }

    /** 先校验全部文件内容，再按任务和类型仅替换同名设备的系统及参数。 */
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
        // 第二步：只匹配当前任务同类型同名设备，未上传的设备保持原有配置。
        Map<String, DeviceSatellite> existing = new HashMap<>();
        for (DeviceSatellite device : deviceSatelliteMapper.findByType(taskId, type)) {
            existing.put(device.getName(), device);
        }

        int count = 0;
        // 第三步：逐页复用设备主键，按首次出现顺序建立所属系统层级。
        for (Map.Entry<String, List<TelemetryParseRuleConfig>> sheet : sheets.entrySet()) {
            DeviceSatellite device = existing.get(sheet.getKey());
            if (device == null) {
                device = new DeviceSatellite();
                device.setTaskId(taskId);
                device.setType(type);
                device.setCode(sheet.getKey());
                device.setName(sheet.getKey());
                deviceSatelliteMapper.insert(device);
            } else {
                // 同名设备保留主键，仅替换该设备的旧参数和系统。
                parseRuleMapper.deleteByDevice(taskId, device.getId());
                systemMapper.deleteByDevice(taskId, device.getId());
            }
            List<TelemetryParseRuleConfig> rules = sheet.getValue();
            Map<String, Long> systemIds = new HashMap<>();
            int order = 0;
            for (TelemetryParseRuleConfig rule : rules) {
                rule.setDeviceSatelliteId(device.getId());
                // 空所属系统保持空主键，其他路径逐段复用当前父节点下的系统。
                String path = rule.getSystemName();
                if (path == null || path.trim().isEmpty() || "-".equals(path.trim())
                        || "—".equals(path.trim())) {
                    rule.setSystemName(null);
                    continue;
                }
                long parentId = 0L;
                StringBuilder key = new StringBuilder();
                for (String component : path.split("\\\\")) {
                    String name = component.trim();
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("所属系统路径包含空层级：" + path);
                    }
                    key.append(parentId).append('\u0000').append(name);
                    Long systemId = systemIds.get(key.toString());
                    if (systemId == null) {
                        TelemetrySystemConfig system = new TelemetrySystemConfig();
                        system.setTaskId(taskId);
                        system.setDeviceSatelliteId(device.getId());
                        system.setParentId(parentId);
                        system.setSystemName(name);
                        system.setSortOrder(++order);
                        systemMapper.insert(system);
                        systemId = system.getId();
                        systemIds.put(key.toString(), systemId);
                    }
                    parentId = systemId;
                    key.setLength(0);
                }
                rule.setSystemId(parentId);
            }
            for (int start = 0; start < rules.size(); start += IMPORT_BATCH_SIZE) {
                parseRuleMapper.batchInsert(rules.subList(start, Math.min(start + IMPORT_BATCH_SIZE, rules.size())));
            }
            count += rules.size();
        }
        // 第四步：同步页面参数，运行接口快照保持不变直到下一次接口同步。
        parameterSyncService.synchronizeTaskParameters(taskId);
        selectionMapper.removeMissing(taskId, type);
        return count;
    }

    /** 按设备卫星主键删除目录及全部参数配置。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteParseRules(Long deviceSatelliteId) {
        // 第一步：定位所属任务并加锁，与上传和参数编辑串行执行。
        if (deviceSatelliteId == null || deviceSatelliteId <= 0) {
            throw new IllegalArgumentException("设备卫星编号必须为正整数");
        }
        DeviceSatellite device = deviceSatelliteMapper.findActiveById(deviceSatelliteId);
        if (device == null) {
            throw new IllegalArgumentException("设备卫星不存在或已删除");
        }
        String taskId = device.getTaskId();
        lockTask(taskId);
        device = deviceSatelliteMapper.findActive(taskId, deviceSatelliteId);
        if (device == null) {
            throw new IllegalArgumentException("设备卫星不存在或已删除");
        }
        // 第二步：复用协议引用保护，仍被本地PDXP协议使用时禁止删除。
        Map<String, DeviceSatellite> deleting = new HashMap<>();
        deleting.put(device.getName(), device);
        assertUnreferencedDevices(taskId, Collections.emptySet(), deleting);
        // 第三步：事务删除参数、系统和目录，并清理页面失效关联。
        parseRuleMapper.deleteByDevice(taskId, deviceSatelliteId);
        systemMapper.deleteByDevice(taskId, deviceSatelliteId);
        deviceSatelliteMapper.deleteByDevice(taskId, deviceSatelliteId);
        parameterSyncService.removeObsoleteParameters(taskId);
        selectionMapper.removeMissing(taskId, device.getType());
    }

    /** 删除前检查有效本地PDXP协议的设备引用。 */
    private void assertUnreferencedDevices(String taskId, Set<String> sheets,
                                            Map<String, DeviceSatellite> existing) {
        // 第一步：整理待删除设备或卫星，并保留名称用于引用提示。
        Map<Long, DeviceSatellite> removedDevices = new HashMap<Long, DeviceSatellite>();
        for (Map.Entry<String, DeviceSatellite> entry : existing.entrySet()) {
            if (!sheets.contains(entry.getKey())) {
                DeviceSatellite device = entry.getValue();
                removedDevices.put(device.getId(), device);
            }
        }
        if (removedDevices.isEmpty()) {
            return;
        }
        // 第二步：只检查仍有效的本地PDXP协议配置。
        for (CollectProtocolConfig protocol : protocolMapper.findAllByTaskId(taskId)) {
            // 只有PDXP本地协议关联设备，RPC历史主键不应阻止删除。
            if (!BusinessEnums.ProtocolType.PDXP.getValue().equals(protocol.getProtocolType())) {
                continue;
            }
            if (protocol.getConfigParams() == null || protocol.getConfigParams().trim().isEmpty()) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(protocol.getConfigParams());
                JsonNode id = root == null ? null : root.get("deviceSatelliteId");
                DeviceSatellite removedDevice = id == null ? null : removedDevices.get(id.asLong());
                if (removedDevice != null) {
                    // 第三步：用协议名称、已有枚举的中文类型及设备名称说明引用关系。
                    String protocolName = protocol.getConfigName();
                    if (protocolName == null || protocolName.trim().isEmpty()) {
                        protocolName = "未命名协议配置（编号：" + protocol.getId() + "）";
                    }
                    String deviceType = EnumData.labelOf(
                            DeviceSatelliteType.values(), removedDevice.getType());
                    if (deviceType == null) {
                        deviceType = "设备或卫星";
                    }
                    String deviceName = removedDevice.getName();
                    if (deviceName == null || deviceName.trim().isEmpty()) {
                        deviceName = removedDevice.getCode();
                    }
                    if (deviceName == null || deviceName.trim().isEmpty()) {
                        deviceName = "编号：" + removedDevice.getId();
                    }
                    throw new IllegalArgumentException("无法删除协议配置【"
                            + protocolName.trim() + "】正在使用的" + deviceType
                            + "【" + deviceName.trim() + "】");
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new IllegalArgumentException("协议配置参数格式不正确，无法执行删除", exception);
            }
        }
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
