package com.example.dataprocess.service.impl;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.FrameCheckResultType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * 按卫星、通道和遥测类型将一帧遥测数据写入 IoTDB。
 *
 * <p>路径格式：{@code root.db.{sat_code}.tms.{channel_code}.{tm_type}.{telemetry_key}}。
 * 每个参数节点下面固定保存 value（物理量）、state（状态）和 raw（原码）。</p>
 */
@Service
@ConditionalOnProperty(
        prefix = "iotdb",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class IoTDBTelemetryStorageServiceImpl implements IoTDBTelemetryStorageService {

    /** 项目约定的固定 IoTDB 数据库根路径。 */
    private static final String ROOT_PATH = "root.db";

    private final SessionPool sessionPool;

    public IoTDBTelemetryStorageServiceImpl(SessionPool sessionPool) {
        this.sessionPool = sessionPool;
    }

    /** 服务启动时自动创建数据库，数据库已存在时不重复创建。 */
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

    /** 保存UDP采集链路中已经成功解析的完整PDXP原始帧。 */
    @Override
    public void savePdxpRawFrame(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) throws Exception {
        // TODO 从协议配置解析正式卫星代号、通道代号和遥测类型后，替换当前临时路径。
        String satelliteCode = "unknown_satellite";
        String channelCode = String.valueOf(config.getInterfaceId());
        String parameterRoot = ROOT_PATH + "." + satelliteCode
                + ".tms." + channelNode(channelCode) + ".unknown";
        byte[] rawPacket = packet.getRawPacket();
        long timestamp = pdxpSendTimeMillis(packet);

        // 正常原始帧使用完整PDXP包，不使用尚未生成的最终遥测消息。
        sessionPool.insertRecord(
                parameterRoot + "._frame",
                timestamp,
                Arrays.asList(
                        "interfaceId", "taskId", "satelliteCode", "channelCode", "rawLength",
                        "raw", "uniqueCode", "checkResult",
                        "checkResultName"),
                Arrays.asList(
                        TSDataType.INT64, TSDataType.TEXT, TSDataType.TEXT, TSDataType.TEXT,
                        TSDataType.INT32, TSDataType.BLOB, TSDataType.TEXT, TSDataType.INT32,
                        TSDataType.TEXT),
                Arrays.<Object>asList(
                        config.getInterfaceId(),
                        text(config.getTaskId()),
                        text(satelliteCode),
                        text(channelCode),
                        rawPacket.length,
                        new Binary(rawPacket),
                        text("接口" + config.getInterfaceId()
                                + "-序号" + packet.getSequenceNumber()),
                        FrameCheckResultType.CHECK_TYPE_CORRECT_VALUE,
                        text("正确")));
    }

    /** 在原始帧的同一路径和时间戳上更新校验结果。 */
    @Override
    public void markPdxpRawFrameAbnormal(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet) throws Exception {
        // TODO 从协议配置解析正式卫星代号、通道代号和遥测类型后，替换当前临时路径。
        String satelliteCode = "unknown_satellite";
        String channelCode = String.valueOf(config.getInterfaceId());
        String parameterRoot = ROOT_PATH + "." + satelliteCode
                + ".tms." + channelNode(channelCode) + ".unknown";

        // 只更新已保存记录的校验字段，不再次写入原始字节。
        sessionPool.insertRecord(
                parameterRoot + "._frame",
                pdxpSendTimeMillis(packet),
                Arrays.asList("checkResult", "checkResultName"),
                Arrays.asList(TSDataType.INT32, TSDataType.TEXT),
                Arrays.<Object>asList(
                        FrameCheckResultType.CHECK_TYPE_ERROR_VALUE,
                        text("错误")));
    }

    @Override
    public void saveRawFrame(TelemetryMessage message) throws Exception {
        String parameterRoot = buildParameterRoot(message);
        long timestamp = sourceTimeMillis(message);
        saveFrame(message, parameterRoot, timestamp);
    }

    @Override
    public void saveProcessedParameters(
            Long interfaceId,
            TelemetryMessage message)
            throws Exception {
        String parameterRoot = buildParameterRoot(message);
        long timestamp = sourceTimeMillis(message);
        int recordCount = message.getTelemetriesCount() + 1;
        List<String> deviceIds = new ArrayList<>(recordCount);
        List<Long> timestamps = new ArrayList<>(recordCount);
        List<List<String>> measurementsList = new ArrayList<>(recordCount);
        List<List<TSDataType>> dataTypesList = new ArrayList<>(recordCount);
        List<List<Object>> valuesList = new ArrayList<>(recordCount);

        // 第一步：将整帧原码和帧级业务信息加入本次批量写入。
        List<String> frameMeasurements = Arrays.asList(
                "interfaceId", "taskId", "satelliteCode", "channelName",
                "businessId", "rawLength", "raw", "frameCheckStatus");
        List<TSDataType> frameDataTypes = Arrays.asList(
                TSDataType.INT64, TSDataType.TEXT, TSDataType.TEXT,
                TSDataType.TEXT, TSDataType.TEXT, TSDataType.INT32,
                TSDataType.BLOB, TSDataType.BOOLEAN);
        List<Object> frameValues = Arrays.<Object>asList(
                interfaceId == null ? 0L : interfaceId,
                text(message.hasProtoHead()
                        ? message.getProtoHead().getTaskId() : ""),
                text(message.getSatCode()),
                text(message.getChannelName()),
                text(message.getBussinessId()),
                message.getFrameRawData().size(),
                new Binary(message.getFrameRawData().toByteArray()),
                message.getFrameCheckStatus());
        deviceIds.add(parameterRoot + "._frame");
        timestamps.add(timestamp);
        measurementsList.add(frameMeasurements);
        dataTypesList.add(frameDataTypes);
        valuesList.add(frameValues);

        // 第二步：telemetries的键作为参数节点，追加本帧全部处理后参数。
        for (Map.Entry<String, Telemetry> entry : message.getTelemetriesMap().entrySet()) {
            Telemetry telemetry = entry.getValue();
            List<String> measurements = new ArrayList<>();
            List<TSDataType> dataTypes = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            // 先按固定顺序写入参数身份和Proto基础字段。
            measurements.add("interfaceId");
            dataTypes.add(TSDataType.INT64);
            values.add(interfaceId == null ? 0L : interfaceId);
            measurements.add("taskId");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(message.hasProtoHead()
                    ? message.getProtoHead().getTaskId() : ""));
            measurements.add("satelliteCode");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(message.getSatCode()));
            measurements.add("channelName");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(message.getChannelName()));
            measurements.add("businessId");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(message.getBussinessId()));
            measurements.add("tableIndex");
            dataTypes.add(TSDataType.INT32);
            values.add(telemetry.getTableIndex());
            measurements.add("tmSymbol");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(telemetry.getTmSymbol()));
            measurements.add("tmName");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(telemetry.getTmName()));
            // value的数据类型根据Protobuf中的ValueType动态确定。
            measurements.add("value");
            appendPhysicalValue(telemetry, dataTypes, values);
            measurements.add("valueText");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(telemetry.getValueText()));
            measurements.add("valueType");
            dataTypes.add(TSDataType.INT32);
            values.add(telemetry.getValueTypeValue());
            measurements.add("raw");
            dataTypes.add(TSDataType.BLOB);
            values.add(new Binary(telemetry.getRawData().toByteArray()));
            measurements.add("stateType");
            dataTypes.add(TSDataType.INT32);
            values.add(telemetry.getStateTypeValue());
            // 状态名称为空表示未命中任何范围，此时不创建stateIndex测点。
            if (!telemetry.getStateName().trim().isEmpty()) {
                measurements.add("stateIndex");
                dataTypes.add(TSDataType.INT32);
                values.add(telemetry.getStateIndex());
            }
            measurements.add("stateName");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(telemetry.getStateName()));
            measurements.add("systemName");
            dataTypes.add(TSDataType.TEXT);
            values.add(text(telemetry.getSystemName()));
            measurements.add("isWildValue");
            dataTypes.add(TSDataType.BOOLEAN);
            values.add(telemetry.getIsWildValue());
            measurements.add("deduplication");
            dataTypes.add(TSDataType.TEXT);
            // 同代号参数保留最后一项时写入去重，其余参数写入原始。
            values.add(text( "原始"));
            measurements.add("processedAt");
            dataTypes.add(TSDataType.INT64);
            values.add(System.currentTimeMillis());

            deviceIds.add(parameterRoot + "." + toPathNode(entry.getKey(), "unknown_tm"));
            timestamps.add(timestamp);
            measurementsList.add(measurements);
            dataTypesList.add(dataTypes);
            valuesList.add(values);
        }

        // 第三步：整帧记录与全部参数记录一次性提交给IoTDB。
        sessionPool.insertRecords(
                deviceIds, timestamps, measurementsList, dataTypesList, valuesList);
    }

    /**
     * 在原参数树下写入 {@code _frame} 设备。
     * 当前由正常帧流程调用；异常帧写入规则尚未确认并保留TODO。
     * 完整帧与处理后参数使用消息时间，依靠相同时间戳建立对应关系。
     */
    private void saveFrame(
            TelemetryMessage message,
            String parameterRoot,
            long timestamp) throws Exception {
        sessionPool.insertRecord(
                parameterRoot + "._frame",
                timestamp,
                Arrays.asList(
                        "taskId", "satelliteCode", "channelName", "businessId",
                        "rawLength", "raw", "frameCheckStatus"),
                Arrays.asList(
                        TSDataType.TEXT, TSDataType.TEXT, TSDataType.TEXT,
                        TSDataType.TEXT, TSDataType.INT32, TSDataType.BLOB,
                        TSDataType.BOOLEAN),
                Arrays.<Object>asList(
                        text(message.hasProtoHead()
                                ? message.getProtoHead().getTaskId() : ""),
                        text(message.getSatCode()),
                        text(message.getChannelName()),
                        text(message.getBussinessId()),
                        message.getFrameRawData().size(),
                        new Binary(message.getFrameRawData().toByteArray()),
                        message.getFrameCheckStatus()));
    }

    private String buildParameterRoot(TelemetryMessage message) {
        return ROOT_PATH + "."
                + toPathNode(message.getSatCode(), "unknown_satellite")
                + ".tms."
                + channelNode(message.getChannelName()) + "."
                + telemetryTypeNode(message.getTmType());
    }

    private void appendPhysicalValue(
            Telemetry telemetry,
            List<TSDataType> dataTypes,
            List<Object> values) {
        ValueType valueType = telemetry.getValueType();
        switch (valueType) {
            case VALUE_TYPE_TEXT:
                dataTypes.add(TSDataType.TEXT);
                values.add(text(telemetry.getValueText()));
                break;
            case VALUE_TYPE_INTEGER:
            case VALUE_TYPE_TIME:
                dataTypes.add(TSDataType.INT64);
                values.add((long) telemetry.getValue());
                break;
            case VALUE_TYPE_FLOAT:
                dataTypes.add(TSDataType.FLOAT);
                values.add((float) telemetry.getValue());
                break;
            case VALUE_TYPE_RAW:
            case VALUE_TYPE_BINARY:
                // value 始终表示物理量；原始字节单独写入 raw 字段。
                dataTypes.add(TSDataType.DOUBLE);
                values.add(telemetry.getValue());
                break;
            case VALUE_TYPE_DOUBLE:
            case VALUE_TYPE_UNSPECIFIED:
            case UNRECOGNIZED:
            default:
                dataTypes.add(TSDataType.DOUBLE);
                values.add(telemetry.getValue());
                break;
        }
    }

    private String channelNode(String channelCode) {
        String value = channelCode == null ? "" : channelCode.trim();
        if (value.matches("(?i)ch.+")) {
            return toPathNode(value, "unknown_channel");
        }
        if (value.matches("\\d+")) {
            return "ch" + value;
        }
        return toPathNode(value, "unknown_channel");
    }

    private String telemetryTypeNode(TelemetryType type) {
        switch (type) {
            case TM_TYPE_REAL_TM:
                return "real";
            case TM_TYPE_DELAY_TM:
                return "delay";
            case TM_TYPE_MEMORY_TM:
                return "memory";
            case TM_TYPE_DEVICE_TM:
                return "device";
            case TM_TYPE_UNSPECIFIED:
            case UNRECOGNIZED:
            default:
                return "unknown";
        }
    }

    private Binary text(String value) {
        return new Binary(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private long sourceTimeMillis(TelemetryMessage message) {
        if (!message.hasTime()) {
            return System.currentTimeMillis();
        }
        Timestamp timestamp = message.getTime();
        return timestamp.getSeconds() * 1000L + timestamp.getNanos() / 1_000_000L;
    }

    /** 将PDXP积日和当日时标换算为毫秒时间戳。 */
    private long pdxpSendTimeMillis(PdxpParser.PdxpPacket packet) {
        LocalDateTime sendTime = LocalDateTime.of(
                packet.getSendDate(), LocalTime.MIDNIGHT)
                .plus(packet.getTimeSinceMidnight());
        return sendTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    private String toPathNode(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String source = value.trim();
        String safe = source.replaceAll("[^A-Za-z0-9_]", "_");
        if (safe.matches("\\d+")) {
            return "`" + safe + "`";
        }
        if (safe.equals(source)) {
            return safe;
        }
        CRC32 crc = new CRC32();
        crc.update(source.getBytes(StandardCharsets.UTF_8));
        return safe + "_" + Long.toHexString(crc.getValue());
    }
}
