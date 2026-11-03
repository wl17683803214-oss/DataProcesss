package com.example.dataadmin.grpc;

import com.example.dataadmin.config.SatelliteManagementRpcProperties;
import com.example.dataadmin.exception.SatelliteManagementException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;
import tm.processing.v1.SatelliteManagementGrpc;
import tm.processing.v1.Tm;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** 卫星目录管理gRPC客户端边界。 */
@Component
public class SatelliteManagementGrpcClient {

    /** 所有管理请求复用的gRPC连接通道。 */
    private final ManagedChannel channel;
    /** 单次管理调用超时时间。 */
    private final long timeoutMillis;
    /** 实际远程地址，连接失败时随具体原因返回，便于定位配置或网络问题。 */
    private final String remoteAddress;

    public SatelliteManagementGrpcClient(
            SatelliteManagementRpcProperties properties) {
        // 第一步：校验远程服务配置，避免启动后才暴露无效地址。
        validateProperties(properties);
        // 第二步：创建可复用连接并限制返回消息大小。
        this.channel = ManagedChannelBuilder
                .forAddress(properties.getHost(), properties.getPort())
                .usePlaintext()
                .maxInboundMessageSize(properties.getMaxInboundMessageSize())
                .build();
        this.timeoutMillis = properties.getTimeoutMillis();
        this.remoteAddress = properties.getHost() + ":" + properties.getPort();
    }

    /** 上传参数表配置，文件已经保存至本地或双方可读取的共享目录。 */
    public void uploadTable(Tm.UploadTableRequest request) {
        invoke("UploadTable", stub -> stub.uploadTable(request));
    }

    /** 上传完整配置文本，名称和代号来自选中的本地档案。 */
    public void uploadJson(Tm.UploadJsonRequest request) {
        invoke("UploadJson", stub -> stub.uploadJson(request));
    }

    /** 使用新协议修改卫星启用状态。 */
    public void setSatelliteEnabled(Tm.SetSatEnabledRequest request) {
        invoke("SetSatEnabled", stub -> stub.setSatEnabled(request));
    }

    /** 使用统一截止时间执行一次阻塞式gRPC调用。 */
    private void invoke(String methodName, Function<
            SatelliteManagementGrpc.SatelliteManagementBlockingStub,
            Tm.ManagementResponse> invocation) {
        try {
            // 每次调用创建带独立截止时间的轻量存根，连接通道仍然复用。
            SatelliteManagementGrpc.SatelliteManagementBlockingStub stub =
                    SatelliteManagementGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(
                                    timeoutMillis,
                                    TimeUnit.MILLISECONDS);
            // 同一调用入口校验业务结果，使失败提示始终包含具体接口名称。
            requireSuccess(methodName, invocation.apply(stub));
        } catch (StatusRuntimeException exception) {
            // 保留远程状态、服务端说明及底层连接原因，不再统一返回模糊提示。
            throw convertException(methodName, exception);
        }
    }

    /** 校验对方返回的通用管理结果。 */
    private void requireSuccess(String methodName, Tm.ManagementResponse response) {
        // 未返回响应时明确接口和服务地址，而不是将空响应继续当作成功处理。
        if (response == null) {
            throw new SatelliteManagementException(502,
                    "卫星管理接口【" + methodName + "】未返回响应；服务地址：" + remoteAddress);
        }
        if (response.getCode() != 0) {
            // 对方已完成调用但拒绝业务请求时按业务错误返回。
            String message = response.getMessage().isEmpty()
                    ? "远程服务未提供错误说明"
                    : response.getMessage();
            throw new SatelliteManagementException(400, "卫星管理接口【" + methodName
                    + "】处理失败；业务状态码：" + response.getCode() + "；原因：" + message);
        }
    }

    /** 将远程调用状态转换为统一异常。 */
    private SatelliteManagementException convertException(
            String methodName, StatusRuntimeException exception) {
        // 第一步：保留现有接口状态码映射，不改变前端的错误处理约定。
        Status.Code code = exception.getStatus().getCode();
        int responseCode = 502;
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            responseCode = 504;
        } else if (code == Status.Code.INVALID_ARGUMENT) {
            responseCode = 400;
        } else if (code == Status.Code.NOT_FOUND) {
            responseCode = 404;
        }
        // 第二步：说明失败接口、目标地址及真实协议状态，远程说明按原文保留。
        StringBuilder message = new StringBuilder("卫星管理接口【").append(methodName)
                .append("】调用失败；服务地址：").append(remoteAddress)
                .append("；状态：").append(code.name()).append("（").append(statusLabel(code)).append("）");
        String description = exception.getStatus().getDescription();
        if (description != null && !description.trim().isEmpty()) {
            message.append("；远程说明：").append(description.trim());
        }
        // 第三步：连接异常通常嵌套在传输层异常内，提取最深原因而不返回整份堆栈。
        Throwable cause = exception.getCause();
        for (int depth = 0; depth < 16 && cause != null
                && cause.getCause() != null && cause.getCause() != cause; depth++) {
            cause = cause.getCause();
        }
        if (cause != null) {
            String reason = cause.getMessage();
            if (reason != null && !reason.trim().isEmpty() && message.indexOf(reason.trim()) < 0) {
                message.append("；底层原因：").append(reason.trim());
            } else if (reason == null || reason.trim().isEmpty()) {
                message.append("；底层异常类型：").append(cause.getClass().getSimpleName());
            }
        }
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            message.append("；超时配置：").append(timeoutMillis).append("毫秒");
        }
        return new SatelliteManagementException(responseCode, message.toString(), exception);
    }

    /** 复用协议已有状态枚举提供中文含义，同时保留原始状态用于排查。 */
    private String statusLabel(Status.Code code) {
        // 只转换展示文案，不另外定义一套与协议重复的状态枚举。
        switch (code) {
            case UNAVAILABLE:
                return "连接或远程服务异常";
            case DEADLINE_EXCEEDED:
                return "调用超时";
            case INVALID_ARGUMENT:
                return "请求参数不正确";
            case NOT_FOUND:
                return "未找到对应资源";
            case UNIMPLEMENTED:
                return "远程未实现该接口";
            case PERMISSION_DENIED:
                return "远程拒绝访问";
            case UNAUTHENTICATED:
                return "远程身份认证失败";
            case RESOURCE_EXHAUSTED:
                return "远程资源或消息大小超限";
            case CANCELLED:
                return "调用已取消";
            case INTERNAL:
                return "远程内部错误";
            default:
                return "远程调用异常";
        }
    }

    /** 校验客户端连接配置。 */
    private void validateProperties(
            SatelliteManagementRpcProperties properties) {
        if (properties.getHost() == null
                || properties.getHost().trim().isEmpty()
                || "0.0.0.0".equals(properties.getHost().trim())) {
            throw new IllegalArgumentException("必须配置卫星管理服务实际地址");
        }
        if (properties.getPort() < 1 || properties.getPort() > 65535) {
            throw new IllegalArgumentException("卫星管理服务端口必须在1到65535之间");
        }
        if (properties.getTimeoutMillis() <= 0) {
            throw new IllegalArgumentException("卫星管理调用超时时间必须大于0");
        }
        if (properties.getMaxInboundMessageSize() <= 0) {
            throw new IllegalArgumentException("卫星管理最大消息大小必须大于0");
        }
        if (properties.getMaxUploadFileSize() <= 0) {
            throw new IllegalArgumentException("卫星参数表最大文件大小必须大于0");
        }
    }

    /** 服务停止时释放gRPC连接。 */
    @PreDestroy
    public void close() {
        // 第一步：停止接收新的远程调用。
        channel.shutdown();
        try {
            // 第二步：等待执行中的调用结束，超时后强制关闭。
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException exception) {
            // 第三步：恢复线程中断标记并立即关闭连接。
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
