package com.example.dataprocess.collection;

import com.example.dataprocess.entity.CalibrationChannelRuntimeConfig;
import com.example.dataprocess.mapper.CalibrationProcessingMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 加载当前采集接口任务使用的校准通道配置快照。 */
@Component
public class CalibrationRuntimeConfigManager {

    /** 校准处理数据访问组件。 */
    private final CalibrationProcessingMapper mapper;

    /** 注入校准处理数据访问组件。 */
    public CalibrationRuntimeConfigManager(CalibrationProcessingMapper mapper) {
        this.mapper = mapper;
    }

    /** 按任务加载全部有效通道，并以校准公式构造只读Map。 */
    public Map<String, CalibrationChannelRuntimeConfig> load(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        List<CalibrationChannelRuntimeConfig> configs =
                mapper.findEnabledChannels(taskId);
        Map<String, CalibrationChannelRuntimeConfig> result =
                new LinkedHashMap<String, CalibrationChannelRuntimeConfig>();
        for (CalibrationChannelRuntimeConfig config : configs) {
            String formula = trimmed(config.getCalibrationFormula());
            if (formula != null) {
                result.put(formula, config);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** 返回去除首尾空白后的公式，空字符串按未配置处理。 */
    private String trimmed(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
