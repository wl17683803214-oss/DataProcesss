package com.example.dataprocess.rpc;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages;
import com.example.dataprocess.config.DataProcessingRpcProperties;
import com.example.dataprocess.protocol.rpc.TelemetryRpcClient;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tm.processing.v1.Tm;
import tm.processing.v1.TelemetryProcessingGrpc;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 遥测远程客户端双向流调用测试。 */
class TelemetryRpcClientTest {

    /** 测试使用的gRPC服务。 */
    private Server server;
    /** 测试使用的gRPC客户端。 */
    private TelemetryRpcClient client;

    /** 验证单帧请求能够取得FrameResponse遥测消息。 */
    @Test
    void shouldReturnResponseData() throws IOException {
        // 第一步：启动一个收到请求后返回固定数据的本地gRPC服务。
        startServer(new TelemetryProcessingGrpc.TelemetryProcessingImplBase() {
            @Override
            public StreamObserver<Tm.FrameRequest> processFrames(
                    StreamObserver<Tm.FrameResponse> responseObserver) {
                return new StreamObserver<Tm.FrameRequest>() {
                    @Override
                    public void onNext(Tm.FrameRequest request) {
                        // 服务端按单帧返回固定的结构化遥测消息。
                        TelemetryMessages.TelemetryMessage tmMessage =
                                TelemetryMessages.TelemetryMessage.newBuilder()
                                        .setSatCode("74")
                                        .setChannelCode("12")
                                        .build();
                        responseObserver.onNext(Tm.FrameResponse.newBuilder()
                                .setCode(0)
                                .setTmMsg(tmMessage)
                                .build());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        // 测试服务无需补充异常处理。
                    }

                    @Override
                    public void onCompleted() {
                        // 请求流结束后同步结束响应流。
                        responseObserver.onCompleted();
                    }
                };
            }
        });

        // 第二步：发送一帧请求并读取服务端返回的数据域。
        byte[] result = client.process(Tm.FrameRequest.newBuilder()
                .setSequence(1L)
                .build());

        // 第三步：核对客户端返回的是遥测消息的完整序列化结果。
        TelemetryMessages.TelemetryMessage expected =
                TelemetryMessages.TelemetryMessage.newBuilder()
                        .setSatCode("74")
                        .setChannelCode("12")
                        .build();
        assertArrayEquals(expected.toByteArray(), result);
    }

    /** 验证服务端业务失败时客户端不会返回数据。 */
    @Test
    void shouldRejectFailedResponse() throws IOException {
        // 第一步：启动固定返回业务失败的本地gRPC服务。
        startServer(new TelemetryProcessingGrpc.TelemetryProcessingImplBase() {
            @Override
            public StreamObserver<Tm.FrameRequest> processFrames(
                    StreamObserver<Tm.FrameResponse> responseObserver) {
                return new StreamObserver<Tm.FrameRequest>() {
                    @Override
                    public void onNext(Tm.FrameRequest request) {
                        // 返回非零响应码，模拟服务端处理失败。
                        responseObserver.onNext(Tm.FrameResponse.newBuilder()
                                .setCode(12)
                                .setMessage("测试处理失败")
                                .build());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        // 测试服务无需补充异常处理。
                    }

                    @Override
                    public void onCompleted() {
                        // 请求流结束后同步结束响应流。
                        responseObserver.onCompleted();
                    }
                };
            }
        });

        // 第二步：非零响应码必须作为调用失败交给上层处理。
        assertThrows(IllegalStateException.class,
                () -> client.process(Tm.FrameRequest.newBuilder().build()));
    }

    /** 启动测试服务并创建指向随机端口的客户端。 */
    private void startServer(
            TelemetryProcessingGrpc.TelemetryProcessingImplBase service)
            throws IOException {
        // 第一步：使用随机可用端口启动本地服务。
        server = ServerBuilder.forPort(0)
                .addService(service)
                .build()
                .start();

        // 第二步：创建指向测试服务的客户端配置。
        DataProcessingRpcProperties properties =
                new DataProcessingRpcProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(server.getPort());
        properties.setTimeoutMillis(3000L);
        client = new TelemetryRpcClient(properties);
    }

    /** 每个测试结束后释放客户端和服务端资源。 */
    @AfterEach
    void closeResources() {
        // 第一步：关闭测试客户端连接。
        if (client != null) {
            client.close();
        }
        // 第二步：关闭测试服务端。
        if (server != null) {
            server.shutdownNow();
        }
    }
}
