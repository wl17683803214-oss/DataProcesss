package com.example.dataadmin.service;

import com.example.common.greptime.GreptimeProperties;
import com.example.common.greptime.GreptimeStore;
import com.example.common.greptime.TelemetryIdentity;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.greptime.TelemetrySourceType;
import com.example.dataadmin.entity.ProcessedTelemetryFilterSelection;
import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.DeviceSatelliteType;
import com.example.dataadmin.mapper.ProcessedTelemetryFilterSelectionMapper;
import com.example.dataadmin.vo.processing.ProcessedTelemetryCurveVO;
import com.example.dataadmin.vo.processing.ProcessedTelemetryVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

/** 按任务已勾选参数并发查询GreptimeDB最新结果与曲线。 */
@Service
public class ProcessedTelemetryQueryService {
    /** 每批提交的任务数不超过GreptimeDB查询线程数。 */
    private final int queryConcurrency;
    private final ProcessedTelemetryFilterSelectionMapper selectionMapper;
    private final GreptimeStore store;
    private final ExecutorService queryExecutor;
    private final TelemetrySelectionScopeService scopeService;

    public ProcessedTelemetryQueryService(
            ProcessedTelemetryFilterSelectionMapper selectionMapper,
            GreptimeStore store,
            GreptimeProperties queryConfig,
            TelemetrySelectionScopeService scopeService,
            @Qualifier("processedTelemetryQueryExecutor") ExecutorService queryExecutor) {
        this.selectionMapper = selectionMapper;
        this.store = store;
        this.queryExecutor = queryExecutor;
        this.scopeService = scopeService;
        this.queryConcurrency = queryConfig.getMaxPoolSize() - 1;
    }

    /** 从有效勾选记录取得查询身份，并按记录顺序汇总每个参数的最新值。 */
    public List<ProcessedTelemetryVO> latest(String taskId, Integer selectionType, Long targetId) {
        // 最新值仅接受处理后数据表格和数据可视化表格的勾选场景。
        if (selectionType != BusinessEnums.SelectionType.PROCESSED_TABLE.getCode()
                && selectionType != BusinessEnums.SelectionType.VISUALIZATION_TABLE.getCode()) {
            throw new IllegalArgumentException("最新值查询的勾选类型不正确");
        }
        return queryInBatches(selected(taskId, selectionType, targetId), this::latestOne);
    }

    /** 从有效勾选记录取得查询身份，并按记录顺序汇总每个参数的曲线。 */
    public List<ProcessedTelemetryCurveVO> curves(String taskId, Integer selectionType,
            Long targetId) {
        // 曲线仅接受处理后数据曲线和数据可视化曲线的勾选场景。
        if (selectionType != BusinessEnums.SelectionType.PROCESSED_CURVE.getCode()
                && selectionType != BusinessEnums.SelectionType.VISUALIZATION_CURVE.getCode()) {
            throw new IllegalArgumentException("曲线查询的勾选类型不正确");
        }
        return queryInBatches(selected(taskId, selectionType, targetId), this::curveOne);
    }

    /** 每批最多提交连接池容量内的查询，不一次创建所有参数的查询任务。 */
    private <T> List<T> queryInBatches(List<ProcessedTelemetryFilterSelection> selected,
            Function<ProcessedTelemetryFilterSelection, T> query) {
        List<T> result = new ArrayList<>(selected.size());
        for (int start = 0; start < selected.size(); start += queryConcurrency) {
            List<Callable<T>> tasks = new ArrayList<>();
            for (ProcessedTelemetryFilterSelection item : selected.subList(
                    start, Math.min(start + queryConcurrency, selected.size()))) {
                tasks.add(() -> query.apply(item));
            }
            try {
                for (Future<T> future : queryExecutor.invokeAll(tasks)) {
                    result.add(future.get());
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("遥测参数查询被中断", exception);
            } catch (ExecutionException exception) {
                throw new IllegalStateException("查询GreptimeDB遥测参数失败", exception.getCause());
            }
        }
        return result;
    }

    /** 单个参数只读取最新一条，缺少历史值时仍返回参数身份。 */
    private ProcessedTelemetryVO latestOne(ProcessedTelemetryFilterSelection item) {
        ProcessedTelemetryVO result = new ProcessedTelemetryVO();
        result.setDeviceSatelliteId(item.getDeviceSatelliteId());
        result.setTelemetryCode(item.getTelemetryCode());
        result.setParameter(item.getTelemetryName());
        // 只改变数据库读取，未采集到数据时仍返回原有参数身份和空值。
        TelemetryParameterRecord row = store.latest(query(item));
        if (row != null) {
            if (row.getTelemetryName() != null && !row.getTelemetryName().trim().isEmpty()) {
                result.setParameter(row.getTelemetryName());
            }
            result.setParameterValue(row.getValue());
            result.setStatus(row.getStateName());
            if (row.getProcessedAt() != null) {
                result.setProcessTime(row.getProcessedAt().toLocalDateTime());
            }
        }
        return result;
    }

    /** 先取最近一百二十个有效点，再按时间升序返回。 */
    private ProcessedTelemetryCurveVO curveOne(ProcessedTelemetryFilterSelection item) {
        ProcessedTelemetryCurveVO result = new ProcessedTelemetryCurveVO();
        result.setDeviceSatelliteId(item.getDeviceSatelliteId());
        result.setTelemetryCode(item.getTelemetryCode());
        result.setParameter(item.getTelemetryName());
        // 查询只读取时间和数值，不读取参数原码及文本。
        for (TelemetryParameterRecord row : store.curve(query(item))) {
            if (row.getSourceTime() != null) {
                result.getTimes().add(row.getSourceTime().getTime());
                result.getValues().add(row.getValue());
            }
        }
        Collections.reverse(result.getTimes());
        Collections.reverse(result.getValues());
        return result;
    }

    /** 保留本地设备精确匹配、远程任务范围匹配的既有业务规则。 */
    private Map<String, Object> query(ProcessedTelemetryFilterSelection item) {
        Map<String, Object> query = new HashMap<>();
        query.put("taskId", item.getTaskId().trim());
        query.put("telemetryCode", item.getTelemetryCode());
        // 来源枚举复用公共定义，身份不再转义为数据库路径。
        if (DeviceSatelliteType.SATELLITE.getValue().equals(item.getDeviceSatelliteType())) {
            query.put("sourceType", TelemetrySourceType.RPC.getValue());
        } else if (DeviceSatelliteType.DEVICE.getValue().equals(item.getDeviceSatelliteType())) {
            query.put("sourceType", TelemetrySourceType.LOCAL.getValue());
            query.put("sourceIdentity", TelemetryIdentity.local(item.getDeviceSatelliteId()));
        } else {
            throw new IllegalStateException("已勾选参数的设备卫星类型不正确");
        }
        return query;
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
