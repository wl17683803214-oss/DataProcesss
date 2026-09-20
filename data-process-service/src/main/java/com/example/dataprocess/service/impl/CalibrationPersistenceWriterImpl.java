package com.example.dataprocess.service.impl;

import com.example.common.calibration.CalibrationRecordDetailCache;
import com.example.dataprocess.entity.CalibrationRecordDetail;
import com.example.dataprocess.mapper.CalibrationProcessingMapper;
import com.example.dataprocess.service.CalibrationPersistenceWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 批量写入校准野值详情和汇总数量。 */
@Service
public class CalibrationPersistenceWriterImpl implements CalibrationPersistenceWriter {

    /** 校准处理数据访问组件。 */
    private final CalibrationProcessingMapper mapper;

    /** 注入校准处理数据访问组件。 */
    public CalibrationPersistenceWriterImpl(CalibrationProcessingMapper mapper) {
        this.mapper = mapper;
    }

    /** 每个批次在同一事务内写入详情并更新汇总，避免数量与详情不一致。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void write(List<CalibrationRecordDetailCache> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        Map<Long, Integer> counts = new LinkedHashMap<Long, Integer>();
        List<CalibrationRecordDetail> records =
                new ArrayList<CalibrationRecordDetail>(details.size());
        // 先转换全部详情并按全局唯一的校准通道ID汇总数量。
        for (CalibrationRecordDetailCache detail : details) {
            records.add(record(detail));
            counts.put(detail.getChannelId(),
                    counts.getOrDefault(detail.getChannelId(), 0) + 1);
        }
        mapper.insertRecordDetails(records);
        // 详情写入后逐个通道累加数量，任一通道缺少汇总记录时整体回滚。
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            int updated = mapper.incrementRecordOutlierCount(
                    entry.getKey(), entry.getValue());
            if (updated <= 0) {
                throw new IllegalStateException(
                        "校准通道没有可用的校准记录：" + entry.getKey());
            }
        }
    }

    /** 将缓存中的野值详情转换为数据库写入实体。 */
    private CalibrationRecordDetail record(CalibrationRecordDetailCache source) {
        CalibrationRecordDetail result = new CalibrationRecordDetail();
        result.setTaskId(source.getTaskId());
        result.setChannelId(source.getChannelId());
        result.setInterfaceId(source.getInterfaceId());
        result.setChannelName(source.getChannelName());
        result.setParameterName(source.getParameterName());
        result.setTmSymbol(source.getTmSymbol());
        result.setTelemetryValue(source.getTelemetryValue());
        result.setDetail(source.getDetail());
        result.setCreateTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(source.getCreateTime()), ZoneId.systemDefault()));
        return result;
    }
}
