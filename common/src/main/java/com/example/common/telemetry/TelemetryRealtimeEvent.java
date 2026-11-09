package com.example.common.telemetry;

import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import java.util.List;

/** 一次解析结果，整帧与参数共用同一来源及处理时间。 */
public final class TelemetryRealtimeEvent {
    private final TelemetryFrameRecord frame;
    private final List<TelemetryParameterRecord> parameters;

    public TelemetryRealtimeEvent(TelemetryFrameRecord frame, List<TelemetryParameterRecord> parameters) {
        this.frame = frame;
        this.parameters = parameters;
    }

    public TelemetryFrameRecord getFrame() {
        return frame;
    }

    public List<TelemetryParameterRecord> getParameters() {
        return parameters;
    }
}
