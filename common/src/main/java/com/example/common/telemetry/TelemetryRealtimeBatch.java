package com.example.common.telemetry;

import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryParameterRecord;
import java.util.Collections;
import java.util.List;

/** Redis向各管理实例分发的实时批次。 */
public class TelemetryRealtimeBatch {
    /** 单次转发的最大字节数，防止大批占满消息和发送队列。 */
    public static final int MAX_BYTES = 262144;
    /** 追加到双方约定前缀后的转发频道。 */
    public static final String CHANNEL_SUFFIX = "events";
    /** 上游跳过数据时通知连接重新同步。 */
    private boolean gap;
    /** 完整帧按缓存写入顺序转发。 */
    private List<TelemetryFrameRecord> frames = Collections.emptyList();
    /** 保留各帧的参数点，曲线消费时不合并。 */
    private List<TelemetryParameterRecord> parameters = Collections.emptyList();

    public boolean isGap() { return gap; }
    public void setGap(boolean value) { gap = value; }
    public List<TelemetryFrameRecord> getFrames() { return frames; }
    public void setFrames(List<TelemetryFrameRecord> value) { frames = value; }
    public List<TelemetryParameterRecord> getParameters() { return parameters; }
    public void setParameters(List<TelemetryParameterRecord> value) { parameters = value; }
}
