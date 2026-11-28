package com.example.common.greptime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DailyTelemetryStatisticsTest {
    @Test
    void mergesPopulationVarianceUsingSampleCounts() {
        DailyTelemetryStatistics first = row(2, 1, 3, 2, 1);
        DailyTelemetryStatistics second = row(1, 5, 5, 5, 0);
        first.merge(second);
        assertEquals(3L, first.sampleCount);
        assertEquals(1.0, first.minValue, 0.00001);
        assertEquals(5.0, first.maxValue, 0.00001);
        assertEquals(3.0, first.avgValue, 0.00001);
        assertEquals(8.0 / 3.0, first.variance, 0.00001);
    }

    @Test
    void distinctStatesHaveDistinctKeys() {
        DailyTelemetryStatistics first = row(1, 1, 1, 1, 0);
        DailyTelemetryStatistics second = row(1, 1, 1, 1, 0);
        first.telemetryStatus = "A";
        second.telemetryStatus = "B";
        assertNotEquals(first.key(), second.key());
    }

    private DailyTelemetryStatistics row(long count, double min, double max, double avg, double variance) {
        DailyTelemetryStatistics row = new DailyTelemetryStatistics();
        row.sampleCount = count;
        row.minValue = min;
        row.maxValue = max;
        row.avgValue = avg;
        row.variance = variance;
        return row;
    }
}

