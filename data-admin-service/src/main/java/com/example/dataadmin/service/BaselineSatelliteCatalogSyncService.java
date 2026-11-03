package com.example.dataadmin.service;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.config.BaselineSsoProperties;
import com.example.dataadmin.exception.SatelliteManagementException;
import com.example.dataadmin.storage.SatelliteTableStorage;
import com.example.dataadmin.vo.satellite.SatelliteBaselineSyncVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 复用基线查询接口，按卫星代号去重并优先覆盖本地档案，同步不下发远程配置。 */
@Service
public class BaselineSatelliteCatalogSyncService {
    /** 使用中文记录同步失败，便于与结果中的行号对应。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaselineSatelliteCatalogSyncService.class);
    /** 与现有基线同步一致，仍按总数继续翻页。 */
    private static final int PAGE_SIZE = 99999;
    /** 复用现有基线接口地址及超时配置。 */
    private final BaselineSsoProperties properties;
    /** 复用现有公共请求工具查询分页数据。 */
    private final HttpRequestTool http;
    /** 文件下载与人工上传共用本地文件保存规则。 */
    private final SatelliteTableStorage storage;
    /** 通过独立代理调用每条卫星的写事务。 */
    private final SatelliteManagementService satelliteService;

    /** 复用基线配置，不改变已有任务参数同步流程。 */
    public BaselineSatelliteCatalogSyncService(BaselineSsoProperties properties,
            SatelliteTableStorage storage, SatelliteManagementService satelliteService) {
        this.properties = properties;
        this.storage = storage;
        this.satelliteService = satelliteService;
        this.http = new HttpRequestTool(properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis());
    }

    /** 查询和可选下载均在写事务之外进行，每个代号独立新增或覆盖本地档案。 */
    public SatelliteBaselineSyncVO synchronize() {
        // 第一步：先完整查询分页，保留原始记录数，不让去重影响分页完整性检查。
        String baseUrl = requireBaseUrl();
        List<JsonNode> rows = queryAll(baseUrl);
        SatelliteBaselineSyncVO result = new SatelliteBaselineSyncVO();
        result.setQueriedCount(rows.size());
        Set<String> processedCodes = new HashSet<>();
        int updatedCount = 0;
        int duplicateCount = 0;
        // 第二步：按代号只处理本批第一条名称及代号有效的记录，不强制要求参数表信息。
        for (int index = 0; index < rows.size(); index++) {
            JsonNode satellite = rows.get(index);
            try {
                String code = requiredText(satellite, "satelliteCode", "基线卫星代号不能为空");
                if (processedCodes.contains(code)) {
                    duplicateCount++;
                    continue;
                }
                String name = requiredText(satellite, "satelliteName", "基线卫星名称不能为空");
                processedCodes.add(code);
                String url = optionalText(satellite, "telemetryUrl");
                String fileName = optionalText(satellite, "telemetryName");
                String localPath = null;
                // 第三步：仅在下载地址及文件名均存在时下载，缺少信息仍同步名称和代号。
                if (url != null && fileName != null) {
                    localPath = storage.download(resolveDownloadUrl(baseUrl, url), fileName);
                }
                // 第四步：通过代理在独立事务中新增或覆盖，绝不调用上传参数表或启停接口。
                if (satelliteService.synchronizeBaselineSatellite(name, code, localPath)) {
                    result.addCreated();
                } else {
                    updatedCount++;
                }
            } catch (RuntimeException exception) {
                // 第五步：只记录实际处理失败，缺少可选参数表字段不产生失败记录。
                String message = failureMessage(exception);
                result.getFailures().add(new SatelliteBaselineSyncVO.Failure(index + 1,
                        satellite.path("satelliteName").asText(""),
                        satellite.path("satelliteCode").asText(""), message));
                LOGGER.warn("基线卫星同步第{}条失败，请查看同步结果中的失败明细", index + 1);
            }
        }
        // 新增数量仍只统计新建档案；覆盖与去重数量记录日志，不增加接口字段。
        LOGGER.info("基线卫星同步完成，查询{}条，新增{}条，覆盖{}个代号，忽略重复{}条，失败{}条",
                rows.size(), result.getCreatedCount(), updatedCount, duplicateCount, result.getFailedCount());
        return result;
    }

    /** 分页规则沿用已有基线同步，按记录数量核对完整性而非按卫星代号计数。 */
    private List<JsonNode> queryAll(String baseUrl) {
        // 第一步：复用已经配置的卫星列表路径。
        String path = properties.getSatelliteListPath();
        if (path == null || !path.trim().startsWith("/") || path.trim().startsWith("//")) {
            throw new IllegalArgumentException("基线卫星列表接口路径未配置或格式错误");
        }
        List<JsonNode> satellites = new ArrayList<>();
        long expectedTotal = -1;
        for (int pageNum = 1; ; pageNum++) {
            // 第二步：不传卫星代号及通道筛选，完整获取所有记录。
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + path.trim())
                    .queryParam("pageNum", pageNum).queryParam("pageSize", PAGE_SIZE)
                    .build().encode().toUriString();
            JsonNode response = http.get(url, JsonNode.class);
            if (response == null || response.path("code").asInt() != 200) {
                throw new IllegalStateException("查询基线卫星列表失败");
            }
            JsonNode data = response.path("data");
            JsonNode rows = data.path("rows");
            JsonNode total = data.path("total");
            if (!rows.isArray() || !total.isIntegralNumber()
                    || !total.canConvertToLong() || total.asLong() < 0) {
                throw new IllegalArgumentException("基线卫星分页返回结构不正确");
            }
            if (expectedTotal >= 0 && expectedTotal != total.asLong()) {
                throw new IllegalStateException("基线卫星总数在同步期间变化，请重新同步");
            }
            expectedTotal = total.asLong();
            // 第三步：重复卫星代号仍逐条保留，只检查分页记录是否完整。
            for (JsonNode row : rows) {
                satellites.add(row);
            }
            if (satellites.size() == expectedTotal) {
                return satellites;
            }
            if (satellites.size() > expectedTotal || rows.size() == 0 || pageNum == Integer.MAX_VALUE) {
                throw new IllegalStateException("基线卫星分页未返回完整数据，请重新同步");
            }
        }
    }

    /** 基线根地址直接复用已有配置。 */
    private String requireBaseUrl() {
        String value = properties.getApiBaseUrl();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("基线服务地址未配置");
        }
        // 查询前统一校验地址，底层地址解析异常转换为中文提示。
        String normalized = value.trim().replaceAll("/+$", "");
        try {
            URI uri = URI.create(normalized);
            if (uri.getHost() == null || (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("基线服务地址必须是完整的HTTP或HTTPS地址");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("基线服务地址格式错误", exception);
        }
        return normalized;
    }

    /** 下载链接支持完整地址及相对基线根地址的路径。 */
    private String resolveDownloadUrl(String baseUrl, String url) {
        // 将非法地址转换为中文业务错误，避免底层英文异常进入同步结果。
        try {
            return URI.create(baseUrl + "/").resolve(url).toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("基线遥测参数表下载地址格式错误", exception);
        }
    }

    /** 读取本次同步实际需要的字段，不依赖旧的遥测数据字段。 */
    private String requiredText(JsonNode satellite, String field, String message) {
        // 代号兼容基线返回的数字及文本，不附加唯一或长度限制。
        JsonNode value = satellite.path(field);
        if ((!value.isTextual() && !value.isNumber()) || value.asText().trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.asText().trim();
    }

    /** 可选文件信息缺失、为空或不是文本值时按未提供处理，不限制名称和代号同步。 */
    private String optionalText(JsonNode satellite, String field) {
        // 只提取可用文本，不将可选字段校验变成卫星同步前置条件。
        JsonNode value = satellite.path(field);
        if (!value.isTextual()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    /** 只将业务中文提示返回前端，不暴露数据库和底层网络异常。 */
    private String failureMessage(RuntimeException exception) {
        if (exception instanceof SatelliteManagementException
                || exception instanceof IllegalStateException || exception instanceof IllegalArgumentException) {
            return exception.getMessage() == null ? "同步卫星失败，请检查参数表与远程服务" : exception.getMessage();
        }
        return "同步卫星失败，请检查数据库及远程服务状态";
    }
}
