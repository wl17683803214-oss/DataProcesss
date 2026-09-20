package tm.processing.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * 遥测处理服务：外部数据源通过双向流持续推送原始遥测帧，
 * 服务端按卫星代号与通道代号定位 TMDLL 解析实例，处理后逐帧返回结果。
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: tm.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class TelemetryProcessingGrpc {

  private TelemetryProcessingGrpc() {}

  public static final java.lang.String SERVICE_NAME = "tm.processing.v1.TelemetryProcessing";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.FrameRequest,
      tm.processing.v1.Tm.FrameResponse> getProcessFramesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ProcessFrames",
      requestType = tm.processing.v1.Tm.FrameRequest.class,
      responseType = tm.processing.v1.Tm.FrameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.FrameRequest,
      tm.processing.v1.Tm.FrameResponse> getProcessFramesMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.FrameRequest, tm.processing.v1.Tm.FrameResponse> getProcessFramesMethod;
    if ((getProcessFramesMethod = TelemetryProcessingGrpc.getProcessFramesMethod) == null) {
      synchronized (TelemetryProcessingGrpc.class) {
        if ((getProcessFramesMethod = TelemetryProcessingGrpc.getProcessFramesMethod) == null) {
          TelemetryProcessingGrpc.getProcessFramesMethod = getProcessFramesMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.FrameRequest, tm.processing.v1.Tm.FrameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ProcessFrames"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.FrameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.FrameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TelemetryProcessingMethodDescriptorSupplier("ProcessFrames"))
              .build();
        }
      }
    }
    return getProcessFramesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TelemetryProcessingStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TelemetryProcessingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TelemetryProcessingStub>() {
        @java.lang.Override
        public TelemetryProcessingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TelemetryProcessingStub(channel, callOptions);
        }
      };
    return TelemetryProcessingStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TelemetryProcessingBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TelemetryProcessingBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TelemetryProcessingBlockingStub>() {
        @java.lang.Override
        public TelemetryProcessingBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TelemetryProcessingBlockingStub(channel, callOptions);
        }
      };
    return TelemetryProcessingBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TelemetryProcessingFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TelemetryProcessingFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TelemetryProcessingFutureStub>() {
        @java.lang.Override
        public TelemetryProcessingFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TelemetryProcessingFutureStub(channel, callOptions);
        }
      };
    return TelemetryProcessingFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * 遥测处理服务：外部数据源通过双向流持续推送原始遥测帧，
   * 服务端按卫星代号与通道代号定位 TMDLL 解析实例，处理后逐帧返回结果。
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<tm.processing.v1.Tm.FrameRequest> processFrames(
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.FrameResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getProcessFramesMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service TelemetryProcessing.
   * <pre>
   * 遥测处理服务：外部数据源通过双向流持续推送原始遥测帧，
   * 服务端按卫星代号与通道代号定位 TMDLL 解析实例，处理后逐帧返回结果。
   * </pre>
   */
  public static abstract class TelemetryProcessingImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return TelemetryProcessingGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service TelemetryProcessing.
   * <pre>
   * 遥测处理服务：外部数据源通过双向流持续推送原始遥测帧，
   * 服务端按卫星代号与通道代号定位 TMDLL 解析实例，处理后逐帧返回结果。
   * </pre>
   */
  public static final class TelemetryProcessingStub
      extends io.grpc.stub.AbstractAsyncStub<TelemetryProcessingStub> {
    private TelemetryProcessingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TelemetryProcessingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TelemetryProcessingStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<tm.processing.v1.Tm.FrameRequest> processFrames(
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.FrameResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getProcessFramesMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service TelemetryProcessing.
   * <pre>
   * 遥测处理服务：外部数据源通过双向流持续推送原始遥测帧，
   * 服务端按卫星代号与通道代号定位 TMDLL 解析实例，处理后逐帧返回结果。
   * </pre>
   */
  public static final class TelemetryProcessingBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<TelemetryProcessingBlockingStub> {
    private TelemetryProcessingBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TelemetryProcessingBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TelemetryProcessingBlockingStub(channel, callOptions);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service TelemetryProcessing.
   * <pre>
   * 遥测处理服务：外部数据源通过双向流持续推送原始遥测帧，
   * 服务端按卫星代号与通道代号定位 TMDLL 解析实例，处理后逐帧返回结果。
   * </pre>
   */
  public static final class TelemetryProcessingFutureStub
      extends io.grpc.stub.AbstractFutureStub<TelemetryProcessingFutureStub> {
    private TelemetryProcessingFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TelemetryProcessingFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TelemetryProcessingFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_PROCESS_FRAMES = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PROCESS_FRAMES:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.processFrames(
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.FrameResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getProcessFramesMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              tm.processing.v1.Tm.FrameRequest,
              tm.processing.v1.Tm.FrameResponse>(
                service, METHODID_PROCESS_FRAMES)))
        .build();
  }

  private static abstract class TelemetryProcessingBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TelemetryProcessingBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tm.processing.v1.Tm.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TelemetryProcessing");
    }
  }

  private static final class TelemetryProcessingFileDescriptorSupplier
      extends TelemetryProcessingBaseDescriptorSupplier {
    TelemetryProcessingFileDescriptorSupplier() {}
  }

  private static final class TelemetryProcessingMethodDescriptorSupplier
      extends TelemetryProcessingBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    TelemetryProcessingMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (TelemetryProcessingGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TelemetryProcessingFileDescriptorSupplier())
              .addMethod(getProcessFramesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
