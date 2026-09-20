package com.example.datasimulator.pdxp.config;

import com.example.datasimulator.config.SimulatorEnvironment;

/** UDP模拟数据源的集中配置。 */
public final class PdxpSimulatorConfig {

    /** 每个输入遥测帧的固定长度。 */
    public static final int INPUT_FRAME_LENGTH = 512;
    /** 输入遥测帧开头需要移动的帧头长度。 */
    public static final int TELEMETRY_HEADER_LENGTH = 4;
    /** PDXP固定包头长度。 */
    public static final int PDXP_HEADER_LENGTH = 32;
    /** 输入文件路径。 */
    public static final String FILE_PATH =
            SimulatorEnvironment.text(
                    "PDXP_FILE_PATH",
                    "D:\\工作文档\\数据处理软件\\模拟源文件\\temp916.dat");
    /** UDP目标地址。 */
    public static final String TARGET_HOST = SimulatorEnvironment.text(
            "PDXP_TARGET_HOST", "127.0.0.1");
    /** UDP目标端口。 */
    public static final int TARGET_PORT = SimulatorEnvironment.integer(
            "PDXP_TARGET_PORT", 9001);
    /** 相邻两帧的发送间隔，单位毫秒。 */
    public static final long SEND_INTERVAL_MILLIS =
            SimulatorEnvironment.longValue(
                    "PDXP_SEND_INTERVAL_MILLIS", 1000L);

    /** 数据处理服务地址。 */
    public static final String DATA_PROCESS_SERVICE_URL =
            SimulatorEnvironment.text(
                    "DATA_PROCESS_SERVICE_URL", "http://127.0.0.1:8083");
    /** 数据处理侧测试采集接口启动地址。 */
    public static final String COLLECTION_START_URL =
            DATA_PROCESS_SERVICE_URL
                    + "/internal/collection/test/pdxp/start";
    /** 数据处理侧测试采集接口停止地址。 */
    public static final String COLLECTION_STOP_URL =
            DATA_PROCESS_SERVICE_URL
                    + "/internal/collection/test/pdxp/stop";
    /** 模拟源启动时是否自动通知数据处理侧启动测试采集。 */
    public static final boolean AUTO_START_COLLECTION =
            SimulatorEnvironment.bool("PDXP_AUTO_START_COLLECTION", true);
    /** 模拟数据发送结束后是否自动停止数据处理侧测试采集。 */
    public static final boolean AUTO_STOP_COLLECTION_AFTER_SEND =
            SimulatorEnvironment.bool(
                    "PDXP_AUTO_STOP_COLLECTION_AFTER_SEND", false);
    /** 通知数据处理服务时使用的连接超时时间，单位毫秒。 */
    public static final int NOTIFY_CONNECT_TIMEOUT_MILLIS = 5_000;
    /** 等待数据处理服务响应的超时时间，单位毫秒。 */
    public static final int NOTIFY_READ_TIMEOUT_MILLIS = 5_000;

    /** 用户填写的两字节传输头，填写时按大端阅读。 */
    public static final String TRANSPORT_HEADER = "1234";
    /** 用户填写的五字节自定义数据，填写时按大端阅读。 */
    public static final String CUSTOM_DATA = "0102030405";

    /** 版本字段，一字节。 */
    public static final String VER = "80";
    /** 任务标志，两字节。 */
    public static final String MID = "0001";
    /** 信源地址，四字节。 */
    public static final String SID = "00000001";
    /** 信宿地址，四字节。 */
    public static final String DID = "00000001";
    /** 数据标志，四字节。 */
    public static final String BID = "00000001";
    /** 包序号初始值，四字节，之后每发送一帧自动加一。 */
    public static final String NO = "00000000";
    /** 数据处理标志，一字节。 */
    public static final String FLAG = "00";

    /** 配置类不允许实例化。 */
    private PdxpSimulatorConfig() {
    }
}
