package com.example.datasimulator.fep.notify;

import com.example.datasimulator.fep.config.FepSimulatorConfig;
import com.example.datasimulator.fep.config.FepTransferType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 通知数据处理服务启停FEP测试采集流程。 */
public final class FepDataProcessFlowNotifier {

    /** 按模拟源传输方式启动对应的FEP测试采集接口。 */
    public void startCollection(FepTransferType transferType)
            throws IOException {
        // 第一步：TCP和UDP分别使用明确的测试启动地址。
        String requestUrl;
        switch (transferType) {
            case TCP:
                requestUrl = FepSimulatorConfig.TCP_COLLECTION_START_URL;
                break;
            case UDP:
                requestUrl = FepSimulatorConfig.UDP_COLLECTION_START_URL;
                break;
            default:
                throw new IllegalArgumentException("不支持的FEP传输方式");
        }

        // 第二步：通知数据处理服务创建对应的FEP接收任务。
        post(requestUrl, "启动");
    }

    /** 停止当前FEP测试采集接口。 */
    public void stopCollection() throws IOException {
        // 调用FEP专用停止地址释放当前测试接收任务。
        post(FepSimulatorConfig.COLLECTION_STOP_URL, "停止");
    }

    /** 发送不带请求体的请求并校验数据处理服务响应。 */
    private void post(String requestUrl, String operationName)
            throws IOException {
        // 第一步：创建请求连接并设置请求方式和超时时间。
        HttpURLConnection connection = (HttpURLConnection)
                new URL(requestUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(
                FepSimulatorConfig.NOTIFY_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(
                FepSimulatorConfig.NOTIFY_READ_TIMEOUT_MILLIS);
        connection.setDoOutput(false);

        try {
            // 第二步：发出请求并判断响应状态是否成功。
            int responseCode = connection.getResponseCode();
            boolean successful = responseCode >= 200 && responseCode < 300;

            // 第三步：读取响应内容，为失败提示保留服务端信息。
            String responseBody = readResponseBody(
                    successful
                            ? connection.getInputStream()
                            : connection.getErrorStream());
            if (!successful) {
                throw new IOException(
                        "数据处理侧FEP测试采集" + operationName
                                + "失败，响应状态：" + responseCode
                                + "，响应内容：" + responseBody);
            }

            // 第四步：输出成功结果，确认接收任务已经先于文件发送启动。
            System.out.println(
                    "数据处理侧FEP测试采集" + operationName
                            + "成功，响应内容：" + responseBody);
        } finally {
            // 第五步：释放本次通知使用的网络连接。
            connection.disconnect();
        }
    }

    /** 读取数据处理服务返回的文本内容。 */
    private String readResponseBody(InputStream inputStream)
            throws IOException {
        // 第一步：没有响应体时返回空字符串。
        if (inputStream == null) {
            return "";
        }

        // 第二步：分批读取全部响应字节并按UTF-8转换为文本。
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
