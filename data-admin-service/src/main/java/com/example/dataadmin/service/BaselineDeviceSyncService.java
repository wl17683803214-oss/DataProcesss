package com.example.dataadmin.service;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.config.BaselineSsoProperties;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.service.support.ParameterWorkbookParser;
import com.example.dataadmin.vo.processing.BaselineDeviceSyncVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 查询基线设备参数表，在全部校验完成后复用现有事务导入。 */
@Service
public class BaselineDeviceSyncService {
    /** 与卫星同步一致的分页大小。 */
    private static final int PAGE_SIZE = 99999;
    /** 基线地址、接口路径和超时配置。 */
    private final BaselineSsoProperties properties;
    /** 公共请求工具。 */
    private final HttpRequestTool http;
    /** 参数字符串解析组件。 */
    private final ObjectMapper objectMapper;
    /** 复用现有参数字段映射与校验。 */
    private final ParameterWorkbookParser parser;
    /** 通过代理进入独立写事务。 */
    private final DataProcessingService processingService;

    /** 复用已有基线配置和导入组件。 */
    public BaselineDeviceSyncService(BaselineSsoProperties properties, ObjectMapper objectMapper,
            ParameterWorkbookParser parser, DataProcessingService processingService) {
        // 初始化共享工具，远程请求不进入数据库事务。
        this.properties = properties;
        this.http = new HttpRequestTool(properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis());
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.processingService = processingService;
    }

    /** 分页查询全部参数表，将每个工作表作为一个设备导入。 */
    public BaselineDeviceSyncVO synchronize(String taskId) {
        // 第一步：校验任务和配置，禁止缺少接口路径时向根地址发起写请求。
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        String baseUrl = properties.getApiBaseUrl();
        String path = properties.getTrialParamTableListPath();
        if (baseUrl == null || baseUrl.trim().isEmpty() || path == null
                || !path.trim().startsWith("/") || path.trim().startsWith("//")) {
            throw new IllegalArgumentException("基线服务地址或设备参数表接口路径未配置或格式错误");
        }
        String url = baseUrl.trim().replaceAll("/+$", "") + path.trim();
        Map<String, List<TelemetryParseRuleConfig>> rules = new LinkedHashMap<>();
        Set<String> seenIds = new HashSet<>();
        long expectedTotal = -1;
        int skipped = 0;
        // 第二步：按文档使用POST，将分页参数放在JSON请求体中，不传额外筛选条件。
        for (int pageNum = 1; ; pageNum++) {
            Map<String, Integer> request = new LinkedHashMap<>();
            request.put("pageNum", pageNum);
            request.put("pageSize", PAGE_SIZE);
            JsonNode response = http.postJson(url, request, JsonNode.class);
            if (response == null || response.path("code").asInt() != 200) {
                throw new IllegalStateException("查询基线设备参数表失败：" + (response == null ? "未返回响应" :
                        response.path("msg").asText(response.path("message").asText("业务返回失败"))));
            }
            JsonNode data = response.path("data");
            JsonNode rows = data.path("rows");
            JsonNode total = data.path("total");
            if (!rows.isArray() || !total.isIntegralNumber() || !total.canConvertToLong() || total.asLong() < 0) {
                throw new IllegalArgumentException("基线设备参数表分页返回结构不正确");
            }
            if (expectedTotal < 0) {
                expectedTotal = total.asLong();
            } else if (expectedTotal != total.asLong()) {
                throw new IllegalStateException("基线设备参数表总数发生变化，请重试");
            }
            // 第三步：按参数表主键核对分页，设备身份沿用现有导入的工作表名称。
            for (JsonNode table : rows) {
                JsonNode id = table.path("id");
                if ((!id.isTextual() && !id.isNumber()) || id.asText().trim().isEmpty()) {
                    throw new IllegalArgumentException("基线设备参数表缺少有效主键");
                }
                if (!seenIds.add(id.asText().trim())) {
                    throw new IllegalArgumentException("基线返回重复设备参数表：" + id.asText());
                }
                JsonNode parameters = table.get("paramData");
                if (parameters == null || parameters.isNull()
                        || (parameters.isTextual() && parameters.asText().trim().isEmpty())) {
                    skipped++;
                    continue;
                }
                appendRules(taskId, id.asText(), parameters, rules);
            }
            // 第四步：总数、重复记录和空页共同校验，避免只导入分页中的部分设备。
            if (seenIds.size() > expectedTotal) {
                throw new IllegalStateException("基线设备参数表记录数超过总数，请重试");
            }
            if (seenIds.size() == expectedTotal) {
                break;
            }
            if (rows.size() == 0 || pageNum == Integer.MAX_VALUE) {
                throw new IllegalStateException("基线设备参数表分页未返回完整数据，请重试");
            }
        }
        // 第五步：全部设备校验成功后统一事务入库，不删除本次未返回的设备。
        int imported = processingService.importBaselineDeviceRules(taskId, rules);
        return new BaselineDeviceSyncVO(seenIds.size(), rules.size(), skipped, imported);
    }

    /** 按现有Excel导入方式拆分工作表，并复用参数字段校验。 */
    private void appendRules(String taskId, String tableId, JsonNode data,
            Map<String, List<TelemetryParseRuleConfig>> rules) {
        // 第一步：解析参数字符串并验证工作表结构，不使用文件下载地址。
        if (data.isTextual()) {
            try {
                data = objectMapper.readTree(data.asText());
            } catch (IOException exception) {
                throw new IllegalArgumentException("设备参数表【" + tableId + "】参数数据不是有效JSON", exception);
            }
        }
        if (data == null || !data.isObject() || !data.path("sheets").isObject()
                || data.path("sheets").size() == 0) {
            throw new IllegalArgumentException("设备参数表【" + tableId + "】没有有效工作表");
        }
        Iterator<Map.Entry<String, JsonNode>> sheets = data.path("sheets").fields();
        // 第二步：每个工作表单独校验序号，不合并不同设备；同名设备禁止相互覆盖。
        while (sheets.hasNext()) {
            Map.Entry<String, JsonNode> sheet = sheets.next();
            String name = sheet.getKey();
            int separator = name.indexOf('_');
            if (name.length() > 100 || separator <= 0
                    || name.substring(0, separator).trim().isEmpty()
                    || name.substring(separator + 1).trim().isEmpty()) {
                throw new IllegalArgumentException("设备工作表名称必须使用通道名称_业务标识格式且不超过100个字符：" + name);
            }
            if (rules.containsKey(name)) {
                throw new IllegalArgumentException("基线参数表存在重复设备工作表：" + name);
            }
            ObjectNode wrapper = objectMapper.createObjectNode();
            ObjectNode oneSheet = wrapper.putObject("sheets");
            oneSheet.set(name, sheet.getValue());
            rules.put(name, parser.parseBaseline(taskId, name, wrapper));
        }
    }
}
