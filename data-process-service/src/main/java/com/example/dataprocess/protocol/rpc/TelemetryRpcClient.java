package com.example.dataprocess.protocol.rpc;

import com.example.dataprocess.config.DataProcessingRpcProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tm.processing.v1.Tm;
import tm.processing.v1.TelemetryProcessingGrpc;

import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 遥测RPC客户端边界。 */
@Component
public class TelemetryRpcClient {

    /** 遥测RPC客户端日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            TelemetryRpcClient.class);
    /** 复用的gRPC连接通道。 */
    private final ManagedChannel channel;
    /** 单帧调用超时时间。 */
    private final long timeoutMillis;

    public TelemetryRpcClient(DataProcessingRpcProperties properties) {
        // 第一步：校验RPC连接参数，避免使用服务端监听地址作为客户端目标。
        validateProperties(properties);
        // 第二步：创建可被所有数据处理线程复用的客户端连接通道。
        this.channel = ManagedChannelBuilder
                .forAddress(properties.getHost(), properties.getPort())
                .usePlaintext()
                .build();
        this.timeoutMillis = properties.getTimeoutMillis();
    }

    /**
     * 调用远程遥测处理服务。
     *
     * @param request 调用前已经组装完成的远程请求
     * @return FrameResponse中的原始数据
     */
    public byte[] process(Tm.FrameRequest request) {
        // 第一步：为当前帧创建独立双向流，避免并发响应无法对应到具体请求。
        CompletableFuture<Tm.FrameResponse> responseFuture =
                new CompletableFuture<>();
        TelemetryProcessingGrpc.TelemetryProcessingStub stub =
                TelemetryProcessingGrpc.newStub(channel)
                        .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);

        // 第二步：注册单帧响应监听器，将异步响应转换为当前处理线程可等待的结果。
        StreamObserver<Tm.FrameResponse> responseObserver =
                createResponseObserver(responseFuture);
        StreamObserver<Tm.FrameRequest> requestObserver =
                stub.processFrames(responseObserver);

        // 第三步：发送一帧请求并结束本次请求流。
        requestObserver.onNext(request);
        requestObserver.onCompleted();

        // 第四步：等待服务端响应，并统一处理超时、调用失败和业务失败。
        Tm.FrameResponse response = awaitResponse(responseFuture);
        if (response.getCode() != 0) {
            throw new IllegalStateException(
                    "遥测RPC处理失败，响应码：" + response.getCode()
                            + "，原因：" + response.getMessage());
        }

        // 第五步：没有遥测消息时返回空数据，保持原处理链路的输出约定。
        if (!response.hasTmMsg()) {
            return new byte[0];
        }
        // 第六步：将结构化遥测消息序列化，保持上层字节接口不变。
        return response.getTmMsg().toByteArray();
    }

    /** 创建当前单帧调用对应的响应监听器。 */
    private StreamObserver<Tm.FrameResponse> createResponseObserver(
            CompletableFuture<Tm.FrameResponse> responseFuture) {
        return new StreamObserver<Tm.FrameResponse>() {
            @Override
            public void onNext(Tm.FrameResponse response) {
                // 单帧调用只接受第一条响应，避免服务端重复响应覆盖结果。
                responseFuture.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                // 将gRPC异步异常交给等待当前帧的处理线程统一处理。
                responseFuture.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                // 服务端未返回数据就结束时，明确标记本次调用失败。
                if (!responseFuture.isDone()) {
                    responseFuture.completeExceptionally(
                            new IllegalStateException("遥测RPC响应为空"));
                }
            }
        };
    }

    /** 等待当前单帧的RPC响应。 */
    private Tm.FrameResponse awaitResponse(
            CompletableFuture<Tm.FrameResponse> responseFuture) {
        try {
            // gRPC截止时间和本地等待时间保持一致。
            return responseFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            // 恢复线程中断标记，交由上层线程池执行退出策略。
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待遥测RPC响应时线程被中断", exception);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("等待遥测RPC响应超时", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    "遥测RPC调用异常", exception.getCause());
        }
    }

    /** 校验RPC客户端配置。 */
    private void validateProperties(DataProcessingRpcProperties properties) {
        String host = properties.getHost();
        if (host == null || host.trim().isEmpty()
                || "0.0.0.0".equals(host.trim())) {
            throw new IllegalArgumentException("必须配置RPC服务实际地址");
        }
        if (properties.getPort() < 1 || properties.getPort() > 65535) {
            throw new IllegalArgumentException("RPC服务端口必须在1到65535之间");
        }
        if (properties.getTimeoutMillis() <= 0) {
            throw new IllegalArgumentException("RPC调用超时时间必须大于0");
        }
    }

    /** 服务关闭时释放gRPC连接通道。 */
    @PreDestroy
    public void close() {
        // 第一步：停止接收新的RPC调用。
        channel.shutdown();
        try {
            // 第二步：等待已提交调用结束，超时后强制关闭连接。
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException exception) {
            // 第三步：中断时立即关闭连接并恢复线程中断标记。
            channel.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.warn("等待遥测RPC连接关闭时线程被中断", exception);
        }
    }
}
