package com.example.dataadmin.service;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.config.BaselineSsoProperties;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.service.support.ParameterWorkbookParser;
import com.example.dataadmin.vo.processing.BaselineSatelliteSyncVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 在数据库事务外查询并校验基线卫星，全部有效后统一导入。 */
@Service
public class BaselineSatelliteSyncService {
    /** 每页请求全部卫星，仍超出时继续翻页。 */
    private static final int PAGE_SIZE = 99999;
    /** 复用已有基线地址和超时配置。 */
    private final BaselineSsoProperties properties;
    /** 复用公共请求工具。 */
    private final HttpRequestTool http;
    /** 统一读取响应和遥测字符串。 */
    private final ObjectMapper objectMapper;
    /** 复用参数转换和业务字段校验。 */
    private final ParameterWorkbookParser parser;
    /** 通过代理调用现有写事务。 */
    private final DataProcessingService processingService;

    /** 初始化共享配置及业务组件，不增加独立配置项。 */
    public BaselineSatelliteSyncService(BaselineSsoProperties properties,
            ObjectMapper objectMapper, ParameterWorkbookParser parser,
            DataProcessingService processingService) {
        // 复用公共工具及基线超时，不在配置中引入占位表达式。
        this.properties = properties;
        this.http = new HttpRequestTool(properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis());
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.processingService = processingService;
    }

    /** 查询全部卫星并将有效遥测参数同步至指定任务。 */
    public BaselineSatelliteSyncVO synchronize(String taskId) {
        // 第一步：校验调用上下文，禁止无任务或无地址发起同步。
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        String baseUrl = properties.getApiBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("基线服务地址未配置");
        }
        // 卫星同步与采集目录使用相同配置项，不再在代码中固定接口路径。
        String listPath = properties.getSatelliteListPath();
        if (listPath == null || !listPath.trim().startsWith("/") || listPath.trim().startsWith("//")) {
            throw new IllegalArgumentException("基线卫星列表接口路径未配置或格式错误");
        }
        Map<String, List<TelemetryParseRuleConfig>> rules = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        Set<String> seenCodes = new HashSet<>();
        int skipped = 0;
        long expectedTotal = -1;
        // 第二步：不传卫星和通道过滤，按基线总数读取全部分页。
        for (int pageNum = 1; ; pageNum++) {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl.trim().replaceAll("/+$", "") + listPath.trim())
                    .queryParam("pageNum", pageNum).queryParam("pageSize", PAGE_SIZE)
                    .build().encode().toUriString();
            JsonNode response = http.get(url, JsonNode.class);
            if (response == null || response.path("code").asInt() != 200) {
                throw new IllegalStateException("查询基线卫星失败：" + (response == null ? "未返回响应" :
                        response.path("msg").asText(response.path("message").asText("业务返回失败"))));
            }
            JsonNode data = response.path("data");
            JsonNode rows = data.path("rows");
            JsonNode total = data.path("total");
            if (!rows.isArray() || !total.isIntegralNumber() || !total.canConvertToLong() || total.asLong() < 0) {
                throw new IllegalArgumentException("基线卫星分页返回结构不正确");
            }
            if (expectedTotal < 0) {
                expectedTotal = total.asLong();
            } else if (expectedTotal != total.asLong()) {
                throw new IllegalStateException("基线卫星总数在同步期间发生变化，请重试");
            }
            // 第三步：按卫星编码识别身份，空遥测保留旧配置，其余数据必须完整有效。
            for (JsonNode satellite : rows) {
                String code = requiredText(satellite.path("satelliteCode"), "卫星编码");
                String name = requiredText(satellite.path("satelliteName"), "卫星名称");
                if (!seenCodes.add(code)) {
                    throw new IllegalArgumentException("基线返回重复卫星编码：" + code);
                }
                JsonNode telemetry = satellite.get("telemetryData");
                if (telemetry == null || telemetry.isNull()
                        || (telemetry.isTextual() && telemetry.asText().trim().isEmpty())) {
                    skipped++;
                    continue;
                }
                if (telemetry.isTextual()) {
                    try {
                        telemetry = objectMapper.readTree(telemetry.asText());
                    } catch (java.io.IOException exception) {
                        throw new IllegalArgumentException("卫星【" + name + "】遥测数据不是有效JSON", exception);
                    }
                }
                if (telemetry == null || !telemetry.isObject()) {
                    throw new IllegalArgumentException("卫星【" + name + "】遥测数据必须为对象");
                }
                rules.put(code, parser.parseBaseline(taskId, name, telemetry));
                names.put(code, name);
            }
            // 第四步：核对分页完整性，防止基线忽略翻页或提前返回空页而覆盖部分配置。
            if (seenCodes.size() > expectedTotal) {
                throw new IllegalStateException("基线卫星记录数超过返回总数，请重试");
            }
            if (seenCodes.size() == expectedTotal) {
                break;
            }
            if (rows.size() == 0 || pageNum == Integer.MAX_VALUE) {
                throw new IllegalStateException("基线卫星分页未返回完整数据，请重试");
            }
        }
        // 第五步：所有远程数据校验完成后，通过独立写事务统一替换并返回统计。
        int imported = processingService.importBaselineRules(taskId, rules, names);
        return new BaselineSatelliteSyncVO(seenCodes.size(), rules.size(), skipped, imported);
    }

    /** 卫星名称与编码使用现有目录字段长度，禁止截断引起身份冲突。 */
    private String requiredText(JsonNode value, String fieldName) {
        // 仅接受文本或数值标识，统一去除首尾空格后检查必填和长度。
        if (!value.isTextual() && !value.isNumber()) {
            throw new IllegalArgumentException("基线" + fieldName + "格式错误");
        }
        String text = value.asText().trim();
        if (text.isEmpty() || text.length() > 100) {
            throw new IllegalArgumentException("基线" + fieldName + "不能为空且不能超过100个字符");
        }
        return text;
    }
}
