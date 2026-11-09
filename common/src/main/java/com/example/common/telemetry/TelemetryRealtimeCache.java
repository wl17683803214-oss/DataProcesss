package com.example.common.telemetry;

import com.example.common.enums.TelemetryFrameStatus;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StreamUtils;

/** 两个业务服务共用的实时快照，读操作不删除数据，也不回查时序库。 */
public class TelemetryRealtimeCache {
    /** 每个接口、每种帧状态最多返回的条数。 */
    public static final int FRAME_LIMIT = 100;
    /** 单次脚本最多更新的参数数量，限制Redis连续执行脚本的时间。 */
    private static final int PARAMETER_BATCH_SIZE = 200;
    /** 单次脚本的序列化内容上限，超大字段不进入实时展示缓存。 */
    private static final int SCRIPT_PAYLOAD_LIMIT = 262144;
    /** 沿用项目的Redis连接和字符串序列化。 */
    private final StringRedisTemplate redis;
    /** 读写双方共用的配置。 */
    private final TelemetryRealtimeProperties properties;
    /** 缓存使用固定的毫秒时间序列化，避免受HTTP日期格式设置影响。 */
    private final ObjectMapper mapper = new ObjectMapper();
    /** 比较时间并更新参数的原子脚本。 */
    private final byte[] latestScript;
    /** 追加并裁剪整帧快照的原子脚本。 */
    private final byte[] frameScript;

    /** 仅加载本地脚本，构造过程不访问Redis。 */
    public TelemetryRealtimeCache(StringRedisTemplate redis, TelemetryRealtimeProperties properties) throws IOException {
        // 启动时检查容量配置并读取公共脚本，避免工作线程反复加载资源。
        properties.validate();
        this.redis = redis;
        this.properties = properties;
        latestScript = readScript("telemetry/latest-snapshot.lua");
        frameScript = readScript("telemetry/frame-snapshot.lua");
    }

    /** 工作线程调用：合并重复参数，再通过流水线批量写入快照。 */
    public void writeSnapshots(Map<TelemetryFrameRecord, List<TelemetryParameterRecord>> batches)
            throws JsonProcessingException {
        if (!properties.isEnabled() || batches.isEmpty()) {
            return;
        }
        // 第一步：只在后台合并同参数的最新值，不在处理或入库线程序列化。
        Map<String, Map<String, TelemetryParameterRecord>> parameters = new LinkedHashMap<>();
        Map<String, List<TelemetryFrameRecord>> frames = new LinkedHashMap<>();
        long expiresAt = System.currentTimeMillis() + retentionMillis();
        for (Map.Entry<TelemetryFrameRecord, List<TelemetryParameterRecord>> batch : batches.entrySet()) {
            TelemetryFrameRecord frame = batch.getKey();
            String typedKey = parameterKey(frame.getTaskId(), frame.getSourceType(),
                    frame.getSourceIdentity(), frame.getTelemetryType());
            String allTypesKey = parameterKey(frame.getTaskId(), frame.getSourceType(), frame.getSourceIdentity(), null);
            for (TelemetryParameterRecord row : batch.getValue()) {
                merge(parameters, typedKey, row);
                merge(parameters, allTypesKey, row);
            }
            // 第二步：每条帧只写入所属任务、接口与状态，设置独立的到期时间。
            frame.setExpiresAt(expiresAt);
            frames.computeIfAbsent(frameKey(frame.getTaskId(), frame.getCheckStatus(), frame.getInterfaceId()),
                    ignored -> new ArrayList<>()).add(frame);
        }
        // 第三步：预先生成有界命令，序列化失败时不打开流水线连接。
        List<CacheCommand> commands = new ArrayList<>();
        for (Map.Entry<String, Map<String, TelemetryParameterRecord>> group : parameters.entrySet()) {
            List<byte[]> keys = new ArrayList<>();
            List<byte[]> values = new ArrayList<>();
            int payload = 0;
            for (TelemetryParameterRecord row : group.getValue().values()) {
                byte[] key = bytes(group.getKey() + ":" + encode(row.getTelemetryCode()));
                byte[] value = bytes(row.getSourceTime().getTime() + "|" + parameterJson(row));
                if (key.length + value.length > SCRIPT_PAYLOAD_LIMIT) {
                    continue;
                }
                if (keys.size() >= PARAMETER_BATCH_SIZE || payload + key.length + value.length > SCRIPT_PAYLOAD_LIMIT) {
                    commands.add(parameterCommand(keys, values));
                    keys = new ArrayList<>();
                    values = new ArrayList<>();
                    payload = 0;
                }
                keys.add(key);
                values.add(value);
                payload += key.length + value.length;
            }
            if (!keys.isEmpty()) {
                commands.add(parameterCommand(keys, values));
            }
        }
        for (Map.Entry<String, List<TelemetryFrameRecord>> group : frames.entrySet()) {
            // 保留到达顺序，同一接口的脚本按完整记录字节数淘汰最旧的帧。
            List<byte[]> arguments = frameArguments(group.getKey(), 0);
            List<TelemetryFrameRecord> writtenFrames = new ArrayList<>();
            int payload = 0;
            for (TelemetryFrameRecord row : group.getValue()) {
                byte[] value = bytes(row.getExpiresAt() + "|" + mapper.writeValueAsString(row));
                if (value.length > SCRIPT_PAYLOAD_LIMIT || value.length > properties.getFrameCapacityBytes()) {
                    // 单条已超出预算时也不进入帧增量，参数点仍可独立推送。
                    row.setExpiresAt(0);
                    continue;
                }
                if (payload + value.length > SCRIPT_PAYLOAD_LIMIT && arguments.size() > 5) {
                    commands.add(new CacheCommand(frameScript, 2, arguments, writtenFrames));
                    arguments = frameArguments(group.getKey(), 0);
                    writtenFrames = new ArrayList<>();
                    payload = 0;
                }
                arguments.add(value);
                writtenFrames.add(row);
                payload += value.length;
            }
            if (arguments.size() > 5) {
                commands.add(new CacheCommand(frameScript, 2, arguments, writtenFrames));
            }
        }
        // 第四步：限制每轮流水线长度；每条脚本的键位于同一Redis集群槽位。
        for (int offset = 0; offset < commands.size(); offset += 64) {
            final List<CacheCommand> chunk = commands.subList(offset, Math.min(offset + 64, commands.size()));
            List<Object> replies = redis.executePipelined((RedisCallback<Object>) connection -> {
                for (CacheCommand command : chunk) {
                    ReturnType type = command.frames == null ? ReturnType.INTEGER : ReturnType.MULTI;
                    connection.eval(command.script, type, command.keyCount, command.arguments);
                }
                return null;
            });
            // 只有实际写入缓存的帧才进入增量，并携带Redis分配的顺序及计费大小。
            for (int index = 0; index < chunk.size(); index++) {
                CacheCommand command = chunk.get(index);
                if (command.frames != null) {
                    List<?> assigned = (List<?>) replies.get(index);
                    for (int frameIndex = 0; frameIndex < command.frames.size(); frameIndex++) {
                        TelemetryFrameRecord row = command.frames.get(frameIndex);
                        long order = ((Number) assigned.get(frameIndex * 2)).longValue();
                        row.setRealtimeOrder(order);
                        row.setRealtimeBytes(((Number) assigned.get(frameIndex * 2 + 1)).intValue());
                        if (order == 0) {
                            row.setExpiresAt(0);
                        }
                    }
                }
            }
        }
    }

    /** 按勾选参数批量读取最新值，不扫描整个对象的参数。 */
    public List<TelemetryParameterRecord> latest(String taskId, List<Map<String, Object>> filters,
            String telemetryType) throws IOException {
        if (!properties.isEnabled() || filters.isEmpty()) {
            return Collections.emptyList();
        }
        // 按勾选列表生成精确的参数键，每个键的过期时间相互独立。
        List<byte[]> keys = new ArrayList<>(filters.size());
        for (Map<String, Object> filter : filters) {
            String key = parameterKey(taskId, (String) filter.get("sourceType"),
                    (String) filter.get("sourceIdentity"), telemetryType);
            keys.add(bytes(key + ":" + encode((String) filter.get("telemetryCode"))));
        }
        List<TelemetryParameterRecord> result = new ArrayList<>();
        for (int offset = 0; offset < keys.size(); offset += PARAMETER_BATCH_SIZE) {
            List<byte[]> batch = keys.subList(offset, Math.min(offset + PARAMETER_BATCH_SIZE, keys.size()));
            // 每轮使用有限个读取命令，兼容同次查询跨对象及Redis槽位。
            List<Object> replies = redis.executePipelined((RedisCallback<Object>) connection -> {
                for (byte[] key : batch) {
                    connection.get(key);
                }
                return null;
            });
            for (Object value : replies) {
                if (value != null) {
                    String envelope = (String) value;
                    result.add(mapper.readValue(envelope.substring(envelope.indexOf('|') + 1),
                            TelemetryParameterRecord.class));
                }
            }
        }
        return result;
    }

    /** 只读指定接口的最新帧，清理到期帧但不延长缓存寿命。 */
    public List<TelemetryFrameRecord> frames(String taskId, Long interfaceId, TelemetryFrameStatus status)
            throws IOException {
        // 缺少任务或接口时拒绝查询，不访问任务级汇总键。
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        if (interfaceId == null || interfaceId <= 0) {
            throw new IllegalArgumentException("采集接口主键不能为空且必须大于零");
        }
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }
        List<byte[]> arguments = frameArguments(frameKey(taskId.trim(), status.getValue(), interfaceId), FRAME_LIMIT);
        List<byte[]> values = redis.execute((RedisCallback<List<byte[]>>) connection ->
                connection.eval(frameScript, ReturnType.MULTI, 2, arguments.toArray(new byte[0][])));
        List<TelemetryFrameRecord> result = new ArrayList<>();
        if (values != null) {
            for (byte[] value : values) {
                // 记录前缀包含过期时间和Redis写入顺序，完整字节数用于前端裁剪。
                String record = new String(value, StandardCharsets.UTF_8);
                int separator = record.indexOf('|');
                int orderEnd = record.indexOf('|', separator + 1);
                TelemetryFrameRecord row = mapper.readValue(record.substring(orderEnd + 1), TelemetryFrameRecord.class);
                row.setRealtimeOrder(Long.parseLong(record.substring(separator + 1, orderEnd)));
                row.setRealtimeBytes(value.length);
                result.add(row);
            }
        }
        return result;
    }

    /** 合并时比较真实时间，不能由入库完成顺序决定最新值。 */
    private void merge(Map<String, Map<String, TelemetryParameterRecord>> groups, String key,
            TelemetryParameterRecord row) {
        Map<String, TelemetryParameterRecord> rows = groups.computeIfAbsent(key, ignored -> new HashMap<>());
        TelemetryParameterRecord previous = rows.get(row.getTelemetryCode());
        if (previous == null || !row.getSourceTime().before(previous.getSourceTime())) {
            rows.put(row.getTelemetryCode(), row);
        }
    }

    /** 参数快照省略原码和页面不读取的字段，避免复制大块二进制数据。 */
    private String parameterJson(TelemetryParameterRecord row) {
        ObjectNode value = mapper.createObjectNode();
        value.put("sourceTime", row.getSourceTime().getTime());
        value.put("taskId", row.getTaskId());
        value.put("sourceType", row.getSourceType());
        value.put("sourceIdentity", row.getSourceIdentity());
        value.put("telemetryType", row.getTelemetryType());
        value.put("telemetryCode", row.getTelemetryCode());
        value.put("telemetryName", row.getTelemetryName());
        value.put("valueType", row.getValueType());
        value.put("value", row.getValue());
        value.put("valueText", row.getValueText());
        value.put("stateName", row.getStateName());
        if (row.getProcessedAt() != null) {
            value.put("processedAt", row.getProcessedAt().getTime());
        }
        return value.toString();
    }

    /** 各组成部分分别编码，避免任务、对象或遥测类型包含分隔符时串键。 */
    private String parameterKey(String taskId, String sourceType, String identity, String type) {
        return properties.getKeyPrefix() + "parameter:{" + encode(taskId) + "." + encode(sourceType)
                + "." + encode(identity) + "." + (type == null ? "all" : encode(type)) + "}";
    }

    /** 任务、接口与状态组成唯一缓存键，容量计数键与列表使用同一槽位。 */
    private String frameKey(String taskId, String status, Long interfaceId) {
        return properties.getKeyPrefix() + "frame:{" + encode(taskId) + "." + status + "." + interfaceId + "}";
    }

    /** 参数键依次传入脚本，随后是公共过期时间和每个键的记录。 */
    private CacheCommand parameterCommand(List<byte[]> keys, List<byte[]> values) {
        List<byte[]> arguments = new ArrayList<>(keys.size() * 2 + 1);
        arguments.addAll(keys);
        arguments.add(bytes(String.valueOf(retentionMillis())));
        arguments.addAll(values);
        return new CacheCommand(latestScript, keys.size(), arguments, null);
    }

    /** 读写复用同一个容量和到期脚本，查询数量为零时表示仅写入。 */
    private List<byte[]> frameArguments(String key, int readLimit) {
        List<byte[]> result = new ArrayList<>();
        result.add(bytes(key));
        result.add(bytes(key + ":bytes"));
        result.add(bytes(String.valueOf(properties.getFrameCapacityBytes())));
        result.add(bytes(String.valueOf(retentionMillis())));
        result.add(bytes(String.valueOf(readLimit)));
        return result;
    }

    private long retentionMillis() {
        return properties.getRetentionSeconds() * 1000L;
    }

    /** 使用UTF-8和无填充编码，保留原始大小写与特殊字符。 */
    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes(value));
    }

    /** Redis字符串统一使用UTF-8。 */
    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /** 读取脚本后关闭资源，支持普通目录和打包后的归档。 */
    private byte[] readScript(String path) throws IOException {
        try (java.io.InputStream input = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToByteArray(input);
        }
    }

    /** 已序列化的同槽原子写命令，仅由工作线程持有。 */
    private static final class CacheCommand {
        private final byte[] script;
        private final int keyCount;
        private final byte[][] arguments;
        private final List<TelemetryFrameRecord> frames;

        private CacheCommand(byte[] script, int keyCount, List<byte[]> arguments, List<TelemetryFrameRecord> frames) {
            this.script = script;
            this.keyCount = keyCount;
            this.arguments = arguments.toArray(new byte[0][]);
            this.frames = frames;
        }
    }
}
