package com.example.common.tool;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 通用 HTTP 请求工具。
 *
 * <p>支持常用请求方式、自定义请求头以及 JSON、文本和二进制数据。</p>
 */
public final class HttpRequestTool {

    /** 默认连接超时时间，单位毫秒。 */
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;

    /** 默认读取超时时间，单位毫秒。 */
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;

    /** 执行 HTTP 请求的客户端。 */
    private final RestTemplate restTemplate;

    /**
     * 使用默认超时时间创建工具。
     */
    public HttpRequestTool() {
        // 使用默认连接和读取超时时间初始化工具。
        this(DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS);
    }

    /**
     * 使用指定超时时间创建工具。
     *
     * @param connectTimeoutMillis 连接超时时间，单位毫秒
     * @param readTimeoutMillis 读取响应超时时间，单位毫秒
     */
    public HttpRequestTool(int connectTimeoutMillis, int readTimeoutMillis) {
        // 超时时间不能为负数，零表示不限制等待时间。
        if (connectTimeoutMillis < 0 || readTimeoutMillis < 0) {
            throw new IllegalArgumentException("HTTP 超时时间不能小于零");
        }

        // 配置底层连接和响应读取超时时间。
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);

        // 创建可在多个请求之间重复使用的客户端。
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * 使用已有客户端创建工具。
     *
     * @param restTemplate 已配置的 HTTP 客户端
     */
    public HttpRequestTool(RestTemplate restTemplate) {
        // 已有客户端不能为空。
        if (restTemplate == null) {
            throw new IllegalArgumentException("HTTP 客户端不能为空");
        }

        // 保存外部提供的客户端及其统一配置。
        this.restTemplate = restTemplate;
    }

    /**
     * 发送 GET 请求。
     *
     * @param url 请求地址，可直接包含查询参数
     * @param responseType 响应数据类型
     * @param <T> 响应数据类型
     * @return 响应数据
     */
    public <T> T get(String url, Class<T> responseType) {
        // GET 请求没有请求体，使用空请求头发送。
        return request(
                url,
                HttpMethod.GET,
                new HttpHeaders(),
                null,
                responseType).getBody();
    }

    /**
     * 发送 JSON 格式 POST 请求。
     *
     * @param url 请求地址
     * @param requestBody 请求对象
     * @param responseType 响应数据类型
     * @param <T> 响应数据类型
     * @return 响应数据
     */
    public <T> T postJson(
            String url,
            Object requestBody,
            Class<T> responseType) {

        // 声明请求体和期望响应使用 JSON 格式。
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 使用统一请求入口发送 JSON 数据。
        return request(
                url,
                HttpMethod.POST,
                headers,
                requestBody,
                responseType).getBody();
    }

    /**
     * 发送二进制格式 POST 请求。
     *
     * @param url 请求地址
     * @param requestBody 原始二进制请求体
     * @return 原始二进制响应体
     */
    public byte[] postBytes(String url, byte[] requestBody) {
        // 声明请求体和期望响应使用二进制格式。
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setAccept(Collections.singletonList(
                MediaType.APPLICATION_OCTET_STREAM));

        // 使用统一请求入口发送原始字节。
        byte[] responseBody = request(
                url,
                HttpMethod.POST,
                headers,
                requestBody,
                byte[].class).getBody();

        // 空响应体统一转换为空数组，便于调用方直接处理。
        return responseBody == null ? new byte[0] : responseBody;
    }

    /**
     * 发送文本格式 POST 请求。
     *
     * @param url 请求地址
     * @param requestBody 文本请求体
     * @return 文本响应体
     */
    public String postText(String url, String requestBody) {
        // 声明请求体和期望响应使用纯文本格式。
        HttpHeaders headers = new HttpHeaders();
        MediaType utf8TextMediaType = new MediaType(
                MediaType.TEXT_PLAIN,
                StandardCharsets.UTF_8);
        headers.setContentType(utf8TextMediaType);
        headers.setAccept(Collections.singletonList(utf8TextMediaType));

        // 使用统一请求入口发送文本内容。
        return request(
                url,
                HttpMethod.POST,
                headers,
                requestBody,
                String.class).getBody();
    }

    /**
     * 发送完全自定义的 HTTP 请求。
     *
     * @param url 请求地址
     * @param method 请求方式
     * @param headers 请求头，为空时使用空请求头
     * @param requestBody 请求体，可以为空
     * @param responseType 响应数据类型
     * @param <T> 响应数据类型
     * @return 包含状态码、响应头和响应体的完整响应
     */
    public <T> ResponseEntity<T> request(
            String url,
            HttpMethod method,
            HttpHeaders headers,
            Object requestBody,
            Class<T> responseType) {

        // 校验并转换请求地址。
        URI requestUri = validateAndCreateUri(url);

        // 请求方式和响应类型是执行请求的必需参数。
        if (method == null) {
            throw new IllegalArgumentException("HTTP 请求方式不能为空");
        }
        if (responseType == null) {
            throw new IllegalArgumentException("HTTP 响应类型不能为空");
        }

        // 请求头为空时创建空请求头，避免调用方额外判断。
        HttpHeaders actualHeaders = headers == null
                ? new HttpHeaders()
                : headers;

        // 将请求头和请求体组合为完整请求实体。
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(
                requestBody,
                actualHeaders);

        try {
            // 发起请求并保留状态码、响应头和响应体。
            return restTemplate.exchange(
                    requestUri,
                    method,
                    requestEntity,
                    responseType);
        } catch (RestClientException exception) {
            // 将底层异常转换为包含请求地址的中文异常。
            throw new IllegalStateException(
                    "HTTP 请求失败，接口地址：" + requestUri,
                    exception);
        }
    }

    /**
     * 校验并创建 HTTP 请求地址。
     */
    private static URI validateAndCreateUri(String url) {
        // 空地址无法发起请求。
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP 请求地址不能为空");
        }

        // 将文本地址转换为统一的资源地址对象。
        URI requestUri;
        try {
            requestUri = URI.create(url.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("HTTP 请求地址格式不正确：" + url, exception);
        }

        // 当前工具只允许通过 HTTP 或 HTTPS 发起请求。
        String scheme = requestUri.getScheme();
        if (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("请求地址必须使用 HTTP 或 HTTPS");
        }
        return requestUri;
    }
}
