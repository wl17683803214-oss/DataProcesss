package com.example.common.tool;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 通用 HTTP 请求工具测试。
 */
class HttpRequestToolTest {

    /**
     * 验证 GET 请求可以读取文本响应。
     */
    @Test
    void shouldSendGetRequest() {
        // 创建可以检查请求内容的模拟客户端。
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer =
                MockRestServiceServer.createServer(restTemplate);
        HttpRequestTool requestTool = new HttpRequestTool(restTemplate);

        // 约定 GET 请求地址和返回内容。
        MediaType utf8TextMediaType = new MediaType(
                MediaType.TEXT_PLAIN,
                StandardCharsets.UTF_8);
        mockServer.expect(requestTo("http://127.0.0.1:8080/status?id=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("成功", utf8TextMediaType));

        // 发送请求并读取文本响应。
        String result = requestTool.get(
                "http://127.0.0.1:8080/status?id=1",
                String.class);

        // 核对响应内容并确认请求执行完成。
        assertEquals("成功", result);
        mockServer.verify();
    }

    /**
     * 验证二进制请求和响应不会发生编码转换。
     */
    @Test
    void shouldSendBinaryRequest() {
        // 创建可以检查请求内容的模拟客户端。
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer =
                MockRestServiceServer.createServer(restTemplate);
        HttpRequestTool requestTool = new HttpRequestTool(restTemplate);

        // 准备原始请求和响应字节。
        byte[] requestBody = new byte[]{
                (byte) 0xEB,
                (byte) 0x90,
                0x01};
        byte[] responseBody = new byte[]{0x11, 0x22};

        // 约定二进制请求内容和返回内容。
        mockServer.expect(requestTo("http://127.0.0.1:8080/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(requestBody))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_OCTET_STREAM));

        // 发送原始字节并读取原始响应字节。
        byte[] result = requestTool.postBytes(
                "http://127.0.0.1:8080/process",
                requestBody);

        // 核对响应内容并确认请求执行完成。
        assertArrayEquals(responseBody, result);
        mockServer.verify();
    }

    /**
     * 验证自定义请求可以携带请求头和指定请求方式。
     */
    @Test
    void shouldSendCustomRequest() {
        // 创建可以检查请求内容的模拟客户端。
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer =
                MockRestServiceServer.createServer(restTemplate);
        HttpRequestTool requestTool = new HttpRequestTool(restTemplate);

        // 准备调用方自定义的请求头。
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Command", "700");
        headers.setContentType(new MediaType(
                MediaType.TEXT_PLAIN,
                StandardCharsets.UTF_8));

        // 约定请求方式、请求头和响应内容。
        mockServer.expect(requestTo("http://127.0.0.1:8080/custom"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Command", "700"))
                .andExpect(content().string("测试数据"))
                .andRespond(withSuccess(
                        "已接收",
                        new MediaType(
                                MediaType.TEXT_PLAIN,
                                StandardCharsets.UTF_8)));

        // 使用统一入口发送完全自定义的请求。
        String result = requestTool.request(
                "http://127.0.0.1:8080/custom",
                HttpMethod.PUT,
                headers,
                "测试数据",
                String.class).getBody();

        // 核对响应内容并确认请求执行完成。
        assertEquals("已接收", result);
        mockServer.verify();
    }
}
