package com.example.dataadmin.grpc;

import com.example.dataadmin.config.SatelliteManagementRpcProperties;
import com.example.dataadmin.exception.SatelliteManagementException;
import com.google.protobuf.Empty;
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
    }

    /** 查询全部卫星配置。 */
    public Tm.ListSatellitesResponse listSatellites() {
        return invoke(stub -> stub.listSatellites(
                Empty.getDefaultInstance()));
    }

    /** 按卫星代号查询完整配置。 */
    public Tm.GetSatelliteResponse getSatellite(
            Tm.GetSatelliteRequest request) {
        return invoke(stub -> stub.getSatellite(request));
    }

    /** 创建卫星。 */
    public void createSatellite(Tm.CreateSatelliteRequest request) {
        requireSuccess(invoke(stub -> stub.createSatellite(request)));
    }

    /** 整体更新卫星。 */
    public void updateSatellite(Tm.UpdateSatelliteRequest request) {
        requireSuccess(invoke(stub -> stub.updateSatellite(request)));
    }

    /** 删除卫星。 */
    public void deleteSatellite(Tm.DeleteSatelliteRequest request) {
        requireSuccess(invoke(stub -> stub.deleteSatellite(request)));
    }

    /** 修改卫星启用状态。 */
    public void setSatelliteEnabled(Tm.SetEnabledRequest request) {
        requireSuccess(invoke(stub -> stub.setSatelliteEnabled(request)));
    }

    /** 修改遥测通道启用状态。 */
    public void setChannelEnabled(Tm.SetChannelEnabledRequest request) {
        requireSuccess(invoke(stub -> stub.setChannelEnabled(request)));
    }

    /** 修改遥测通道解扰状态。 */
    public void setChannelDescramble(
            Tm.SetChannelDescrambleRequest request) {
        requireSuccess(invoke(stub -> stub.setChannelDescramble(request)));
    }

    /** 上传卫星参数表。 */
    public void uploadTable(Tm.UploadTableRequest request) {
        requireSuccess(invoke(stub -> stub.uploadTable(request)));
    }

    /** 保存卫星解扰规则。 */
    public void setDescrambleRule(Tm.SetDescrambleRuleRequest request) {
        requireSuccess(invoke(stub -> stub.setDescrambleRule(request)));
    }

    /** 添加卫星遥测通道。 */
    public void addTmChannels(Tm.AddTmChannelsRequest request) {
        requireSuccess(invoke(stub -> stub.addTmChannels(request)));
    }

    /** 使用统一截止时间执行一次阻塞式gRPC调用。 */
    private <T> T invoke(Function<
            SatelliteManagementGrpc.SatelliteManagementBlockingStub,
            T> invocation) {
        try {
            // 每次调用创建带独立截止时间的轻量存根，连接通道仍然复用。
            SatelliteManagementGrpc.SatelliteManagementBlockingStub stub =
                    SatelliteManagementGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(
                                    timeoutMillis,
                                    TimeUnit.MILLISECONDS);
            return invocation.apply(stub);
        } catch (StatusRuntimeException exception) {
            // 将gRPC状态转换为前端可理解的中文错误。
            throw convertException(exception);
        }
    }

    /** 校验对方返回的通用管理结果。 */
    private void requireSuccess(Tm.ManagementResponse response) {
        if (response.getCode() != 0) {
            // 对方已完成调用但拒绝业务请求时按业务错误返回。
            String message = response.getMessage().isEmpty()
                    ? "卫星目录管理操作失败"
                    : response.getMessage();
            throw new SatelliteManagementException(400, message);
        }
    }

    /** 将远程调用状态转换为统一异常。 */
    private SatelliteManagementException convertException(
            StatusRuntimeException exception) {
        Status.Code code = exception.getStatus().getCode();
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            return new SatelliteManagementException(
                    504,
                    "卫星管理服务响应超时",
                    exception);
        }
        if (code == Status.Code.INVALID_ARGUMENT) {
            return new SatelliteManagementException(
                    400,
                    "卫星管理请求参数不正确",
                    exception);
        }
        if (code == Status.Code.NOT_FOUND) {
            return new SatelliteManagementException(
                    404,
                    "未找到指定的卫星配置",
                    exception);
        }
        // 其他连接或服务状态统一按网关错误处理，避免暴露底层英文信息。
        return new SatelliteManagementException(
                502,
                "卫星管理服务暂时不可用",
                exception);
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
