package com.example.common.greptime;

import java.util.Arrays;
import java.util.List;

/** 每个日期、任务、卫星、遥测代号及状态的聚合结果。 */
public class DailyTelemetryStatistics {
    public String taskId;
    public String satelliteCode;
    public String telemetryCode;
    public String telemetryName;
    public String status;
    public Double maxValue;
    public Double minValue;
    public Double avgValue;
    public Double variance;
    /** 仅供跨分表合并，不写入统计表。 */
    public long sampleCount;

    List<String> key() {
        return Arrays.asList(taskId, satelliteCode, telemetryCode, status);
    }

    /** 按实际样本数合并，不能直接平均各分表的均值或方差。 */
    void merge(DailyTelemetryStatistics other) {
        if (other.telemetryName != null && (telemetryName == null
                || other.telemetryName.compareTo(telemetryName) > 0)) {
            telemetryName = other.telemetryName;
        }
        if (other.sampleCount == 0) {
            return;
        }
        if (sampleCount == 0) {
            maxValue = other.maxValue;
            minValue = other.minValue;
            avgValue = other.avgValue;
            variance = other.variance;
            sampleCount = other.sampleCount;
            return;
        }
        long total = Math.addExact(sampleCount, other.sampleCount);
        double leftWeight = (double) sampleCount / total;
        double rightWeight = (double) other.sampleCount / total;
        double delta = other.avgValue - avgValue;
        variance = leftWeight * (variance == null ? 0 : variance)
                + rightWeight * (other.variance == null ? 0 : other.variance)
                + leftWeight * rightWeight * delta * delta;
        avgValue += delta * rightWeight;
        maxValue = Math.max(maxValue, other.maxValue);
        minValue = Math.min(minValue, other.minValue);
        sampleCount = total;
    }
}
