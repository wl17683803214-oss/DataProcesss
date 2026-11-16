package com.example.dataprocess.service;

import com.example.common.greptime.GreptimeDataWriter;
import com.example.common.greptime.GreptimeProperties;
import com.example.common.greptime.GreptimeTableRoute;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.dataprocess.config.GreptimeBulkProperties;
import io.greptime.BulkStreamWriter;
import io.greptime.BulkWrite;
import io.greptime.GreptimeDB;
import io.greptime.models.AuthInfo;
import io.greptime.models.DataType;
import io.greptime.models.Table;
import io.greptime.models.TableSchema;
import io.greptime.options.GreptimeOptions;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 在已有入库线程执行列式批量写入，按表复用流，采集线程不访问网络。 */
public class GreptimeBulkWriter implements GreptimeDataWriter, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreptimeBulkWriter.class);
    /** 仅持有有界数量的表结构和写入流，按最近使用顺序淘汰。 */
    private final Map<String, StreamEntry> streams = new LinkedHashMap<>(16, 0.75f, true);
    private final GreptimeProperties storage;
    private final GreptimeBulkProperties properties;
    private final GreptimeDB client;
    private final BulkWrite.Config streamConfig;
    /** 每个通道同一时刻只处理一个合并组，同一对象也能复用多路独立写入流。 */
    private final ArrayBlockingQueue<Integer> lanes;
    private volatile boolean closed;

    /** 关闭存储时不创建客户端；认证和数据库名与现有查询连接一致。 */
    public GreptimeBulkWriter(GreptimeProperties storage, GreptimeBulkProperties properties) {
        storage.validate();
        properties.validate();
        this.storage = storage;
        this.properties = properties;
        this.lanes = new ArrayBlockingQueue<>(properties.getMaxConcurrentWrites(), true);
        for (int index = 0; index < properties.getMaxConcurrentWrites(); index++) {
            lanes.add(index);
        }
        this.streamConfig = BulkWrite.Config.newBuilder()
                .allocatorInitReservation(0)
                .allocatorMaxAllocation(properties.getAllocatorMaxBytes())
                .timeoutMsPerMessage(properties.getTimeoutMillis())
                .maxRequestsInFlight(1)
                .build();
        // 第一步：不重复声明数据库配置，从查询地址复用库名和默认主机。
        URI address = URI.create(storage.getUrl().substring("jdbc:".length()));
        String[] endpoints = properties.getEndpoints();
        if (endpoints.length == 0) {
            String host = address.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("无法从查询连接推导批量写入主机，请配置写入地址");
            }
            endpoints = new String[] {(host.indexOf(':') >= 0 && !host.startsWith("[")
                    ? "[" + host + "]" : host) + ":4001"};
        }
        // 第二步：只使用官方客户端，不把认证信息写入业务日志。
        AuthInfo auth = storage.getUsername() == null || storage.getUsername().isEmpty()
                ? AuthInfo.noAuthorization() : new AuthInfo(storage.getUsername(), storage.getPassword());
        client = storage.isEnabled() ? GreptimeDB.create(GreptimeOptions.newBuilder(endpoints,
                address.getPath().substring(1)).authInfo(auth).writeMaxRetries(0).build()) : null;
    }

    /** 参数与整帧分别确认，重试由公共存储组件使用固定主键和时间执行。 */
    @Override
    public void write(GreptimeTableRoute route, List<TelemetryFrameRecord> frames,
            List<TelemetryParameterRecord> parameters) {
        Integer lane = null;
        try {
            // 第一步：限制同时组装的任务数，不新增无界缓冲或后台任务队列。
            if (closed || client == null) {
                throw new IllegalStateException("遥测批量写入组件已关闭或未启用");
            }
            lane = lanes.poll(storage.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (lane == null) {
                throw new IllegalStateException("遥测批量写入并发额度等待超时");
            }
            // 第二步：多帧参数作为连续批次写入，超过行数或字节额度时在行边界拆分。
            if (!parameters.isEmpty()) {
                writeRows(route.parameterTable(), true, parameters.size(),
                        index -> parameterValues(parameters.get(index)), lane);
            }
            // 第三步：参数全部确认后批量写入各帧，保留每帧时间和正常、异常状态。
            writeRows(route.frameTable(), false, frames.size(), index -> frameValues(frames.get(index)), lane);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("遥测批量写入等待被中断", exception);
        } finally {
            // 所有失败路径均释放额度，由既有异步入库链路记录错误与告警。
            if (lane != null) {
                lanes.add(lane);
            }
        }
    }

    /** 同一表独占一个列缓冲，等待确认后才能复用，避免并发修改或提前清空。 */
    private void writeRows(String tableName, boolean parameterTable, int count, IntFunction<Object[]> rows, int lane)
            throws InterruptedException {
        StreamEntry entry = lease(tableName, parameterTable, lane);
        boolean locked = false;
        try {
            // 第一步：租用计数保护流不被淘汰，表锁只阻塞后台入库线程。
            locked = entry.lock.tryLock(storage.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new IllegalStateException("遥测分表写入流等待超时，表名：" + tableName);
            }
            if (closed) {
                throw new IllegalStateException("遥测批量写入组件正在关闭");
            }
            if (entry.writer == null) {
                entry.writer = client.bulkStreamWriter(entry.schema, streamConfig);
            }
            // 第二步：逐行估算批次大小，只保留当前行和SDK内部的有界列缓冲。
            int offset = 0;
            Object[] pending = rows.apply(offset);
            while (offset < count) {
                Table.TableBufferRoot table = entry.writer.tableBufferRoot(
                        Math.min(1024, properties.getMaxBatchRows()));
                int batchRows = 0;
                long batchBytes = 0L;
                while (offset < count && batchRows < properties.getMaxBatchRows()) {
                    long rowBytes = GreptimeRecordSizer.values(pending);
                    if (rowBytes > properties.getMaxBatchBytes()) {
                        throw new IllegalArgumentException("遥测单条记录超过批次字节额度，表名：" + tableName);
                    }
                    if (batchRows > 0 && batchBytes + rowBytes > properties.getMaxBatchBytes()) {
                        break;
                    }
                    table.addRow(pending);
                    batchRows++;
                    batchBytes += rowBytes;
                    offset++;
                    pending = offset < count ? rows.apply(offset) : null;
                }
                // 第三步：只有服务端确认行数完整，当前批次才算写入成功。
                table.complete();
                int written = entry.writer.writeNext().get(properties.getTimeoutMillis(), TimeUnit.MILLISECONDS);
                if (written != batchRows) {
                    throw new IllegalStateException("遥测批量写入确认行数不一致，表名：" + tableName
                            + "，发送：" + batchRows + "，确认：" + written);
                }
            }
        } catch (InterruptedException exception) {
            if (locked) {
                discard(entry, exception);
            }
            throw exception;
        } catch (Exception exception) {
            // 失败后流和缓冲均不能用于重放，下一次调用从原始记录重新构造。
            if (locked) {
                discard(entry, exception);
            }
            throw new IllegalStateException("遥测列式批量写入失败，表名：" + tableName, exception);
        } finally {
            if (locked) {
                entry.lock.unlock();
            }
            synchronized (streams) {
                entry.users--;
            }
        }
    }

    /** 全局缓存锁仅维护租用计数，不在锁内访问网络或等待服务端。 */
    private StreamEntry lease(String tableName, boolean parameterTable, int lane) {
        StreamEntry retired = null;
        StreamEntry entry;
        synchronized (streams) {
            if (closed) {
                throw new IllegalStateException("遥测批量写入组件正在关闭");
            }
            // 同一通道顺序复用缓冲，不同通道对同一张表也可并行写入。
            String cacheKey = GreptimeTableRoute.requireTable(tableName) + "#" + lane;
            entry = streams.get(cacheKey);
            if (entry == null) {
                // 优先淘汰最久未使用且没有调用方持有的表流。
                if (streams.size() >= properties.getMaxCachedStreams()) {
                    Iterator<StreamEntry> iterator = streams.values().iterator();
                    while (iterator.hasNext()) {
                        StreamEntry candidate = iterator.next();
                        if (candidate.users == 0) {
                            retired = candidate;
                            iterator.remove();
                            break;
                        }
                    }
                    if (retired == null) {
                        throw new IllegalStateException("遥测写入流缓存已满且全部正在使用");
                    }
                }
                entry = new StreamEntry(schema(tableName, parameterTable));
                streams.put(cacheKey, entry);
            }
            entry.users++;
        }
        // 淘汰发生在全局锁外，已确认的数据不重新发送。
        if (retired != null) {
            finish(retired);
        }
        return entry;
    }

    /** 按数据库既有列顺序声明语义类型，时间索引和普通时间字段分别处理。 */
    private TableSchema schema(String tableName, boolean parameterTable) {
        TableSchema.Builder schema = TableSchema.newBuilder(tableName)
                .addTimestamp("source_time", DataType.TimestampMillisecond)
                .addTag("task_id", DataType.String);
        if (!parameterTable) {
            schema.addTag("interface_id", DataType.Int64);
        }
        schema.addTag("source_type", DataType.String)
                .addTag("source_identity", DataType.String)
                .addTag("telemetry_type", DataType.String);
        if (parameterTable) {
            schema.addTag("telemetry_code", DataType.String)
                    .addField("telemetry_name", DataType.String)
                    .addField("value_type", DataType.String)
                    .addField("value", DataType.Float64)
                    .addField("value_text", DataType.String)
                    .addField("raw", DataType.Binary)
                    .addField("state_name", DataType.String)
                    .addField("state_type", DataType.String)
                    .addField("state_index", DataType.Int32)
                    .addField("processed_at", DataType.TimestampMillisecond);
        } else {
            schema.addField("device_satellite_name", DataType.String)
                    .addField("channel_name", DataType.String)
                    .addField("raw", DataType.Binary)
                    .addField("check_status", DataType.String);
        }
        return schema.build();
    }

    /** 所有编码和空值沿用原存储记录，不改变查询和前端的字段含义。 */
    private Object[] parameterValues(TelemetryParameterRecord row) {
        return new Object[] {millis(row.getSourceTime()), row.getTaskId(), row.getSourceType(),
                row.getSourceIdentity(), row.getTelemetryType(), row.getTelemetryCode(), row.getTelemetryName(),
                row.getValueType(), row.getValue(), row.getValueText(), row.getRaw(), row.getStateName(),
                row.getStateType(), row.getStateIndex(), millis(row.getProcessedAt())};
    }

    /** 原码直接以二进制传输，不转成十六进制或文本。 */
    private Object[] frameValues(TelemetryFrameRecord row) {
        return new Object[] {millis(row.getSourceTime()), row.getTaskId(), row.getInterfaceId(),
                row.getSourceType(), row.getSourceIdentity(), row.getTelemetryType(), row.getDeviceSatelliteName(),
                row.getChannelName(), row.getRaw(), row.getCheckStatus()};
    }

    /** 以毫秒纪元传输时间，避免传输协议引入时区偏移。 */
    private Long millis(Timestamp value) {
        return value == null ? null : value.getTime();
    }

    /** 异常流只释放资源，不能完成或复用已失败的传输。 */
    private void discard(StreamEntry entry, Exception failure) {
        BulkStreamWriter current = entry.writer;
        entry.writer = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    /** 正常结束已无在途数据的流，并检查服务端结束响应。 */
    private void finish(StreamEntry entry) {
        BulkStreamWriter current = entry.writer;
        entry.writer = null;
        if (current != null) {
            try {
                current.completed();
            } catch (Exception exception) {
                LOGGER.warn("遥测批量写入流结束失败，已确认批次不重复发送，表名：{}",
                        entry.schema.getTableName(), exception);
            } finally {
                try {
                    current.close();
                } catch (Exception exception) {
                    LOGGER.warn("遥测批量写入流资源释放失败，表名：{}", entry.schema.getTableName(), exception);
                }
            }
        }
    }

    /** 依赖方的入库线程池先结束，再按表关闭流并释放客户端。 */
    @Override
    public void close() {
        List<StreamEntry> closing;
        synchronized (streams) {
            if (closed) {
                return;
            }
            closed = true;
            closing = new ArrayList<>(streams.values());
            streams.clear();
        }
        // 表锁保护最后一次写入，防止服务关闭和缓冲区组装同时进行。
        for (StreamEntry entry : closing) {
            entry.lock.lock();
            try {
                finish(entry);
            } finally {
                entry.lock.unlock();
            }
        }
        if (client != null) {
            client.shutdownGracefully();
        }
    }

    /** 表结构不可变；租用计数由全局锁保护，缓冲和流由表锁保护。 */
    private static final class StreamEntry {
        private final TableSchema schema;
        private final ReentrantLock lock = new ReentrantLock(true);
        private BulkStreamWriter writer;
        private int users;

        private StreamEntry(TableSchema schema) {
            this.schema = schema;
        }
    }
}
