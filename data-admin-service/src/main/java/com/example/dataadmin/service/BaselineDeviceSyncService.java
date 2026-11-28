package com.example.dataadmin.service;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.config.BaselineSsoProperties;
import com.example.dataadmin.entity.TelemetryParseRuleConfig;
import com.example.dataadmin.service.support.ParameterWorkbookParser;
import com.example.dataadmin.storage.SatelliteTableStorage;
import com.example.dataadmin.vo.processing.BaselineDeviceSyncVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 查询基线设备参数表，在全部校验完成后复用现有事务导入。 */
@Service
public class BaselineDeviceSyncService {
    /** 使用中文记录跳过原因及实际导入数量，避免把请求成功误认为已导入参数。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaselineDeviceSyncService.class);
    /** 与卫星同步一致的分页大小。 */
    private static final int PAGE_SIZE = 99999;
    /** 基线地址、接口路径和超时配置。 */
    private final BaselineSsoProperties properties;
    /** 公共请求工具。 */
    private final HttpRequestTool http;
    /** 与卫星参数同步共用内存下载，不在本地保存参数表。 */
    private final SatelliteTableStorage storage;
    /** 同步和人工上传共用固定列顺序及参数校验。 */
    private final ParameterWorkbookParser parser;
    /** 通过代理进入独立写事务。 */
    private final DataProcessingService processingService;

    /** 复用已有基线配置和导入组件。 */
    public BaselineDeviceSyncService(BaselineSsoProperties properties, SatelliteTableStorage storage,
            ParameterWorkbookParser parser, DataProcessingService processingService) {
        // 初始化共享工具，远程请求不进入数据库事务。
        this.properties = properties;
        this.http = new HttpRequestTool(properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis());
        this.storage = storage;
        this.parser = parser;
        this.processingService = processingService;
    }

    /** 分页查询全部参数表，将每个工作表作为一个设备导入。 */
    public BaselineDeviceSyncVO synchronize(String taskId) {
        // 第一步：校验基线配置，禁止缺少接口路径时向根地址发起写请求。
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
            // 兼容基线原始分页和接口包装后的分页，后续统一核对完整性。
            JsonNode data = response.has("rows") || response.has("total") ? response : response.path("data");
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
                // 参数以原始文件为准，不读取可能缺失或改写字段名称的参数数据。
                JsonNode fileUrl = table.path("fileUrl");
                if (fileUrl.isMissingNode() || fileUrl.isNull()
                        || (fileUrl.isTextual() && fileUrl.asText().trim().isEmpty())) {
                    skipped++;
                    LOGGER.warn("基线设备参数表缺少文件下载地址，已跳过，参数表主键：{}", id.asText());
                    continue;
                }
                if (!fileUrl.isTextual()) {
                    throw new IllegalArgumentException("设备参数表【" + id.asText() + "】文件下载地址必须为文本");
                }
                appendRules(taskId, baseUrl, table, rules);
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
        LOGGER.info("基线设备参数同步完成，查询{}张表，导入{}个设备，跳过{}张表，导入{}条参数",
                seenIds.size(), rules.size(), skipped, imported);
        return new BaselineDeviceSyncVO(seenIds.size(), rules.size(), skipped, imported);
    }

    /** 下载原始文件到内存，复用手动导入解析后再检查设备身份及跨表重名。 */
    private void appendRules(String taskId, String baseUrl, JsonNode table,
            Map<String, List<TelemetryParseRuleConfig>> rules) {
        // 第一步：文件名只用于选择解析格式，设备身份仍来自有效工作表名称。
        String tableId = table.path("id").asText();
        JsonNode fileName = table.path("fileName");
        if (!fileName.isTextual() || fileName.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("设备参数表【" + tableId + "】缺少有效文件名");
        }
        Map<String, List<TelemetryParseRuleConfig>> sheets;
        try {
            // 第二步：下载内容仅保存在本次调用的内存中，不调用文件持久化方法。
            byte[] content = storage.downloadContent(baseUrl, table.path("fileUrl").asText().trim());
            sheets = parser.parse(taskId, fileName.asText().trim(), content, false);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IllegalArgumentException("设备参数表【" + tableId + "】处理失败："
                    + exception.getMessage(), exception);
        }
        // 第三步：每个工作表单独校验序号，不合并不同设备；同名设备禁止相互覆盖。
        for (Map.Entry<String, List<TelemetryParseRuleConfig>> sheet : sheets.entrySet()) {
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
            rules.put(name, sheet.getValue());
        }
    }
}
