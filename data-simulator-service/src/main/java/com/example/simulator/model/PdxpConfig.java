package com.example.simulator.model;

/** 单个模拟源的PDXP十六进制参数，字段逐项按小端发送。 */
public class PdxpConfig {
    /** 所属模拟源。 */
    public Long sourceId;
    /** 每次从文件读取的完整帧长度。 */
    public Integer inputFrameLength = 512;
    /** 传输头字节长度，零表示不附加传输头。 */
    public Integer transportHeaderLength = 2;
    /** 从原帧开头移动到数据域末尾的字节长度。 */
    public Integer telemetryHeaderLength = 4;
    /** 自定义数据字节长度，零表示不附加自定义数据。 */
    public Integer customDataLength = 5;
    /** 与传输头长度匹配的十六进制内容。 */
    public String transportHeader = "1234";
    /** 与自定义数据长度匹配的十六进制内容。 */
    public String customData = "0102030405";
    /** 版本。 */
    public String ver = "80";
    /** 任务标志。 */
    public String mid = "0001";
    /** 信源地址。 */
    public String sid = "00000001";
    /** 信宿地址。 */
    public String did = "00000001";
    /** 数据标志。 */
    public String bid = "00000001";
    /** 包序号初值。 */
    public String initialNo = "00000000";
    /** 数据处理标志。 */
    public String flag = "00";
}

