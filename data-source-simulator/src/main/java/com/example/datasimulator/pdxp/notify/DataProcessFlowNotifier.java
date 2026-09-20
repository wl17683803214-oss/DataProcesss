package com.example.datasimulator.pdxp.notify;

import com.example.datasimulator.pdxp.config.PdxpSimulatorConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 通知数据处理服务启停测试采集流程的工具。 */
public final class DataProcessFlowNotifier {

    /** 通知数据处理服务启动测试采集接口。 */
    public void startCollection() throws IOException {
        // 调用数据处理服务已有的测试采集启动接口。
        post(PdxpSimulatorConfig.COLLECTION_START_URL, "启动");
    }

    /** 通知数据处理服务停止测试采集接口。 */
    public void stopCollection() throws IOException {
        // 调用数据处理服务已有的测试采集停止接口。
        post(PdxpSimulatorConfig.COLLECTION_STOP_URL, "停止");
    }

    /** 发送不带请求体的POST通知，并校验服务端响应。 */
    private void post(String requestUrl, String operationName) throws IOException {
        // 第一步：创建HTTP连接并设置请求方式和超时时间。
        HttpURLConnection connection = (HttpURLConnection)
                new URL(requestUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(
                PdxpSimulatorConfig.NOTIFY_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(
                PdxpSimulatorConfig.NOTIFY_READ_TIMEOUT_MILLIS);
        connection.setDoOutput(false);

        try {
            // 第二步：发送请求并取得HTTP响应状态。
            int responseCode = connection.getResponseCode();
            boolean successful = responseCode >= 200 && responseCode < 300;

            // 第三步：读取服务端响应，失败时用于说明具体原因。
            String responseBody = readResponseBody(
                    successful
                            ? connection.getInputStream()
                            : connection.getErrorStream());
            if (!successful) {
                throw new IOException(
                        "数据处理侧测试采集" + operationName
                                + "失败，响应状态：" + responseCode
                                + "，响应内容：" + responseBody);
            }

            // 第四步：输出成功信息，确认模拟源发送前采集流程已经就绪。
            System.out.println(
                    "数据处理侧测试采集" + operationName
                            + "成功，响应内容：" + responseBody);
        } finally {
            // 第五步：释放本次HTTP连接资源。
            connection.disconnect();
        }
    }

    /** 读取服务端响应文本。 */
    private String readResponseBody(InputStream inputStream) throws IOException {
        // 没有响应体时返回空字符串，不影响HTTP状态判断。
        if (inputStream == null) {
            return "";
        }

        // 分批读取响应内容，避免假设单次读取可以取得全部字节。
        try (InputStream responseStream = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1_024];
            int readCount;
            while ((readCount = responseStream.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
