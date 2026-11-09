package com.example.dataadmin.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.common.telemetry.TelemetryRealtimeCache;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.enums.TelemetryTypeOptions;
import com.example.dataadmin.mapper.ProcessedTelemetryFilterSelectionMapper;
import com.example.dataadmin.service.support.TelemetryQueryTimeRange;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import java.time.LocalDateTime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 按任务勾选参数和时间范围，分别以单条语句查询最新值或曲线。 */
@Service
public class ProcessedTelemetryQueryService {
    /** 业务库中的参数勾选记录。 */
    private final ProcessedTelemetryFilterSelectionMapper selectionMapper;
    /** 两种查询共用的独立时序存储组件。 */
    private final GreptimeStore store;
    /** 勾选场景与可视化组件归属校验。 */
    private final TelemetrySelectionScopeService scopeService;
    /** 实时查询只读取共享缓存，历史接口继续使用时序存储。 */
    private final TelemetryRealtimeCache realtimeCache;

    /** 复用既有勾选和存储组件，不再为每个参数提交并发任务。 */
    public ProcessedTelemetryQueryService(ProcessedTelemetryFilterSelectionMapper selectionMapper,
            GreptimeStore store, TelemetrySelectionScopeService scopeService, TelemetryRealtimeCache realtimeCache) {
        this.selectionMapper = selectionMapper;
        this.store = store;
        this.scopeService = scopeService;
        this.realtimeCache = realtimeCache;
    }

    /** 订阅时校验场景与组件归属，后续推送按本次勾选快照匹配。 */
    public LiveSelection prepareLive(String taskId, Integer selectionType, Long targetId,
            String telemetryType, boolean curve) {
        BusinessEnums.SelectionType scene = BusinessEnums.SelectionType.fromCode(selectionType);
        boolean curveScene = scene == BusinessEnums.SelectionType.PROCESSED_CURVE
                || scene == BusinessEnums.SelectionType.VISUALIZATION_CURVE;
        if (curve != curveScene) {
            throw new IllegalArgumentException("订阅类别与参数勾选场景不匹配");
        }
        String normalizedType = TelemetryTypeOptions.normalize(telemetryType);
        List<ProcessedTelemetryFilterSelection> selections = selected(taskId, selectionType, targetId);
        return new LiveSelection(taskId.trim(), normalizedType, curve, selections);
    }

    /** 一次订阅的勾选范围和表格已发送时间，由该连接的发送任务顺序访问。 */
    public final class LiveSelection {
        private final String taskId;
        private final String telemetryType;
        private final boolean curve;
        private final List<ProcessedTelemetryFilterSelection> selections;
        private final Map<ParameterIdentity, ProcessedTelemetryFilterSelection> selectedByIdentity = new LinkedHashMap<>();
        private final Map<ParameterIdentity, Long> lastSentTimes = new HashMap<>();

        private LiveSelection(String taskId, String telemetryType, boolean curve,
                List<ProcessedTelemetryFilterSelection> selections) {
            this.taskId = taskId;
            this.telemetryType = telemetryType;
            this.curve = curve;
            this.selections = selections;
            for (ProcessedTelemetryFilterSelection item : selections) {
                selectedByIdentity.put(identity(item), item);
            }
        }

        /** 表格从缓存取得各勾选参数最新值，曲线的历史数据由原查询接口提供。 */
        public List<ProcessedTelemetryVO> snapshot() throws IOException {
            if (curve || selections.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            Map<ParameterIdentity, TelemetryParameterRecord> cached = new HashMap<>();
            for (TelemetryParameterRecord row : realtimeCache.latest(taskId, parameterFilters(selections), telemetryType)) {
                ParameterIdentity key = identity(row);
                cached.put(key, row);
                lastSentTimes.put(key, row.getSourceTime().getTime());
            }
            List<ProcessedTelemetryVO> result = new ArrayList<>(selections.size());
            for (ProcessedTelemetryFilterSelection item : selections) {
                result.add(latestResult(item, cached.get(identity(item))));
            }
            return result;
        }

        /** 曲线保留所有有限数值点，表格只发送批内最新值。 */
        public List<ProcessedTelemetryVO> updates(List<TelemetryParameterRecord> rows) {
            List<ProcessedTelemetryVO> result = new ArrayList<>();
            Map<ParameterIdentity, TelemetryParameterRecord> latest = new LinkedHashMap<>();
            for (TelemetryParameterRecord row : rows) {
                // 以任务、遥测类型及参数身份同时约束推送。
                if (!taskId.equals(row.getTaskId())
                        || (telemetryType != null && !telemetryType.equals(row.getTelemetryType()))) {
                    continue;
                }
                ParameterIdentity key = identity(row);
                ProcessedTelemetryFilterSelection item = selectedByIdentity.get(key);
                if (item == null) {
                    continue;
                }
                if (curve) {
                    if (row.getValue() != null && Double.isFinite(row.getValue())) {
                        result.add(latestResult(item, row));
                    }
                } else {
                    TelemetryParameterRecord previous = latest.get(key);
                    if (previous == null || !row.getSourceTime().before(previous.getSourceTime())) {
                        latest.put(key, row);
                    }
                }
            }
            // 表格初始化期间缓存可能先看到后续增量，旧值不能覆盖快照。
            for (Map.Entry<ParameterIdentity, TelemetryParameterRecord> entry : latest.entrySet()) {
                ParameterIdentity key = entry.getKey();
                TelemetryParameterRecord row = entry.getValue();
                long time = row.getSourceTime().getTime();
                if (time >= lastSentTimes.getOrDefault(key, Long.MIN_VALUE)) {
                    lastSentTimes.put(key, time);
                    result.add(latestResult(selectedByIdentity.get(key), row));
                }
            }
            return result;
        }
    }

    /** 一次读取所有勾选参数的最新值，并按勾选顺序返回原有结构。 */
    public List<ProcessedTelemetryVO> latest(String taskId, Integer selectionType, Long targetId,
            String telemetryType, LocalDateTime startTime, LocalDateTime endTime) {
        // 第一步：固定本次时间范围，并复用协议类型和勾选场景校验。
        TelemetryQueryTimeRange range = TelemetryQueryTimeRange.resolve(startTime, endTime);
        String normalizedType = TelemetryTypeOptions.normalize(telemetryType);
        if (selectionType != BusinessEnums.SelectionType.PROCESSED_TABLE.getCode()
                && selectionType != BusinessEnums.SelectionType.VISUALIZATION_TABLE.getCode()) {
            throw new IllegalArgumentException("最新值查询的勾选类型不正确");
        }
        List<ProcessedTelemetryFilterSelection> selections = selected(taskId, selectionType, targetId);
        List<ProcessedTelemetryVO> result = new ArrayList<>(selections.size());
        if (selections.isEmpty()) {
            return result;
        }
        // 第二步：一条数据库语句完成各参数排名，结果按查询身份建立索引。
        Map<String, Object> query = parameterQuery(taskId, selections, normalizedType, range);
        Map<ParameterIdentity, TelemetryParameterRecord> latestRows = new HashMap<>();
        for (TelemetryParameterRecord row : store.latestBatch(query)) {
            latestRows.put(identity(row), row);
        }
        // 第三步：未匹配到数据的勾选参数也返回，文本值和状态沿用现有约定。
        for (ProcessedTelemetryFilterSelection item : selections) {
            result.add(latestResult(item, latestRows.get(identity(item))));
        }
        return result;
    }

    /** 一次读取所有有效曲线点，按参数归属组装各自的时间和数值数组。 */
    public List<ProcessedTelemetryCurveVO> curves(String taskId, Integer selectionType,
            Long targetId, String telemetryType, LocalDateTime startTime, LocalDateTime endTime) {
        // 第一步：固定时间范围，保留处理后曲线与可视化曲线的场景约束。
        TelemetryQueryTimeRange range = TelemetryQueryTimeRange.resolve(startTime, endTime);
        String normalizedType = TelemetryTypeOptions.normalize(telemetryType);
        if (selectionType != BusinessEnums.SelectionType.PROCESSED_CURVE.getCode()
                && selectionType != BusinessEnums.SelectionType.VISUALIZATION_CURVE.getCode()) {
            throw new IllegalArgumentException("曲线查询的勾选类型不正确");
        }
        List<ProcessedTelemetryFilterSelection> selections = selected(taskId, selectionType, targetId);
        List<ProcessedTelemetryCurveVO> result = new ArrayList<>(selections.size());
        if (selections.isEmpty()) {
            return result;
        }
        // 第二步：先按勾选顺序创建空曲线，设备和卫星均按自己的对象身份归组。
        Map<ParameterIdentity, List<ProcessedTelemetryCurveVO>> curvesByIdentity = new HashMap<>();
        for (ProcessedTelemetryFilterSelection item : selections) {
            ProcessedTelemetryCurveVO curve = new ProcessedTelemetryCurveVO();
            curve.setDeviceSatelliteId(item.getDeviceSatelliteId());
            curve.setTelemetryCode(item.getTelemetryCode());
            curve.setParameter(item.getTelemetryName());
            result.add(curve);
            curvesByIdentity.computeIfAbsent(identity(item), ignored -> new ArrayList<>()).add(curve);
        }
        // 第三步：数据库统一按时间升序返回，逐点归组并同时写入时间与数值。
        Map<String, Object> query = parameterQuery(taskId, selections, normalizedType, range);
        for (TelemetryParameterRecord row : store.curves(query)) {
            List<ProcessedTelemetryCurveVO> targets = curvesByIdentity.get(identity(row));
            if (targets == null || row.getSourceTime() == null || row.getValue() == null) {
                continue;
            }
            for (ProcessedTelemetryCurveVO curve : targets) {
                curve.getTimes().add(row.getSourceTime().getTime());
                curve.getValues().add(row.getValue());
            }
        }
        return result;
    }

    /** 将最新记录转换为既有页面结构，保留文本类型和无数据参数。 */
    private ProcessedTelemetryVO latestResult(ProcessedTelemetryFilterSelection item, TelemetryParameterRecord row) {
        // 第一步：始终返回勾选参数的身份与配置名称。
        ProcessedTelemetryVO result = new ProcessedTelemetryVO();
        result.setDeviceSatelliteId(item.getDeviceSatelliteId());
        result.setTelemetryCode(item.getTelemetryCode());
        result.setParameter(item.getTelemetryName());
        if (row == null) {
            return result;
        }
        // 第二步：有记录时补充最新值、状态和实际处理时间。
        if (row.getTelemetryName() != null && !row.getTelemetryName().trim().isEmpty()) {
            result.setParameter(row.getTelemetryName());
        }
        result.setParameterValue(ValueType.VALUE_TYPE_TEXT.name().equals(row.getValueType())
                ? row.getValueText() : row.getValue());
        result.setStatus(row.getStateName());
        if (row.getProcessedAt() != null) {
            result.setProcessTime(row.getProcessedAt().toLocalDateTime());
        }
        return result;
    }

    /** 合并并去重全部查询条件，时间和任务仅绑定一次。 */
    private Map<String, Object> parameterQuery(String taskId, List<ProcessedTelemetryFilterSelection> selections,
            String telemetryType, TelemetryQueryTimeRange range) {
        // 第一步：复用来源枚举，把同一任务的公共筛选条件交给映射处理。
        Map<String, Object> query = new HashMap<>();
        query.put("taskId", taskId.trim());
        query.put("telemetryType", telemetryType);
        range.applyTo(query);
        query.put("filters", parameterFilters(selections));
        return query;
    }

    /** 历史与实时接口共用参数身份，避免卫星代号相同时跨对象混合。 */
    private List<Map<String, Object>> parameterFilters(List<ProcessedTelemetryFilterSelection> selections) {
        // 第一步：按对象身份与代号去重，同代号的不同卫星保留各自条件。
        Set<ParameterIdentity> identities = new LinkedHashSet<>();
        for (ProcessedTelemetryFilterSelection item : selections) {
            identities.add(identity(item));
        }
        // 第二步：保留明确的筛选字段，历史映射与实时缓存共用同一份条件。
        List<Map<String, Object>> filters = new ArrayList<>(identities.size());
        for (ParameterIdentity identity : identities) {
            Map<String, Object> filter = new HashMap<>();
            filter.put("sourceType", identity.sourceType);
            filter.put("sourceIdentity", identity.sourceIdentity);
            filter.put("telemetryCode", identity.telemetryCode);
            filters.add(filter);
        }
        return filters;
    }

    /** 本地按设备主键匹配，远程使用同次勾选查询关联取得的卫星编码。 */
    private ParameterIdentity identity(ProcessedTelemetryFilterSelection item) {
        // 根据所属资料判断来源，不使用页面传入的类型推断身份。
        if (DeviceSatelliteType.SATELLITE.getValue().equals(item.getDeviceSatelliteType())) {
            return new ParameterIdentity(TelemetrySourceType.RPC.getValue(),
                    TelemetryIdentity.rpc(item.getDeviceSatelliteCode()), item.getTelemetryCode());
        }
        if (DeviceSatelliteType.DEVICE.getValue().equals(item.getDeviceSatelliteType())) {
            return new ParameterIdentity(TelemetrySourceType.LOCAL.getValue(),
                    TelemetryIdentity.local(item.getDeviceSatelliteId()), item.getTelemetryCode());
        }
        throw new IllegalStateException("已勾选参数的设备卫星类型不正确");
    }

    /** 数据库结果使用与筛选条件相同的身份规则归组。 */
    private ParameterIdentity identity(TelemetryParameterRecord row) {
        // 分表记录保存原始对象身份，与勾选条件按相同字段匹配，不跨卫星归组。
        return new ParameterIdentity(row.getSourceType(), row.getSourceIdentity(), row.getTelemetryCode());
    }

    /** 查询任务下仍有效的勾选参数，任务编号不允许为空。 */
    private List<ProcessedTelemetryFilterSelection> selected(String taskId, Integer selectionType, Long targetId) {
        // 先校验任务和组件归属，再读取一次业务库勾选列表。
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        long scopeId = scopeService.require(taskId.trim(), selectionType, targetId);
        return selectionMapper.findSelected(taskId.trim(), selectionType, scopeId);
    }

    /** 参数的查询身份，按字段比较，避免拼接字符串造成分隔符冲突。 */
    private static final class ParameterIdentity {
        /** 数据来源英文编码。 */
        private final String sourceType;
        /** 本地设备主键或远程原始卫星代号。 */
        private final String sourceIdentity;
        /** 当前勾选的遥测代号。 */
        private final String telemetryCode;

        /** 保留完整对象身份，与分表路由和数据库窗口分组保持一致。 */
        private ParameterIdentity(String sourceType, String sourceIdentity, String telemetryCode) {
            this.sourceType = sourceType;
            this.sourceIdentity = sourceIdentity;
            this.telemetryCode = telemetryCode;
        }

        /** 同来源、查询身份和代号表示同一组查询结果。 */
        @Override
        public boolean equals(Object other) {
            // 先检查对象类型，再逐字段判断是否属于相同参数。
            if (!(other instanceof ParameterIdentity)) {
                return false;
            }
            ParameterIdentity identity = (ParameterIdentity) other;
            return Objects.equals(sourceType, identity.sourceType)
                    && Objects.equals(sourceIdentity, identity.sourceIdentity)
                    && Objects.equals(telemetryCode, identity.telemetryCode);
        }

        /** 使用与相等判断相同的字段生成散列值。 */
        @Override
        public int hashCode() {
            return Objects.hash(sourceType, sourceIdentity, telemetryCode);
        }
    }
}
