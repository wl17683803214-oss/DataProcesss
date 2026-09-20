package com.example.dataprocess.protocol.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 将协议动态配置中的fields数组转换为指定字段类型。 */
@Component
public class ProtocolConfigParser {

    /** 协议配置解析日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ProtocolConfigParser.class);
    /** 复用项目统一的JSON解析器。 */
    private final ObjectMapper objectMapper;
    /** 按配置原文和目标类型缓存结果，避免每帧重复解析。 */
    private final ConcurrentMap<String, List<?>> cache =
            new ConcurrentHashMap<>();

    public ProtocolConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 解析指定类型的协议配置字段列表。 */
    @SuppressWarnings("unchecked")
    public <T> List<T> parseFields(
            String configParams,
            Class<T> fieldType) {
        // 类型名和配置原文共同组成缓存键，避免不同协议类型共用错误结果。
        String normalizedConfig = configParams == null ? "" : configParams;
        String cacheKey = fieldType.getName() + "\u0000" + normalizedConfig;
        return (List<T>) cache.computeIfAbsent(
                cacheKey,
                ignored -> parseUncached(normalizedConfig, fieldType));
    }

    /** 将完整协议动态配置转换为指定的协议配置类型。 */
    public <T> T parseConfig(
            String configParams,
            Class<T> configType) {
        // 配置为空时返回一个默认配置对象，便于调用方使用协议默认值。
        String normalizedConfig = configParams == null ? "" : configParams.trim();
        try {
            if (normalizedConfig.isEmpty()) {
                return configType.getDeclaredConstructor().newInstance();
            }

            // 直接转换根对象，保留FEP等协议定义的独立配置字段。
            return objectMapper.readValue(normalizedConfig, configType);
        } catch (Exception exception) {
            // 配置异常时只提示并返回默认对象，不阻断其他采集接口启动。
            LOGGER.warn("协议动态配置格式不正确，将使用协议默认配置");
            try {
                return configType.getDeclaredConstructor().newInstance();
            } catch (Exception createException) {
                throw new IllegalStateException("无法创建协议默认配置", createException);
            }
        }
    }

    /** 对一份新的协议配置执行实际解析。 */
    private <T> List<T> parseUncached(
            String configParams,
            Class<T> fieldType) {
        // 配置为空时只提示并返回空列表。
        if (configParams.trim().isEmpty()) {
            LOGGER.warn("协议动态配置为空，将使用空字段列表");
            return Collections.emptyList();
        }

        try {
            // 第一步：读取configParams根对象中的fields数组。
            JsonNode root = objectMapper.readTree(configParams);
            JsonNode fieldsNode = root == null ? null : root.get("fields");
            if (fieldsNode == null || !fieldsNode.isArray()) {
                LOGGER.warn("协议动态配置缺少fields数组，将使用空字段列表");
                return Collections.emptyList();
            }

            // 第二步：将每个JSON字段转换为RPC或PDXP各自的字段类。
            List<T> fields = new ArrayList<>(fieldsNode.size());
            for (JsonNode fieldNode : fieldsNode) {
                fields.add(objectMapper.treeToValue(fieldNode, fieldType));
            }
            // 返回只读列表，防止调用方修改缓存内容。
            return Collections.unmodifiableList(fields);
        } catch (Exception exception) {
            // JSON或字段结构错误时只提示，不阻断当前数据处理。
            LOGGER.warn("协议动态配置格式不正确，将使用空字段列表");
            return Collections.emptyList();
        }
    }
}
