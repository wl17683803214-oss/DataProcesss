package com.example.dataprocess.service.impl;

import com.example.common.calibration.CalibrationRedisKeys;
import com.example.common.calibration.CalibrationRecordDetailCache;
import com.example.dataprocess.service.CalibrationPersistenceWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 按配置周期将缓存中的校准野值详情批量写入数据库。 */
@Component
public class CalibrationPersistenceScheduler {

    /** 校准落库日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CalibrationPersistenceScheduler.class);
    /** 字符串缓存访问组件。 */
    private final StringRedisTemplate redisTemplate;
    /** 野值详情序列化组件。 */
    private final ObjectMapper objectMapper;
    /** 事务写入组件。 */
    private final CalibrationPersistenceWriter writer;
    /** 单次最多写入的野值详情数量。 */
    private final int batchSize;

    /** 注入定时落库所需组件。 */
    public CalibrationPersistenceScheduler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CalibrationPersistenceWriter writer,
            @Value("${calibration.persistence-batch-size:500}") int batchSize) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.writer = writer;
        this.batchSize = batchSize;
    }

    /** 默认每五秒取出一个批次，具体周期由配置文件覆盖。 */
    @Scheduled(fixedDelayString = "${calibration.persistence-interval-millis:5000}")
    public void flush() {
        List<String> jsonValues = popBatch();
        if (jsonValues.isEmpty()) {
            return;
        }
        try {
            List<CalibrationRecordDetailCache> details =
                    new ArrayList<CalibrationRecordDetailCache>(
                    jsonValues.size());
            for (String json : jsonValues) {
                details.add(objectMapper.readValue(
                        json, CalibrationRecordDetailCache.class));
            }
            writer.write(details);
        } catch (Exception exception) {
            // 数据库或反序列化失败时恢复原队列顺序，等待下次定时任务重试。
            restore(jsonValues);
            LOGGER.error("校准野值详情批量写入数据库失败，本批次已恢复到缓存队列", exception);
        }
    }

    /** 从队首取出一个受配置限制的批次。 */
    private List<String> popBatch() {
        List<String> result = new ArrayList<String>(batchSize);
        try {
            for (int index = 0; index < batchSize; index++) {
                String value = redisTemplate.opsForList().leftPop(
                        CalibrationRedisKeys.PENDING_QUEUE);
                if (value == null) {
                    break;
                }
                result.add(value);
            }
        } catch (Exception exception) {
            restore(result);
            LOGGER.error("读取待落库校准野值详情失败", exception);
            return Collections.emptyList();
        }
        return result;
    }

    /** 从后向前压回队首，恢复原来的消费顺序。 */
    private void restore(List<String> jsonValues) {
        try {
            for (int index = jsonValues.size() - 1; index >= 0; index--) {
                redisTemplate.opsForList().leftPush(
                        CalibrationRedisKeys.PENDING_QUEUE, jsonValues.get(index));
            }
        } catch (Exception exception) {
            LOGGER.error("恢复待落库校准野值详情失败", exception);
        }
    }
}
