package com.example.dataprocess.controller;

import com.example.common.response.ApiResponse;
import com.example.dataprocess.service.InterfaceStatisticsWriter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/** 统一接收HTTP采集请求，并按接收路径更新累计采集量。 */
@RestController
@RequestMapping("/collection/receive")
public class HttpCollectionController {

    private final InterfaceStatisticsWriter statisticsWriter;
    private final ObjectReader jsonReader;

    public HttpCollectionController(InterfaceStatisticsWriter statisticsWriter, ObjectMapper objectMapper) {
        this.statisticsWriter = statisticsWriter;
        // 完整读取单个JSON值，避免请求体尾部存在无效内容时仍计数。
        this.jsonReader = objectMapper.readerFor(JsonNode.class)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /** 接收统一前缀下任意路径的JSON，不要求为每条采集配置新增接口方法。 */
    @PostMapping(value = "/**", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> receive(@RequestBody byte[] content, HttpServletRequest request) {
        // 第一步：校验整个请求体，失败时沿用原有的参数错误响应。
        try {
            jsonReader.readValue(content);
        } catch (IOException exception) {
            throw new HttpMessageNotReadableException(
                    "请求体必须是非空的合法JSON", exception, new ServletServerHttpRequest(request));
        }
        // 第二步：按应用内路径匹配，忽略部署前缀和查询参数。
        String receivePath = request.getRequestURI().substring(request.getContextPath().length());
        statisticsWriter.recordHttpCollection(receivePath);
        // 第三步：已匹配的计数提交后再应答，未匹配的路径沿用成功应答。
        return ApiResponse.success();
    }

    /** 请求体缺失或JSON无法解析时返回中文参数错误，不返回成功应答。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(
            HttpMessageNotReadableException exception) {
        // 使用已有统一响应结构，明确区分请求格式错误与成功接收。
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>fail(400, "请求体必须是非空的合法JSON"));
    }
}
