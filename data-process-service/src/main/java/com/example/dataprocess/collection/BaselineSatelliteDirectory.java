package com.example.dataprocess.collection;

import com.example.common.tool.HttpRequestTool;
import com.example.dataprocess.entity.BaselineSatelliteInfo;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.protocol.rpc.ProtocolConfigParser;
import com.example.dataprocess.protocol.rpc.RpcProtocolField;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 采集启动前加载基线目录，逐帧处理时只读取内存快照。 */
@Component
public class BaselineSatelliteDirectory {
    /** 目录加载日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaselineSatelliteDirectory.class);
    /** 卫星分页查询路径。 */
    private static final String LIST_PATH = "/rpc-api/knowledge/knowledge/satellite/list";
    /** 与角色接口一致的基线根地址。 */
    private final String baseUrl;
    /** 公共协议配置解析器。 */
    private final ProtocolConfigParser parser;
    /** 公共请求工具。 */
    private final HttpRequestTool http;

    /** 沿用角色查询的根地址、超时参数和无额外鉴权头的请求方式。 */
    public BaselineSatelliteDirectory(
            ProtocolConfigParser parser,
            @Value("${baseline.sso.api-base-url:}") String baseUrl,
            @Value("${baseline.sso.connect-timeout-millis:5000}") int connectTimeout,
            @Value("${baseline.sso.read-timeout-millis:10000}") int readTimeout) {
        this.parser = parser;
        this.baseUrl = baseUrl;
        this.http = new HttpRequestTool(connectTimeout, readTimeout);
    }

    /** 加载当前接口的目录，查询失败时允许保留远程返回的名称。 */
    public Map<String, BaselineSatelliteInfo> load(CollectInterfaceRuntimeConfig config) {
        if (!Integer.valueOf(1).equals(config.getRpcEnabled())) {
            return Collections.emptyMap();
        }
        try {
            // 第一步：与RPC请求采用相同字段解析顺序，同名配置以后项为准。
            String satelliteCode = "";
            String channel = "";
            for (RpcProtocolField field : parser.parseFields(
                    config.getProtocolConfigParams(), RpcProtocolField.class)) {
                String value = field.getValue() == null ? "" : String.valueOf(field.getValue()).trim();
                if ("sat_code".equals(field.getField())) {
                    satelliteCode = value;
                } else if ("channel".equals(field.getField())) {
                    channel = value;
                }
            }
            if (satelliteCode.isEmpty() || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("基线服务地址或协议卫星编码未配置");
            }
            // 第二步：固定取第一页十条卫星记录，通道不作为查询过滤条件。
            String url = UriComponentsBuilder.fromHttpUrl(
                            baseUrl.trim().replaceAll("/+$", "") + LIST_PATH)
                    .queryParam("pageNum", 1).queryParam("pageSize", 10)
                    .queryParam("satelliteCode", satelliteCode)
                    .build().encode().toUriString();
            JsonNode response = http.get(url, JsonNode.class);
            if (response == null || response.path("code").asInt() != 200
                    || !response.path("data").path("rows").isArray()) {
                throw new IllegalStateException("基线卫星查询失败或返回结构不正确");
            }
            // 第三步：将列表转为只读索引，每颗卫星独立保存自己的通道信息。
            Map<String, BaselineSatelliteInfo> satellites = new LinkedHashMap<>();
            for (JsonNode row : response.path("data").path("rows")) {
                String code = row.path("satelliteCode").asText("").trim();
                if (code.isEmpty()) {
                    continue;
                }
                Map<String, String> channels = new LinkedHashMap<>();
                JsonNode channelList = row.path("channelList");
                if (channelList.isArray()) {
                    for (JsonNode item : channelList) {
                        String channelCode = item.path("channelCode").asText("").trim();
                        if (!channelCode.isEmpty()) {
                            channels.put(channelCode, item.path("channelName").asText(""));
                        }
                    }
                }
                satellites.put(code, new BaselineSatelliteInfo(
                        row.path("satelliteName").asText(""), channels));
            }
            LOGGER.info("基线卫星目录加载完成，接口编号：{}，卫星编码：{}，配置通道：{}，记录数：{}",
                    config.getInterfaceId(), satelliteCode, channel, satellites.size());
            return Collections.unmodifiableMap(satellites);
        } catch (RuntimeException exception) {
            // 目录不可用时继续采集，处理器保留RPC返回值。
            LOGGER.warn("基线卫星目录加载失败，接口编号：{}，原因：{}",
                    config.getInterfaceId(), exception.getMessage());
            return Collections.emptyMap();
        }
    }
}
