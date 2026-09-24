package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import com.example.common.tool.IoTDBPathTool;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.service.IoTDBTelemetryStorageService;
import com.example.dataprocess.tool.PdxpParser;
import com.google.protobuf.Timestamp;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.rpc.TSStatusCode;
import org.apache.iotdb.session.pool.SessionPool;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.utils.Binary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 按任务、设备卫星主键和遥测类型保存最终Proto数据。 */
@Service
@ConditionalOnProperty(prefix = "iotdb", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class IoTDBTelemetryStorageServiceImpl implements IoTDBTelemetryStorageService {
    /** 业务数据固定数据库路径。 */
    private static final String ROOT_PATH = "root.db";
    private final SessionPool sessionPool;

    public IoTDBTelemetryStorageServiceImpl(SessionPool sessionPool) {
        this.sessionPool = sessionPool;
    }

    /** 服务启动时创建不存在的IoTDB数据库。 */
    @PostConstruct
    public void initializeDatabase() throws Exception {
        try {
            sessionPool.createDatabase(ROOT_PATH);
        } catch (StatementExecutionException exception) {
            if (exception.getStatusCode()
                    != TSStatusCode.DATABASE_ALREADY_EXISTS.getStatusCode()) {
                throw exception;
            }
        }
    }

    /** 没有最终Proto时不写IoTDB。 */
    @Override
    public void savePdxpRawFrame(CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        // 当前存储结构要求有效的设备主键和最终数据域。
    }

    /** 没有最终Proto时不创建异常整帧记录。 */
    @Override
    public void markPdxpRawFrameAbnormal(CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) {
        // 当前存储结构不支持修改不存在的回退帧。
    }

    /** 旧独立写帧入口缺少设备主键，由统一批量入口处理。 */
    @Override
    public void saveRawFrame(Long interfaceId, TelemetryMessage message) {
        // 处理成功后由saveProcessedParameters一次保存整帧和参数。
    }

    /** 一次写入最终Proto的数据域和全部遥测参数。 */
    @Override
    public void saveProcessedParameters(CollectInterfaceRuntimeConfig config,
            TelemetryMessage message) throws Exception {
        if (config == null || config.getDeviceSatelliteId() == null
                || config.getDeviceSatelliteId() <= 0 || message == null
                || !message.hasProtoHead()) {
            throw new IllegalArgumentException("IoTDB写入缺少任务、设备卫星主键或遥测消息");
        }
        String taskId = message.getProtoHead().getTaskId();
        if (!config.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException("遥测消息任务编号与采集接口不一致");
        }
        String parameterRoot = ROOT_PATH + "."
                + IoTDBPathTool.prefixedPathNode("task_", taskId, "unknown_task")
                + "." + IoTDBPathTool.prefixedPathNode("device_",
                        String.valueOf(config.getDeviceSatelliteId()), "unknown_device")
                + ".tms." + telemetryTypeNode(message.getTmType());
        long timestamp = sourceTimeMillis(message);
        int recordCount = message.getTelemetriesCount() + 1;
        List<String> deviceIds = new ArrayList<>(recordCount);
        List<Long> timestamps = new ArrayList<>(recordCount);
        List<List<String>> measurementsList = new ArrayList<>(recordCount);
        List<List<TSDataType>> dataTypesList = new ArrayList<>(recordCount);
        List<List<Object>> valuesList = new ArrayList<>(recordCount);

        // 整帧只保存设备名称、Proto通道名称、去头数据域和中文检查结果。
        deviceIds.add(parameterRoot + "._frame");
        timestamps.add(timestamp);
        measurementsList.add(Arrays.asList(
                "deviceSatelliteName", "channelName", "raw", "checkResultName"));
        dataTypesList.add(Arrays.asList(
                TSDataType.TEXT, TSDataType.TEXT, TSDataType.BLOB, TSDataType.TEXT));
        valuesList.add(Arrays.<Object>asList(
                text(config.getDeviceSatelliteName()), text(message.getChannelName()),
                new Binary(message.getFrameRawData().toByteArray()), text("正常")));

        // 参数数值严格取Proto的value，不再使用valueText或派生状态索引。
        for (Map.Entry<String, Telemetry> entry : message.getTelemetriesMap().entrySet()) {
            Telemetry telemetry = entry.getValue();
            String telemetryCode = telemetry.getTmSymbol().trim().isEmpty()
                    ? entry.getKey() : telemetry.getTmSymbol();
            deviceIds.add(parameterRoot + "."
                    + IoTDBPathTool.pathNode(telemetryCode, "unknown_tm"));
            timestamps.add(timestamp);
            measurementsList.add(Arrays.asList(
                    "tmName", "value", "raw", "stateName", "processedAt"));
            dataTypesList.add(Arrays.asList(
                    TSDataType.TEXT, TSDataType.DOUBLE, TSDataType.BLOB,
                    TSDataType.TEXT, TSDataType.INT64));
            valuesList.add(Arrays.<Object>asList(
                    text(telemetry.getTmName()), telemetry.getValue(),
                    new Binary(telemetry.getRawData().toByteArray()),
                    text(telemetry.getStateName()), System.currentTimeMillis()));
        }
        sessionPool.insertRecords(deviceIds, timestamps,
                measurementsList, dataTypesList, valuesList);
    }

    /** 将遥测类型编码映射到稳定的路径节点。 */
    private String telemetryTypeNode(TelemetryType type) {
        switch (type) {
            case TM_TYPE_REAL_TM: return "real";
            case TM_TYPE_DELAY_TM: return "delay";
            case TM_TYPE_MEMORY_TM: return "memory";
            case TM_TYPE_DEVICE_TM: return "device";
            default: return "unknown";
        }
    }

    /** IoTDB文本统一用UTF-8编码。 */
    private Binary text(String value) {
        return new Binary(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** 帧与所有参数共用Proto消息时间，缺省时使用当前时间。 */
    private long sourceTimeMillis(TelemetryMessage message) {
        if (!message.hasTime()) {
            return System.currentTimeMillis();
        }
        Timestamp timestamp = message.getTime();
        return timestamp.getSeconds() * 1000L + timestamp.getNanos() / 1_000_000L;
    }
}
