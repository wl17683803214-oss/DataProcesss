package com.example.dataprocess.service;

import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import com.example.common.telemetry.TelemetryRealtimeEvent;

/** 缓冲和传输共用保守的字节估算，避免为了统计再次序列化完整记录。 */
final class GreptimeRecordSizer {
    private GreptimeRecordSizer() {
        // 仅提供静态计算，不维护额外数据副本。
    }

    /** 估算存储记录、集合引用和合并上下文，实际堆占用还受对象布局影响。 */
    static long event(TelemetryRealtimeEvent event) {
        TelemetryFrameRecord frame = event.getFrame();
        long bytes = 512L + 64L + 10 * 16L + text(frame.getTaskId()) + text(frame.getSourceType())
                + text(frame.getSourceIdentity()) + text(frame.getTelemetryType())
                + text(frame.getDeviceSatelliteName()) + text(frame.getChannelName())
                + text(frame.getCheckStatus()) + binary(frame.getRaw());
        for (TelemetryParameterRecord row : event.getParameters()) {
            // 为参数对象、列偏移、空值位图和合并列表保留固定预算。
            bytes += 64L + 15 * 16L + text(row.getTaskId()) + text(row.getSourceType())
                    + text(row.getSourceIdentity()) + text(row.getTelemetryType()) + text(row.getTelemetryCode())
                    + text(row.getTelemetryName()) + text(row.getValueType()) + text(row.getValueText())
                    + text(row.getStateName()) + text(row.getStateType()) + binary(row.getRaw());
        }
        return bytes;
    }

    /** 传输行按同样的字段长度计算，不分配中间字符串或字节数组。 */
    static long values(Object[] row) {
        long bytes = 64L + row.length * 16L;
        for (Object value : row) {
            if (value instanceof String) {
                bytes += text((String) value);
            } else if (value instanceof byte[]) {
                bytes += binary((byte[]) value);
            }
        }
        return bytes;
    }

    /** 每个字符最多按三个编码字节计算，同时覆盖中文和代理对。 */
    private static long text(String value) {
        return value == null ? 0L : value.length() * 3L;
    }

    /** 原码按二进制长度计入额度。 */
    private static long binary(byte[] value) {
        return value == null ? 0L : value.length;
    }
}
