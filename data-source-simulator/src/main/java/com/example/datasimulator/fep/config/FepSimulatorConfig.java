package com.example.datasimulator.fep.config;

import com.example.datasimulator.config.SimulatorEnvironment;

import java.util.Locale;

/** 文件交换模拟源集中配置。 */
public final class FepSimulatorConfig {

    /** 待发送文件的完整路径或相对路径。 */
    public static final String FILE_PATH =
            SimulatorEnvironment.text(
                    "FEP_FILE_PATH",
                    "data-source-simulator/sample-data/fep-sample.txt");
    /** 文件发送使用的传输方式。 */
    public static final FepTransferType TRANSFER_TYPE =
            FepTransferType.valueOf(SimulatorEnvironment.text(
                    "FEP_TRANSFER_TYPE", FepTransferType.TCP.name())
                    .toUpperCase(Locale.ROOT));
    /** 接收端地址。 */
    public static final String TARGET_HOST = SimulatorEnvironment.text(
            "FEP_TARGET_HOST", "127.0.0.1");
    /** UDP接收端端口。 */
    public static final int UDP_TARGET_PORT = SimulatorEnvironment.integer(
            "FEP_UDP_TARGET_PORT", 19_002);
    /** TCP接收端端口。 */
    public static final int TCP_TARGET_PORT = SimulatorEnvironment.integer(
            "FEP_TCP_TARGET_PORT", 19_003);
    /** 每个文件数据单元的最大字节数。 */
    public static final int DATA_UNIT_LENGTH = 4_096;
    /** 相邻数据单元的发送间隔，单位毫秒。 */
    public static final long SEND_INTERVAL_MILLIS =
            SimulatorEnvironment.longValue("FEP_SEND_INTERVAL_MILLIS", 10L);
    /** 建立连接的最长等待时间，单位毫秒。 */
    public static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    /** 等待应答的最长时间，单位毫秒。 */
    public static final int RESPONSE_TIMEOUT_MILLIS = 10_000;

    /** 数据处理服务地址。 */
    public static final String DATA_PROCESS_SERVICE_URL =
            SimulatorEnvironment.text(
                    "DATA_PROCESS_SERVICE_URL", "http://127.0.0.1:8083");
    /** FEP UDP测试采集接口启动地址。 */
    public static final String UDP_COLLECTION_START_URL =
            DATA_PROCESS_SERVICE_URL
                    + "/internal/collection/test/fep/udp/start";
    /** FEP TCP测试采集接口启动地址。 */
    public static final String TCP_COLLECTION_START_URL =
            DATA_PROCESS_SERVICE_URL
                    + "/internal/collection/test/fep/tcp/start";
    /** FEP测试采集接口停止地址。 */
    public static final String COLLECTION_STOP_URL =
            DATA_PROCESS_SERVICE_URL
                    + "/internal/collection/test/fep/stop";
    /** 模拟源启动时是否自动通知数据处理侧启动FEP测试采集。 */
    public static final boolean AUTO_START_COLLECTION =
            SimulatorEnvironment.bool("FEP_AUTO_START_COLLECTION", true);
    /** 文件发送完成后是否自动停止FEP测试采集。 */
    public static final boolean AUTO_STOP_COLLECTION_AFTER_SEND =
            SimulatorEnvironment.bool(
                    "FEP_AUTO_STOP_COLLECTION_AFTER_SEND", false);
    /** 通知数据处理服务时使用的连接超时时间，单位毫秒。 */
    public static final int NOTIFY_CONNECT_TIMEOUT_MILLIS = 5_000;
    /** 等待数据处理服务响应的超时时间，单位毫秒。 */
    public static final int NOTIFY_READ_TIMEOUT_MILLIS = 5_000;

    /** 配置类不允许实例化。 */
    private FepSimulatorConfig() {
    }
}
