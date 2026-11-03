package tm.processing.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: tm.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SatelliteManagementGrpc {

  private SatelliteManagementGrpc() {}

  public static final String SERVICE_NAME = "tm.processing.v1.SatelliteManagement";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Tm.UploadTableRequest,
      Tm.ManagementResponse> getUploadTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadTable",
      requestType = Tm.UploadTableRequest.class,
      responseType = Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Tm.UploadTableRequest,
      Tm.ManagementResponse> getUploadTableMethod() {
    io.grpc.MethodDescriptor<Tm.UploadTableRequest, Tm.ManagementResponse> getUploadTableMethod;
    if ((getUploadTableMethod = SatelliteManagementGrpc.getUploadTableMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getUploadTableMethod = SatelliteManagementGrpc.getUploadTableMethod) == null) {
          SatelliteManagementGrpc.getUploadTableMethod = getUploadTableMethod =
              io.grpc.MethodDescriptor.<Tm.UploadTableRequest, Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Tm.UploadTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("UploadTable"))
              .build();
        }
      }
    }
    return getUploadTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Tm.UploadJsonRequest,
      Tm.ManagementResponse> getUploadJsonMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadJson",
      requestType = Tm.UploadJsonRequest.class,
      responseType = Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Tm.UploadJsonRequest,
      Tm.ManagementResponse> getUploadJsonMethod() {
    io.grpc.MethodDescriptor<Tm.UploadJsonRequest, Tm.ManagementResponse> getUploadJsonMethod;
    if ((getUploadJsonMethod = SatelliteManagementGrpc.getUploadJsonMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getUploadJsonMethod = SatelliteManagementGrpc.getUploadJsonMethod) == null) {
          SatelliteManagementGrpc.getUploadJsonMethod = getUploadJsonMethod =
              io.grpc.MethodDescriptor.<Tm.UploadJsonRequest, Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadJson"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Tm.UploadJsonRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("UploadJson"))
              .build();
        }
      }
    }
    return getUploadJsonMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Tm.SetSatEnabledRequest,
      Tm.ManagementResponse> getSetSatEnabledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetSatEnabled",
      requestType = Tm.SetSatEnabledRequest.class,
      responseType = Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Tm.SetSatEnabledRequest,
      Tm.ManagementResponse> getSetSatEnabledMethod() {
    io.grpc.MethodDescriptor<Tm.SetSatEnabledRequest, Tm.ManagementResponse> getSetSatEnabledMethod;
    if ((getSetSatEnabledMethod = SatelliteManagementGrpc.getSetSatEnabledMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getSetSatEnabledMethod = SatelliteManagementGrpc.getSetSatEnabledMethod) == null) {
          SatelliteManagementGrpc.getSetSatEnabledMethod = getSetSatEnabledMethod =
              io.grpc.MethodDescriptor.<Tm.SetSatEnabledRequest, Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetSatEnabled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Tm.SetSatEnabledRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("SetSatEnabled"))
              .build();
        }
      }
    }
    return getSetSatEnabledMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SatelliteManagementStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementStub>() {
        @Override
        public SatelliteManagementStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SatelliteManagementStub(channel, callOptions);
        }
      };
    return SatelliteManagementStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SatelliteManagementBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementBlockingStub>() {
        @Override
        public SatelliteManagementBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SatelliteManagementBlockingStub(channel, callOptions);
        }
      };
    return SatelliteManagementBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SatelliteManagementFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementFutureStub>() {
        @Override
        public SatelliteManagementFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SatelliteManagementFutureStub(channel, callOptions);
        }
      };
    return SatelliteManagementFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void uploadTable(Tm.UploadTableRequest request,
                             io.grpc.stub.StreamObserver<Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUploadTableMethod(), responseObserver);
    }

    /**
     */
    default void uploadJson(Tm.UploadJsonRequest request,
                            io.grpc.stub.StreamObserver<Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUploadJsonMethod(), responseObserver);
    }

    /**
     */
    default void setSatEnabled(Tm.SetSatEnabledRequest request,
                               io.grpc.stub.StreamObserver<Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetSatEnabledMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service SatelliteManagement.
   */
  public static abstract class SatelliteManagementImplBase
      implements io.grpc.BindableService, AsyncService {

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return SatelliteManagementGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service SatelliteManagement.
   */
  public static final class SatelliteManagementStub
      extends io.grpc.stub.AbstractAsyncStub<SatelliteManagementStub> {
    private SatelliteManagementStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SatelliteManagementStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SatelliteManagementStub(channel, callOptions);
    }

    /**
     */
    public void uploadTable(Tm.UploadTableRequest request,
                            io.grpc.stub.StreamObserver<Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUploadTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void uploadJson(Tm.UploadJsonRequest request,
                           io.grpc.stub.StreamObserver<Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUploadJsonMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setSatEnabled(Tm.SetSatEnabledRequest request,
                              io.grpc.stub.StreamObserver<Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetSatEnabledMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service SatelliteManagement.
   */
  public static final class SatelliteManagementBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SatelliteManagementBlockingStub> {
    private SatelliteManagementBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SatelliteManagementBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SatelliteManagementBlockingStub(channel, callOptions);
    }

    /**
     */
    public Tm.ManagementResponse uploadTable(Tm.UploadTableRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUploadTableMethod(), getCallOptions(), request);
    }

    /**
     */
    public Tm.ManagementResponse uploadJson(Tm.UploadJsonRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUploadJsonMethod(), getCallOptions(), request);
    }

    /**
     */
    public Tm.ManagementResponse setSatEnabled(Tm.SetSatEnabledRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetSatEnabledMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service SatelliteManagement.
   */
  public static final class SatelliteManagementFutureStub
      extends io.grpc.stub.AbstractFutureStub<SatelliteManagementFutureStub> {
    private SatelliteManagementFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected SatelliteManagementFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SatelliteManagementFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<Tm.ManagementResponse> uploadTable(
        Tm.UploadTableRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUploadTableMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<Tm.ManagementResponse> uploadJson(
        Tm.UploadJsonRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUploadJsonMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<Tm.ManagementResponse> setSatEnabled(
        Tm.SetSatEnabledRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetSatEnabledMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_UPLOAD_TABLE = 0;
  private static final int METHODID_UPLOAD_JSON = 1;
  private static final int METHODID_SET_SAT_ENABLED = 2;

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

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UPLOAD_TABLE:
          serviceImpl.uploadTable((Tm.UploadTableRequest) request,
              (io.grpc.stub.StreamObserver<Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_UPLOAD_JSON:
          serviceImpl.uploadJson((Tm.UploadJsonRequest) request,
              (io.grpc.stub.StreamObserver<Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_SET_SAT_ENABLED:
          serviceImpl.setSatEnabled((Tm.SetSatEnabledRequest) request,
              (io.grpc.stub.StreamObserver<Tm.ManagementResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getUploadTableMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              Tm.UploadTableRequest,
              Tm.ManagementResponse>(
                service, METHODID_UPLOAD_TABLE)))
        .addMethod(
          getUploadJsonMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              Tm.UploadJsonRequest,
              Tm.ManagementResponse>(
                service, METHODID_UPLOAD_JSON)))
        .addMethod(
          getSetSatEnabledMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              Tm.SetSatEnabledRequest,
              Tm.ManagementResponse>(
                service, METHODID_SET_SAT_ENABLED)))
        .build();
  }

  private static abstract class SatelliteManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SatelliteManagementBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return Tm.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SatelliteManagement");
    }
  }

  private static final class SatelliteManagementFileDescriptorSupplier
      extends SatelliteManagementBaseDescriptorSupplier {
    SatelliteManagementFileDescriptorSupplier() {}
  }

  private static final class SatelliteManagementMethodDescriptorSupplier
      extends SatelliteManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SatelliteManagementMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (SatelliteManagementGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SatelliteManagementFileDescriptorSupplier())
              .addMethod(getUploadTableMethod())
              .addMethod(getUploadJsonMethod())
              .addMethod(getSetSatEnabledMethod())
              .build();
        }
      }
    }
    return result;
  }
}
