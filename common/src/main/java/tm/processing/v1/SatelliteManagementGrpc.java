package tm.processing.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * 卫星目录管理服务：供前端创建、编辑、启停卫星，并配置参数表、解扰规则与通道。
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: tm.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SatelliteManagementGrpc {

  private SatelliteManagementGrpc() {}

  public static final java.lang.String SERVICE_NAME = "tm.processing.v1.SatelliteManagement";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      tm.processing.v1.Tm.ListSatellitesResponse> getListSatellitesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListSatellites",
      requestType = com.google.protobuf.Empty.class,
      responseType = tm.processing.v1.Tm.ListSatellitesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      tm.processing.v1.Tm.ListSatellitesResponse> getListSatellitesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, tm.processing.v1.Tm.ListSatellitesResponse> getListSatellitesMethod;
    if ((getListSatellitesMethod = SatelliteManagementGrpc.getListSatellitesMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getListSatellitesMethod = SatelliteManagementGrpc.getListSatellitesMethod) == null) {
          SatelliteManagementGrpc.getListSatellitesMethod = getListSatellitesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, tm.processing.v1.Tm.ListSatellitesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListSatellites"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ListSatellitesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("ListSatellites"))
              .build();
        }
      }
    }
    return getListSatellitesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.GetSatelliteRequest,
      tm.processing.v1.Tm.GetSatelliteResponse> getGetSatelliteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSatellite",
      requestType = tm.processing.v1.Tm.GetSatelliteRequest.class,
      responseType = tm.processing.v1.Tm.GetSatelliteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.GetSatelliteRequest,
      tm.processing.v1.Tm.GetSatelliteResponse> getGetSatelliteMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.GetSatelliteRequest, tm.processing.v1.Tm.GetSatelliteResponse> getGetSatelliteMethod;
    if ((getGetSatelliteMethod = SatelliteManagementGrpc.getGetSatelliteMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getGetSatelliteMethod = SatelliteManagementGrpc.getGetSatelliteMethod) == null) {
          SatelliteManagementGrpc.getGetSatelliteMethod = getGetSatelliteMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.GetSatelliteRequest, tm.processing.v1.Tm.GetSatelliteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSatellite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.GetSatelliteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.GetSatelliteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("GetSatellite"))
              .build();
        }
      }
    }
    return getGetSatelliteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.CreateSatelliteRequest,
      tm.processing.v1.Tm.ManagementResponse> getCreateSatelliteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateSatellite",
      requestType = tm.processing.v1.Tm.CreateSatelliteRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.CreateSatelliteRequest,
      tm.processing.v1.Tm.ManagementResponse> getCreateSatelliteMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.CreateSatelliteRequest, tm.processing.v1.Tm.ManagementResponse> getCreateSatelliteMethod;
    if ((getCreateSatelliteMethod = SatelliteManagementGrpc.getCreateSatelliteMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getCreateSatelliteMethod = SatelliteManagementGrpc.getCreateSatelliteMethod) == null) {
          SatelliteManagementGrpc.getCreateSatelliteMethod = getCreateSatelliteMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.CreateSatelliteRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateSatellite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.CreateSatelliteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("CreateSatellite"))
              .build();
        }
      }
    }
    return getCreateSatelliteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.UpdateSatelliteRequest,
      tm.processing.v1.Tm.ManagementResponse> getUpdateSatelliteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateSatellite",
      requestType = tm.processing.v1.Tm.UpdateSatelliteRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.UpdateSatelliteRequest,
      tm.processing.v1.Tm.ManagementResponse> getUpdateSatelliteMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.UpdateSatelliteRequest, tm.processing.v1.Tm.ManagementResponse> getUpdateSatelliteMethod;
    if ((getUpdateSatelliteMethod = SatelliteManagementGrpc.getUpdateSatelliteMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getUpdateSatelliteMethod = SatelliteManagementGrpc.getUpdateSatelliteMethod) == null) {
          SatelliteManagementGrpc.getUpdateSatelliteMethod = getUpdateSatelliteMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.UpdateSatelliteRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateSatellite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.UpdateSatelliteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("UpdateSatellite"))
              .build();
        }
      }
    }
    return getUpdateSatelliteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.DeleteSatelliteRequest,
      tm.processing.v1.Tm.ManagementResponse> getDeleteSatelliteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteSatellite",
      requestType = tm.processing.v1.Tm.DeleteSatelliteRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.DeleteSatelliteRequest,
      tm.processing.v1.Tm.ManagementResponse> getDeleteSatelliteMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.DeleteSatelliteRequest, tm.processing.v1.Tm.ManagementResponse> getDeleteSatelliteMethod;
    if ((getDeleteSatelliteMethod = SatelliteManagementGrpc.getDeleteSatelliteMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getDeleteSatelliteMethod = SatelliteManagementGrpc.getDeleteSatelliteMethod) == null) {
          SatelliteManagementGrpc.getDeleteSatelliteMethod = getDeleteSatelliteMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.DeleteSatelliteRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteSatellite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.DeleteSatelliteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("DeleteSatellite"))
              .build();
        }
      }
    }
    return getDeleteSatelliteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetEnabledRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetSatelliteEnabledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetSatelliteEnabled",
      requestType = tm.processing.v1.Tm.SetEnabledRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetEnabledRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetSatelliteEnabledMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetEnabledRequest, tm.processing.v1.Tm.ManagementResponse> getSetSatelliteEnabledMethod;
    if ((getSetSatelliteEnabledMethod = SatelliteManagementGrpc.getSetSatelliteEnabledMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getSetSatelliteEnabledMethod = SatelliteManagementGrpc.getSetSatelliteEnabledMethod) == null) {
          SatelliteManagementGrpc.getSetSatelliteEnabledMethod = getSetSatelliteEnabledMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.SetEnabledRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetSatelliteEnabled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.SetEnabledRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("SetSatelliteEnabled"))
              .build();
        }
      }
    }
    return getSetSatelliteEnabledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetChannelEnabledRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetChannelEnabledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetChannelEnabled",
      requestType = tm.processing.v1.Tm.SetChannelEnabledRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetChannelEnabledRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetChannelEnabledMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetChannelEnabledRequest, tm.processing.v1.Tm.ManagementResponse> getSetChannelEnabledMethod;
    if ((getSetChannelEnabledMethod = SatelliteManagementGrpc.getSetChannelEnabledMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getSetChannelEnabledMethod = SatelliteManagementGrpc.getSetChannelEnabledMethod) == null) {
          SatelliteManagementGrpc.getSetChannelEnabledMethod = getSetChannelEnabledMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.SetChannelEnabledRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetChannelEnabled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.SetChannelEnabledRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("SetChannelEnabled"))
              .build();
        }
      }
    }
    return getSetChannelEnabledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetChannelDescrambleRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetChannelDescrambleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetChannelDescramble",
      requestType = tm.processing.v1.Tm.SetChannelDescrambleRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetChannelDescrambleRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetChannelDescrambleMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetChannelDescrambleRequest, tm.processing.v1.Tm.ManagementResponse> getSetChannelDescrambleMethod;
    if ((getSetChannelDescrambleMethod = SatelliteManagementGrpc.getSetChannelDescrambleMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getSetChannelDescrambleMethod = SatelliteManagementGrpc.getSetChannelDescrambleMethod) == null) {
          SatelliteManagementGrpc.getSetChannelDescrambleMethod = getSetChannelDescrambleMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.SetChannelDescrambleRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetChannelDescramble"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.SetChannelDescrambleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("SetChannelDescramble"))
              .build();
        }
      }
    }
    return getSetChannelDescrambleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.UploadTableRequest,
      tm.processing.v1.Tm.ManagementResponse> getUploadTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadTable",
      requestType = tm.processing.v1.Tm.UploadTableRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.UploadTableRequest,
      tm.processing.v1.Tm.ManagementResponse> getUploadTableMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.UploadTableRequest, tm.processing.v1.Tm.ManagementResponse> getUploadTableMethod;
    if ((getUploadTableMethod = SatelliteManagementGrpc.getUploadTableMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getUploadTableMethod = SatelliteManagementGrpc.getUploadTableMethod) == null) {
          SatelliteManagementGrpc.getUploadTableMethod = getUploadTableMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.UploadTableRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.UploadTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("UploadTable"))
              .build();
        }
      }
    }
    return getUploadTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetDescrambleRuleRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetDescrambleRuleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetDescrambleRule",
      requestType = tm.processing.v1.Tm.SetDescrambleRuleRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetDescrambleRuleRequest,
      tm.processing.v1.Tm.ManagementResponse> getSetDescrambleRuleMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.SetDescrambleRuleRequest, tm.processing.v1.Tm.ManagementResponse> getSetDescrambleRuleMethod;
    if ((getSetDescrambleRuleMethod = SatelliteManagementGrpc.getSetDescrambleRuleMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getSetDescrambleRuleMethod = SatelliteManagementGrpc.getSetDescrambleRuleMethod) == null) {
          SatelliteManagementGrpc.getSetDescrambleRuleMethod = getSetDescrambleRuleMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.SetDescrambleRuleRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetDescrambleRule"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.SetDescrambleRuleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("SetDescrambleRule"))
              .build();
        }
      }
    }
    return getSetDescrambleRuleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tm.processing.v1.Tm.AddTmChannelsRequest,
      tm.processing.v1.Tm.ManagementResponse> getAddTmChannelsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddTmChannels",
      requestType = tm.processing.v1.Tm.AddTmChannelsRequest.class,
      responseType = tm.processing.v1.Tm.ManagementResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tm.processing.v1.Tm.AddTmChannelsRequest,
      tm.processing.v1.Tm.ManagementResponse> getAddTmChannelsMethod() {
    io.grpc.MethodDescriptor<tm.processing.v1.Tm.AddTmChannelsRequest, tm.processing.v1.Tm.ManagementResponse> getAddTmChannelsMethod;
    if ((getAddTmChannelsMethod = SatelliteManagementGrpc.getAddTmChannelsMethod) == null) {
      synchronized (SatelliteManagementGrpc.class) {
        if ((getAddTmChannelsMethod = SatelliteManagementGrpc.getAddTmChannelsMethod) == null) {
          SatelliteManagementGrpc.getAddTmChannelsMethod = getAddTmChannelsMethod =
              io.grpc.MethodDescriptor.<tm.processing.v1.Tm.AddTmChannelsRequest, tm.processing.v1.Tm.ManagementResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddTmChannels"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.AddTmChannelsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tm.processing.v1.Tm.ManagementResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SatelliteManagementMethodDescriptorSupplier("AddTmChannels"))
              .build();
        }
      }
    }
    return getAddTmChannelsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SatelliteManagementStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SatelliteManagementStub>() {
        @java.lang.Override
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
        @java.lang.Override
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
        @java.lang.Override
        public SatelliteManagementFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SatelliteManagementFutureStub(channel, callOptions);
        }
      };
    return SatelliteManagementFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * 卫星目录管理服务：供前端创建、编辑、启停卫星，并配置参数表、解扰规则与通道。
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void listSatellites(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ListSatellitesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListSatellitesMethod(), responseObserver);
    }

    /**
     */
    default void getSatellite(tm.processing.v1.Tm.GetSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.GetSatelliteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSatelliteMethod(), responseObserver);
    }

    /**
     */
    default void createSatellite(tm.processing.v1.Tm.CreateSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateSatelliteMethod(), responseObserver);
    }

    /**
     */
    default void updateSatellite(tm.processing.v1.Tm.UpdateSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateSatelliteMethod(), responseObserver);
    }

    /**
     */
    default void deleteSatellite(tm.processing.v1.Tm.DeleteSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteSatelliteMethod(), responseObserver);
    }

    /**
     */
    default void setSatelliteEnabled(tm.processing.v1.Tm.SetEnabledRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetSatelliteEnabledMethod(), responseObserver);
    }

    /**
     */
    default void setChannelEnabled(tm.processing.v1.Tm.SetChannelEnabledRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetChannelEnabledMethod(), responseObserver);
    }

    /**
     */
    default void setChannelDescramble(tm.processing.v1.Tm.SetChannelDescrambleRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetChannelDescrambleMethod(), responseObserver);
    }

    /**
     */
    default void uploadTable(tm.processing.v1.Tm.UploadTableRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUploadTableMethod(), responseObserver);
    }

    /**
     */
    default void setDescrambleRule(tm.processing.v1.Tm.SetDescrambleRuleRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetDescrambleRuleMethod(), responseObserver);
    }

    /**
     */
    default void addTmChannels(tm.processing.v1.Tm.AddTmChannelsRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddTmChannelsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service SatelliteManagement.
   * <pre>
   * 卫星目录管理服务：供前端创建、编辑、启停卫星，并配置参数表、解扰规则与通道。
   * </pre>
   */
  public static abstract class SatelliteManagementImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return SatelliteManagementGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service SatelliteManagement.
   * <pre>
   * 卫星目录管理服务：供前端创建、编辑、启停卫星，并配置参数表、解扰规则与通道。
   * </pre>
   */
  public static final class SatelliteManagementStub
      extends io.grpc.stub.AbstractAsyncStub<SatelliteManagementStub> {
    private SatelliteManagementStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SatelliteManagementStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SatelliteManagementStub(channel, callOptions);
    }

    /**
     */
    public void listSatellites(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ListSatellitesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListSatellitesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSatellite(tm.processing.v1.Tm.GetSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.GetSatelliteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSatelliteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createSatellite(tm.processing.v1.Tm.CreateSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateSatelliteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateSatellite(tm.processing.v1.Tm.UpdateSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateSatelliteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteSatellite(tm.processing.v1.Tm.DeleteSatelliteRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteSatelliteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setSatelliteEnabled(tm.processing.v1.Tm.SetEnabledRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetSatelliteEnabledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setChannelEnabled(tm.processing.v1.Tm.SetChannelEnabledRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetChannelEnabledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setChannelDescramble(tm.processing.v1.Tm.SetChannelDescrambleRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetChannelDescrambleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void uploadTable(tm.processing.v1.Tm.UploadTableRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUploadTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setDescrambleRule(tm.processing.v1.Tm.SetDescrambleRuleRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetDescrambleRuleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addTmChannels(tm.processing.v1.Tm.AddTmChannelsRequest request,
        io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddTmChannelsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service SatelliteManagement.
   * <pre>
   * 卫星目录管理服务：供前端创建、编辑、启停卫星，并配置参数表、解扰规则与通道。
   * </pre>
   */
  public static final class SatelliteManagementBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SatelliteManagementBlockingStub> {
    private SatelliteManagementBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SatelliteManagementBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SatelliteManagementBlockingStub(channel, callOptions);
    }

    /**
     */
    public tm.processing.v1.Tm.ListSatellitesResponse listSatellites(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListSatellitesMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.GetSatelliteResponse getSatellite(tm.processing.v1.Tm.GetSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSatelliteMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse createSatellite(tm.processing.v1.Tm.CreateSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateSatelliteMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse updateSatellite(tm.processing.v1.Tm.UpdateSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateSatelliteMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse deleteSatellite(tm.processing.v1.Tm.DeleteSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteSatelliteMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse setSatelliteEnabled(tm.processing.v1.Tm.SetEnabledRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetSatelliteEnabledMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse setChannelEnabled(tm.processing.v1.Tm.SetChannelEnabledRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetChannelEnabledMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse setChannelDescramble(tm.processing.v1.Tm.SetChannelDescrambleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetChannelDescrambleMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse uploadTable(tm.processing.v1.Tm.UploadTableRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUploadTableMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse setDescrambleRule(tm.processing.v1.Tm.SetDescrambleRuleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetDescrambleRuleMethod(), getCallOptions(), request);
    }

    /**
     */
    public tm.processing.v1.Tm.ManagementResponse addTmChannels(tm.processing.v1.Tm.AddTmChannelsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddTmChannelsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service SatelliteManagement.
   * <pre>
   * 卫星目录管理服务：供前端创建、编辑、启停卫星，并配置参数表、解扰规则与通道。
   * </pre>
   */
  public static final class SatelliteManagementFutureStub
      extends io.grpc.stub.AbstractFutureStub<SatelliteManagementFutureStub> {
    private SatelliteManagementFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SatelliteManagementFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SatelliteManagementFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ListSatellitesResponse> listSatellites(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListSatellitesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.GetSatelliteResponse> getSatellite(
        tm.processing.v1.Tm.GetSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSatelliteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> createSatellite(
        tm.processing.v1.Tm.CreateSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateSatelliteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> updateSatellite(
        tm.processing.v1.Tm.UpdateSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateSatelliteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> deleteSatellite(
        tm.processing.v1.Tm.DeleteSatelliteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteSatelliteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> setSatelliteEnabled(
        tm.processing.v1.Tm.SetEnabledRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetSatelliteEnabledMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> setChannelEnabled(
        tm.processing.v1.Tm.SetChannelEnabledRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetChannelEnabledMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> setChannelDescramble(
        tm.processing.v1.Tm.SetChannelDescrambleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetChannelDescrambleMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> uploadTable(
        tm.processing.v1.Tm.UploadTableRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUploadTableMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> setDescrambleRule(
        tm.processing.v1.Tm.SetDescrambleRuleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetDescrambleRuleMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tm.processing.v1.Tm.ManagementResponse> addTmChannels(
        tm.processing.v1.Tm.AddTmChannelsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddTmChannelsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_SATELLITES = 0;
  private static final int METHODID_GET_SATELLITE = 1;
  private static final int METHODID_CREATE_SATELLITE = 2;
  private static final int METHODID_UPDATE_SATELLITE = 3;
  private static final int METHODID_DELETE_SATELLITE = 4;
  private static final int METHODID_SET_SATELLITE_ENABLED = 5;
  private static final int METHODID_SET_CHANNEL_ENABLED = 6;
  private static final int METHODID_SET_CHANNEL_DESCRAMBLE = 7;
  private static final int METHODID_UPLOAD_TABLE = 8;
  private static final int METHODID_SET_DESCRAMBLE_RULE = 9;
  private static final int METHODID_ADD_TM_CHANNELS = 10;

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
        case METHODID_LIST_SATELLITES:
          serviceImpl.listSatellites((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ListSatellitesResponse>) responseObserver);
          break;
        case METHODID_GET_SATELLITE:
          serviceImpl.getSatellite((tm.processing.v1.Tm.GetSatelliteRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.GetSatelliteResponse>) responseObserver);
          break;
        case METHODID_CREATE_SATELLITE:
          serviceImpl.createSatellite((tm.processing.v1.Tm.CreateSatelliteRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_UPDATE_SATELLITE:
          serviceImpl.updateSatellite((tm.processing.v1.Tm.UpdateSatelliteRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_DELETE_SATELLITE:
          serviceImpl.deleteSatellite((tm.processing.v1.Tm.DeleteSatelliteRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_SET_SATELLITE_ENABLED:
          serviceImpl.setSatelliteEnabled((tm.processing.v1.Tm.SetEnabledRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_SET_CHANNEL_ENABLED:
          serviceImpl.setChannelEnabled((tm.processing.v1.Tm.SetChannelEnabledRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_SET_CHANNEL_DESCRAMBLE:
          serviceImpl.setChannelDescramble((tm.processing.v1.Tm.SetChannelDescrambleRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_UPLOAD_TABLE:
          serviceImpl.uploadTable((tm.processing.v1.Tm.UploadTableRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_SET_DESCRAMBLE_RULE:
          serviceImpl.setDescrambleRule((tm.processing.v1.Tm.SetDescrambleRuleRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        case METHODID_ADD_TM_CHANNELS:
          serviceImpl.addTmChannels((tm.processing.v1.Tm.AddTmChannelsRequest) request,
              (io.grpc.stub.StreamObserver<tm.processing.v1.Tm.ManagementResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
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
          getListSatellitesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              tm.processing.v1.Tm.ListSatellitesResponse>(
                service, METHODID_LIST_SATELLITES)))
        .addMethod(
          getGetSatelliteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.GetSatelliteRequest,
              tm.processing.v1.Tm.GetSatelliteResponse>(
                service, METHODID_GET_SATELLITE)))
        .addMethod(
          getCreateSatelliteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.CreateSatelliteRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_CREATE_SATELLITE)))
        .addMethod(
          getUpdateSatelliteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.UpdateSatelliteRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_UPDATE_SATELLITE)))
        .addMethod(
          getDeleteSatelliteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.DeleteSatelliteRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_DELETE_SATELLITE)))
        .addMethod(
          getSetSatelliteEnabledMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.SetEnabledRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_SET_SATELLITE_ENABLED)))
        .addMethod(
          getSetChannelEnabledMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.SetChannelEnabledRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_SET_CHANNEL_ENABLED)))
        .addMethod(
          getSetChannelDescrambleMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.SetChannelDescrambleRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_SET_CHANNEL_DESCRAMBLE)))
        .addMethod(
          getUploadTableMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.UploadTableRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_UPLOAD_TABLE)))
        .addMethod(
          getSetDescrambleRuleMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.SetDescrambleRuleRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_SET_DESCRAMBLE_RULE)))
        .addMethod(
          getAddTmChannelsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              tm.processing.v1.Tm.AddTmChannelsRequest,
              tm.processing.v1.Tm.ManagementResponse>(
                service, METHODID_ADD_TM_CHANNELS)))
        .build();
  }

  private static abstract class SatelliteManagementBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SatelliteManagementBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tm.processing.v1.Tm.getDescriptor();
    }

    @java.lang.Override
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
    private final java.lang.String methodName;

    SatelliteManagementMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (SatelliteManagementGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SatelliteManagementFileDescriptorSupplier())
              .addMethod(getListSatellitesMethod())
              .addMethod(getGetSatelliteMethod())
              .addMethod(getCreateSatelliteMethod())
              .addMethod(getUpdateSatelliteMethod())
              .addMethod(getDeleteSatelliteMethod())
              .addMethod(getSetSatelliteEnabledMethod())
              .addMethod(getSetChannelEnabledMethod())
              .addMethod(getSetChannelDescrambleMethod())
              .addMethod(getUploadTableMethod())
              .addMethod(getSetDescrambleRuleMethod())
              .addMethod(getAddTmChannelsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
