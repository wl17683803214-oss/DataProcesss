package com.example.dataprocess.service;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.tool.PdxpParser;

/** 解析完成后的遥测消息存储接口。 */
public interface IoTDBTelemetryStorageService {

    /**
     * 保存未能生成最终遥测消息的完整PDXP原始帧。
     *
     * <p>该方法用于解析失败或后续处理失败时保留排查数据。</p>
     *
     * @param config 采集接口配置快照
     * @param packet 已成功解析的PDXP包
     * @throws Exception IoTDB连接或写入失败
     */
    void savePdxpRawFrame(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) throws Exception;

    /**
     * 将已保存PDXP原始帧的校验结果更新为错误。
     *
     * @param config 采集接口配置快照
     * @param packet 已保存的PDXP包
     * @throws Exception IoTDB连接或写入失败
     */
    void markPdxpRawFrameAbnormal(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) throws Exception;

    /**
     * 保存一条遥测消息的完整原始帧。
     *
     * <p>当前只保存正常原始帧；异常帧写入规则尚未确认并保留TODO。
     * 原始帧用于“实时遥测数据”页面查询，不包含解析后的参数值。</p>
     *
     * @param message 已解析完成的遥测消息
     * @throws Exception IoTDB连接或写入失败
     */
    void saveRawFrame(TelemetryMessage message) throws Exception;

    /**
     * 一次批量保存遥测消息的整帧记录和全部参数记录。
     *
     * <p>整帧写入帧节点，每个遥测参数写入独立设备节点，二者使用相同消息时间。</p>
     *
     * @param interfaceId 采集接口主键，非采集链路调用时允许为空
     * @param message 已完成业务处理的遥测消息
     * @throws Exception IoTDB连接或写入失败
     */
    void saveProcessedParameters(
            Long interfaceId,
            TelemetryMessage message) throws Exception;
}
