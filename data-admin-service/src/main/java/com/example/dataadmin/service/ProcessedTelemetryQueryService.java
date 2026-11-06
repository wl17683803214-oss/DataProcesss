package com.example.dataadmin.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.enums.TelemetryTypeOptions;
import com.example.dataadmin.mapper.ProcessedTelemetryFilterSelectionMapper;
import com.example.dataadmin.service.support.TelemetryQueryTimeRange;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

    /** 复用既有勾选和存储组件，不再为每个参数提交并发任务。 */
    public ProcessedTelemetryQueryService(ProcessedTelemetryFilterSelectionMapper selectionMapper,
            GreptimeStore store, TelemetrySelectionScopeService scopeService) {
        this.selectionMapper = selectionMapper;
        this.store = store;
        this.scopeService = scopeService;
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
        // 第二步：先按勾选顺序创建空曲线，同一查询身份可以对应多个勾选项。
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
        query.put("localSourceType", TelemetrySourceType.LOCAL.getValue());
        query.put("rpcSourceType", TelemetrySourceType.RPC.getValue());
        range.applyTo(query);
        // 第二步：同一远程代号可能被多次勾选，去重后避免重复拼接条件。
        Set<ParameterIdentity> identities = new LinkedHashSet<>();
        for (ProcessedTelemetryFilterSelection item : selections) {
            identities.add(identity(item));
        }
        List<Map<String, Object>> filters = new ArrayList<>(identities.size());
        for (ParameterIdentity identity : identities) {
            Map<String, Object> filter = new HashMap<>();
            filter.put("sourceType", identity.sourceType);
            filter.put("sourceIdentity", identity.sourceIdentity);
            filter.put("telemetryCode", identity.telemetryCode);
            filters.add(filter);
        }
        query.put("filters", filters);
        return query;
    }

    /** 本地按设备身份匹配，远程沿用任务内按代号匹配的业务规则。 */
    private ParameterIdentity identity(ProcessedTelemetryFilterSelection item) {
        // 根据所属资料判断来源，不使用页面传入的类型推断身份。
        if (DeviceSatelliteType.SATELLITE.getValue().equals(item.getDeviceSatelliteType())) {
            return new ParameterIdentity(TelemetrySourceType.RPC.getValue(), null, item.getTelemetryCode());
        }
        if (DeviceSatelliteType.DEVICE.getValue().equals(item.getDeviceSatelliteType())) {
            return new ParameterIdentity(TelemetrySourceType.LOCAL.getValue(),
                    TelemetryIdentity.local(item.getDeviceSatelliteId()), item.getTelemetryCode());
        }
        throw new IllegalStateException("已勾选参数的设备卫星类型不正确");
    }

    /** 数据库结果使用与筛选条件相同的身份规则归组。 */
    private ParameterIdentity identity(TelemetryParameterRecord row) {
        // 统一远程新旧身份，确保历史组合身份仍能归入原有勾选曲线。
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
        /** 本地设备身份；远程沿用不限定具体身份的查询规则。 */
        private final String sourceIdentity;
        /** 当前勾选的遥测代号。 */
        private final String telemetryCode;

        /** 将远程身份归一化，与数据库窗口分组保持一致。 */
        private ParameterIdentity(String sourceType, String sourceIdentity, String telemetryCode) {
            this.sourceType = sourceType;
            this.sourceIdentity = TelemetrySourceType.RPC.getValue().equals(sourceType) ? null : sourceIdentity;
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
