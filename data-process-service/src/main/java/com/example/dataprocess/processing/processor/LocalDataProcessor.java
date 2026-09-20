
package com.example.dataprocess.processing.processor;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.StateType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.Telemetry;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryMessage;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import SatDataCenter.DataExchange.TmTc.TelemetryMessages.ValueType;
import SatDataCenter.DataExchange.TmTc.Version.ExchangeTopicType;
import SatDataCenter.DataExchange.TmTc.Version.ProtoHeadInfo;
import SatDataCenter.DataExchange.TmTc.Version.SubTopicName;
import com.example.dataprocess.collection.CollectInterfaceStatistics;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.entity.PdxpFrameSource;
import com.example.dataprocess.mapper.TelemetryCodeMappingMapper;
import com.example.dataprocess.protocol.rpc.PdxpDataPayload;
import com.example.dataprocess.protocol.rpc.PdxpProtocolField;
import com.example.dataprocess.protocol.rpc.ProtocolConfigParser;
import com.example.dataprocess.tool.LittleEndianBitReader;
import com.example.dataprocess.tool.PdxpParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 使用采集接口关联协议配置完成本地解析。 */
@Component
public class LocalDataProcessor implements DataProcessor {
    /** 本地数据处理日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            LocalDataProcessor.class);
    /** 状态范围格式。 */
    private static final Pattern RANGE = Pattern.compile(
            "^\\s*([^:：/]+)\\s*[:：]\\s*\\[\\s*([^,，]+)\\s*[,，]\\s*([^\\]]+)\\s*\\]\\s*$");
    /** 协议配置解析器。 */
    private final ProtocolConfigParser parser;
    /** 参数解析配置和设备名称查询组件。 */
    private final TelemetryCodeMappingMapper mappingMapper;
    /** 本地处理去重数量统计组件。 */
    private final CollectInterfaceStatistics statistics;
    /** 按任务和参数解析配置缓存设备名称拆分结果。 */
    private final ConcurrentMap<String, FrameIdentity> identityCache =
            new ConcurrentHashMap<String, FrameIdentity>();

    /** 注入协议配置解析器和设备名称查询组件。 */
    public LocalDataProcessor(
            ProtocolConfigParser parser,
            TelemetryCodeMappingMapper mappingMapper,
            CollectInterfaceStatistics statistics) {
        this.parser = parser;
        this.mappingMapper = mappingMapper;
        this.statistics = statistics;
    }

    /** 按字段位宽解码并组装遥测消息。 */
    @Override
    public Optional<TelemetryMessage> process(
            CollectInterfaceRuntimeConfig config,
            PdxpParser.PdxpPacket packet,
            PdxpDataPayload payload) {
        // 记录本帧处理起点，用于输出完整处理耗时。
        long startNanos = System.nanoTime();
        // 第一步：解析并校验协议配置字段。
        List<PdxpProtocolField> fields = fields(
                config.getProtocolConfigParams());
        byte[] data = payload.getData();
        LittleEndianBitReader reader = new LittleEndianBitReader(data);
        Instant time = packet.getSendDate().atStartOfDay()
                .plus(packet.getTimeSinceMidnight()).toInstant(ZoneOffset.ofHours(8));
        FrameIdentity identity = frameIdentity(config, fields);
        ProtoHeadInfo protoHead = ProtoHeadInfo.newBuilder()
                .setTopicType(ExchangeTopicType.TEST_DATA_TYPE)
                .setBussiness(SubTopicName.DATA_SUBSYSTEM_YCHL_PHYVALUE)
                .setTaskId(config.getTaskId())
                .setMsgSource("数据处理")
                .build();
        TelemetryMessage.Builder message = TelemetryMessage.newBuilder()
                .setProtoHead(protoHead)
                .setTime(Timestamp.newBuilder().setSeconds(time.getEpochSecond()).setNanos(time.getNano()))
                .setSatCode(identity.satelliteCode)
                .setFrameRawData(ByteString.copyFrom(data))
                .setChannelName(identity.channelName)
                .setTmType(TelemetryType.TM_TYPE_DEVICE_TM)
                .setBussinessId(identity.businessId)
                .setFrameCheckStatus(true);
        // 第二步：依次取位并构造每个遥测对象，相同遥测代号只保留最后一项。
        Set<String> decodedSymbols = new HashSet<String>();
        long duplicateCount = 0L;
        for (PdxpProtocolField item : fields) {
            byte[] raw = reader.read(item.getBitWidth());
            BigInteger value = unsigned(raw);
            String tmSymbol = item.getTelemetryCode().trim();
            boolean duplicate = !decodedSymbols.add(tmSymbol);
            Telemetry.Builder telemetry = Telemetry.newBuilder()
                    .setTableIndex(item.getTableIndex())
                    .setTmSymbol(tmSymbol)
                    .setValue(value.doubleValue())
                    .setValueText(value.toString())
                    // TODO 后续根据公式类型设置对应的数据类型。
                    .setValueType(ValueType.VALUE_TYPE_RAW)
                    .setRawData(ByteString.copyFrom(raw));
            if (duplicate) {
                // 后出现的同代号参数覆盖前值，并在最终Proto中标记发生过去重。
//                telemetry.setIsDepValue(true);
                duplicateCount++;
            }
            // 每个遥测参数保存自己对应的校准公式，不再作为帧级通道编号使用。
            if (text(item.getCalibrationFormula())) {
                telemetry.setCalibrationFormula(
                        item.getCalibrationFormula().trim());
            }
            if (text(item.getTelemetryName())) {
                telemetry.setTmName(item.getTelemetryName().trim());
            }
            if (text(item.getSystemName())) {
                telemetry.setSystemName(item.getSystemName().trim());
            }
            state(telemetry, item, value);
            message.putTelemetries(
                    telemetry.getTmSymbol(), telemetry.build());
        }
        // 第三步：每个被覆盖参数计入处理统计，最终只保留后出现的参数。
        statistics.recordDuplicate(config, duplicateCount);
        // 第四步：构造结果并记录本帧本地处理完成信息。
        TelemetryMessage result = message.build();
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        LOGGER.info(
                "本地处理完成，接口编号：{}，包序号：{}，参数数量：{}，"
                        + "原始数据长度：{}，处理耗时：{}毫秒",
                config.getInterfaceId(),
                packet.getSequenceNumber(),
                result.getTelemetriesCount(),
                data.length,
                elapsedMillis);
        return Optional.of(result);
    }

    /** 转换、校验并排序配置字段。 */
    private List<PdxpProtocolField> fields(String configParams) {
        List<PdxpProtocolField> parsedFields = parser.parseFields(
                configParams, PdxpProtocolField.class);
        if (parsedFields.isEmpty()) {
            throw new IllegalArgumentException("协议配置没有有效的fields字段");
        }
        // 创建可排序副本，缓存中的协议字段列表保持只读和原始顺序。
        List<PdxpProtocolField> result = new ArrayList<PdxpProtocolField>(
                parsedFields);
        Set<Integer> indexes = new HashSet<Integer>();
        // 每项必须具有唯一序号、正位宽和遥测代号。
        for (PdxpProtocolField item : result) {
            Integer index = item.getTableIndex();
            Integer width = item.getBitWidth();
            if (item.getId() == null || index == null || index < 0
                    || !indexes.add(index) || width == null || width <= 0
                    || !text(item.getTelemetryCode())) {
                throw new IllegalArgumentException("协议字段配置无效，序号：" + item.getTableIndex());
            }
        }
        result.sort(Comparator.comparingInt(
                PdxpProtocolField::getTableIndex));
        return result;
    }

    /** 设置报警状态；报警时完全不写状态索引。 */
    private void state(Telemetry.Builder telemetry, PdxpProtocolField field, BigInteger value) {
        if (!"1".equals(trim(field.getAlarmFlag()))) {
            return;
        }
        // 预警范围未配置或使用横线占位时，继续回退到正常值范围。
        String ranges = configuredRange(field.getWarningValue())
                ? field.getWarningValue() : field.getNormalValue();
        StateMatch match = stateMatch(ranges, new BigDecimal(value));
        if (match == null) {
            telemetry.setStateType(StateType.STATE_TYPE_ALARM);
        } else {
            telemetry.setStateType(StateType.STATE_TYPE_NORMAL)
                    .setStateIndex(match.index)
                    .setStateName(match.name);
        }
    }

    /** 查找包含当前值的首个闭区间及其中文状态名称。 */
    private StateMatch stateMatch(String ranges, BigDecimal value) {
        // 空值和常用横线占位符都表示没有配置状态范围。
        if (!configuredRange(ranges)) {
            return null;
        }
        String[] states = ranges.split("/", -1);
        // 状态索引按照配置顺序从零开始。
        for (int index = 0; index < states.length; index++) {
            Matcher matcher = RANGE.matcher(states[index]);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("状态范围格式不正确：" + states[index]);
            }
            BigDecimal lower = decimal(matcher.group(2), states[index]);
            BigDecimal upper = decimal(matcher.group(3), states[index]);
            if (lower.compareTo(upper) > 0) {
                throw new IllegalArgumentException("状态范围下限不能大于上限：" + states[index]);
            }
            if (value.compareTo(lower) >= 0 && value.compareTo(upper) <= 0) {
                return new StateMatch(index, matcher.group(1).trim());
            }
        }
        return null;
    }

    /** 将小端原码转换为无符号整数。 */
    private BigInteger unsigned(byte[] raw) {
        byte[] reversed = new byte[raw.length];
        // 反转副本供大整数读取，消息中的原码保持原顺序。
        for (int index = 0; index < raw.length; index++) {
            reversed[raw.length - index - 1] = raw[index];
        }
        return new BigInteger(1, reversed);
    }

    /** 查询并拆分当前帧对应的设备工作表名称。 */
    private FrameIdentity frameIdentity(
            CollectInterfaceRuntimeConfig config,
            List<PdxpProtocolField> fields) {
        String taskId = config.getTaskId();
        Long ruleId = fields.get(0).getId();
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("采集接口未配置试验任务编号");
        }
        String cacheKey = taskId + "\u0000" + ruleId;
        return identityCache.computeIfAbsent(
                cacheKey, ignored -> loadFrameIdentity(taskId, ruleId));
    }

    /** 从参数解析配置关联的设备名称创建帧标识。 */
    private FrameIdentity loadFrameIdentity(String taskId, Long ruleId) {
        PdxpFrameSource source = mappingMapper.findFrameSourceByRuleId(
                taskId, ruleId);
        if (source == null || !text(source.getCode())) {
            throw new IllegalArgumentException(
                    "参数解析配置未关联有效的设备卫星编码，配置主键：" + ruleId);
        }
        String deviceName = trim(source.getName());
        int separator = deviceName.indexOf('_');
        if (separator <= 0 || separator == deviceName.length() - 1) {
            throw new IllegalArgumentException(
                    "设备名称必须使用“通道名称_业务标识”格式：" + deviceName);
        }
        return new FrameIdentity(
                source.getCode().trim(),
                deviceName.substring(0, separator).trim(),
                deviceName.substring(separator + 1).trim());
    }

    /** 解析状态边界。 */
    private BigDecimal decimal(String value, String range) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("状态范围数值不正确：" + range);
        }
    }

    /** 判断字符串具有内容。 */
    private boolean text(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** 判断状态范围不是空值或横线占位符。 */
    private boolean configuredRange(String value) {
        if (!text(value)) {
            return false;
        }
        String normalized = value.trim();
        // 只判断整个字段，范围内的负数符号仍按正常数值解析。
        return !"-".equals(normalized)
                && !"—".equals(normalized)
                && !"–".equals(normalized);
    }

    /** 清理可空字符串。 */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** 保存设备名称拆分得到的通道名称和业务标识。 */
    private static final class FrameIdentity {
        /** 设备卫星编码。 */
        private final String satelliteCode;
        /** 通道名称。 */
        private final String channelName;
        /** 业务标识。 */
        private final String businessId;

        /** 保存已经校验通过的名称组成部分。 */
        private FrameIdentity(
                String satelliteCode,
                String channelName,
                String businessId) {
            if (satelliteCode.isEmpty() || channelName.isEmpty()
                    || businessId.isEmpty()) {
                throw new IllegalArgumentException("设备名称中的通道名称和业务标识不能为空");
            }
            this.satelliteCode = satelliteCode;
            this.channelName = channelName;
            this.businessId = businessId;
        }
    }

    /** 保存命中的状态序号和中文名称。 */
    private static final class StateMatch {
        /** 状态序号。 */
        private final int index;
        /** 状态中文名称。 */
        private final String name;

        /** 保存命中的状态信息。 */
        private StateMatch(int index, String name) {
            this.index = index;
            this.name = name;
        }
    }
}
