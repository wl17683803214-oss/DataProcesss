package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.tool.PdxpParser;

/** 解析完成后的遥测消息存储接口，保留原名兼容现有调用，实际由新时序存储实现。 */
public interface IoTDBTelemetryStorageService {

    /**
     * 旧原始帧入口。未生成最终Proto时不写入时序数据库。
     *
     * @param config 采集接口配置快照
     * @param packet 已成功解析的PDXP包
     * @throws Exception 时序数据库连接或写入失败
     */
    void savePdxpRawFrame(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) throws Exception;

    /**
     * 保存本地参数解析失败的异常整帧，保留完整帧及各类头，不写参数记录。
     *
     * @param config 采集接口配置快照
     * @param packet 已成功解析且包含可拆分数据域的PDXP包
     * @throws Exception 时序数据库连接或写入失败
     */
    void markPdxpRawFrameAbnormal(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) throws Exception;

    /**
     * 旧独立写帧入口；统一批量入口同时写入整帧和参数。
     *
     * @param interfaceId 采集接口主键
     * @param message 已解析完成的遥测消息
     * @throws Exception 时序数据库连接或写入失败
     */
    void saveRawFrame(
            Long interfaceId,
            TelemetryMessage message) throws Exception;

    /**
     * 组装遥测消息的整帧和全部参数记录，提交跨帧合并缓冲。
     *
     * <p>返回只表示缓冲已接收，实际写入由后台确认；每帧源时间保持不变。</p>
     *
     * @param config 当前接口配置快照，包含设备卫星主键
     * @param message 已完成业务处理的遥测消息
     * @param rawFrame 完整采集帧，包含传输头、PDXP包头及完整数据域
     * @throws Exception 时序数据库连接或写入失败
     */
    void saveProcessedParameters(
            CollectInterfaceRuntimeConfig config,
            TelemetryMessage message,
            byte[] rawFrame) throws Exception;
}
