package com.example.dataadmin.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetryParameterBatchRecord;
import com.example.common.greptime.TelemetryParameterQuery;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.enums.TelemetryTypeOptions;
import com.example.dataadmin.mapper.ProcessedTelemetryFilterSelectionMapper;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 按任务已勾选参数，用单条语句分别查询最新结果与曲线。 */
@Service
public class ProcessedTelemetryQueryService {
    /** 复用现有勾选查询，保留场景和有效参数筛选规则。 */
    private final ProcessedTelemetryFilterSelectionMapper selectionMapper;
    /** 独立时序库组件，批量查询不参与业务库事务。 */
    private final GreptimeStore store;
    /** 校验固定页面和当前用户可视化组件的归属。 */
    private final TelemetrySelectionScopeService scopeService;

    /** 注入既有查询组件，不再为每个参数创建并发查询任务。 */
    public ProcessedTelemetryQueryService(
            ProcessedTelemetryFilterSelectionMapper selectionMapper,
            GreptimeStore store,
            TelemetrySelectionScopeService scopeService) {
        this.selectionMapper = selectionMapper;
        this.store = store;
        this.scopeService = scopeService;
    }

    /** 从有效勾选记录取得查询身份，并按记录顺序汇总每个参数的最新值。 */
    public List<ProcessedTelemetryVO> latest(String taskId, Integer selectionType, Long targetId,
            String telemetryType) {
        // 第一步：复用协议类型校验，非法输入直接返回中文提示。
        String normalizedType = TelemetryTypeOptions.normalize(telemetryType);
        // 第二步：最新值仅接受处理后数据表格和数据可视化表格场景。
        if (selectionType == null
                || (selectionType != BusinessEnums.SelectionType.PROCESSED_TABLE.getCode()
                && selectionType != BusinessEnums.SelectionType.VISUALIZATION_TABLE.getCode())) {
            throw new IllegalArgumentException("最新值查询的勾选类型不正确");
        }
        // 第三步：沿用原有勾选列表及其顺序，无历史数据的参数也先保留身份。
        List<ProcessedTelemetryFilterSelection> selected = selected(taskId, selectionType, targetId);
        List<TelemetryParameterQuery> queries = queries(selected);
        List<ProcessedTelemetryVO> result = new ArrayList<>(selected.size());
        for (ProcessedTelemetryFilterSelection item : selected) {
            ProcessedTelemetryVO value = new ProcessedTelemetryVO();
            value.setDeviceSatelliteId(item.getDeviceSatelliteId());
            value.setTelemetryCode(item.getTelemetryCode());
            value.setParameter(item.getTelemetryName());
            result.add(value);
        }
        // 第四步：只执行一次时序查询，再按内部位置填充各参数的最新值。
        for (TelemetryParameterBatchRecord row : store.latestBatch(taskId.trim(), queries, normalizedType)) {
            fillLatest(result.get(row.getSelectionIndex()), row);
        }
        return result;
    }

    /** 从有效勾选记录取得查询身份，并按记录顺序汇总每个参数的曲线。 */
    public List<ProcessedTelemetryCurveVO> curves(String taskId, Integer selectionType,
            Long targetId, String telemetryType) {
        // 第一步：曲线和最新值使用相同的类型校验与空白处理规则。
        String normalizedType = TelemetryTypeOptions.normalize(telemetryType);
        // 第二步：曲线仅接受处理后数据曲线和数据可视化曲线场景。
        if (selectionType == null
                || (selectionType != BusinessEnums.SelectionType.PROCESSED_CURVE.getCode()
                && selectionType != BusinessEnums.SelectionType.VISUALIZATION_CURVE.getCode())) {
            throw new IllegalArgumentException("曲线查询的勾选类型不正确");
        }
        // 第三步：先保留全部已勾选曲线，未采集的参数返回空时间轴和空数值。
        List<ProcessedTelemetryFilterSelection> selected = selected(taskId, selectionType, targetId);
        List<TelemetryParameterQuery> queries = queries(selected);
        List<ProcessedTelemetryCurveVO> result = new ArrayList<>(selected.size());
        for (ProcessedTelemetryFilterSelection item : selected) {
            ProcessedTelemetryCurveVO curve = new ProcessedTelemetryCurveVO();
            curve.setDeviceSatelliteId(item.getDeviceSatelliteId());
            curve.setTelemetryCode(item.getTelemetryCode());
            curve.setParameter(item.getTelemetryName());
            result.add(curve);
        }
        // 第四步：数据库每组最多返回一百二十点，并已按原有反转规则排好时间顺序。
        for (TelemetryParameterBatchRecord row : store.curveBatch(taskId.trim(), queries, normalizedType)) {
            if (row.getSourceTime() != null) {
                ProcessedTelemetryCurveVO curve = result.get(row.getSelectionIndex());
                curve.getTimes().add(row.getSourceTime().getTime());
                curve.getValues().add(row.getValue());
            }
        }
        return result;
    }

    /** 复用原有最新值转换规则，不向接口暴露内部位置和存储字段。 */
    private void fillLatest(ProcessedTelemetryVO result, TelemetryParameterRecord row) {
        // 第一步：实际参数名称非空时覆盖配置名称，否则保留配置名称。
        if (row.getTelemetryName() != null && !row.getTelemetryName().trim().isEmpty()) {
            result.setParameter(row.getTelemetryName());
        }
        // 第二步：文本类型返回原始文本，其余类型保持数值返回和原有空值语义。
        result.setParameterValue(ValueType.VALUE_TYPE_TEXT.name().equals(row.getValueType())
                ? row.getValueText() : row.getValue());
        result.setStatus(row.getStateName());
        // 第三步：处理时间继续采用已有接口的本地时间格式。
        if (row.getProcessedAt() != null) {
            result.setProcessTime(row.getProcessedAt().toLocalDateTime());
        }
    }

    /** 将有效勾选转为内部查询条件，位置编号防止相同代号的结果互相覆盖。 */
    private List<TelemetryParameterQuery> queries(List<ProcessedTelemetryFilterSelection> selected) {
        List<TelemetryParameterQuery> queries = new ArrayList<>(selected.size());
        // 第一步：沿用勾选列表顺序，每个参数分配独立位置。
        for (int index = 0; index < selected.size(); index++) {
            ProcessedTelemetryFilterSelection item = selected.get(index);
            // 第二步：复用已有设备卫星和来源枚举，不使用展示名称参与数据库筛选。
            if (DeviceSatelliteType.SATELLITE.getValue().equals(item.getDeviceSatelliteType())) {
                queries.add(new TelemetryParameterQuery(index, TelemetrySourceType.RPC.getValue(),
                        null, item.getTelemetryCode()));
            } else if (DeviceSatelliteType.DEVICE.getValue().equals(item.getDeviceSatelliteType())) {
                queries.add(new TelemetryParameterQuery(index, TelemetrySourceType.LOCAL.getValue(),
                        TelemetryIdentity.local(item.getDeviceSatelliteId()), item.getTelemetryCode()));
            } else {
                throw new IllegalStateException("已勾选参数的设备卫星类型不正确");
            }
        }
        return queries;
    }

    /** 查询任务下仍有效的勾选参数，任务编号不允许为空。 */
    private List<ProcessedTelemetryFilterSelection> selected(String taskId,
            Integer selectionType, Long targetId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        long scopeId = scopeService.require(taskId.trim(), selectionType, targetId);
        return selectionMapper.findSelected(taskId.trim(), selectionType, scopeId);
    }
}
