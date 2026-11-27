package com.example.dataprocess.collection;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** 保存一个任务的累计完成帧数和最近五秒的时间边界。 */
final class ProcessingRateWindow {

    /** 按一百毫秒保存累计值，滚动时只使用已经结束的时间片。 */
    private static final long SLOT_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final int WINDOW_SLOTS = (int) (WINDOW_NANOS / SLOT_NANOS);
    private static final double NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    /** 接口开始计数时建立任务的采样时间起点。 */
    private final long startedNanos = System.nanoTime();
    /** 多保留一个位置，给当前尚未结束的时间片使用。 */
    private final long[] slotNumbers = new long[WINDOW_SLOTS + 1];
    private final long[] countsBeforeSlot = new long[slotNumbers.length];
    private long completedFrames;

    ProcessingRateWindow() {
        Arrays.fill(slotNumbers, -1L);
    }

    /** 在对应时间片第一次收到完成帧时保存片首累计数，再累加帧数。 */
    synchronized void recordFrame() {
        long slotNumber = (System.nanoTime() - startedNanos) / SLOT_NANOS;
        int index = (int) (slotNumber % slotNumbers.length);
        if (slotNumbers[index] != slotNumber) {
            slotNumbers[index] = slotNumber;
            countsBeforeSlot[index] = completedFrames;
        }
        completedFrames++;
    }

    /** 启动时使用已有时长，满五秒后使用最近五秒的累计量差值。 */
    synchronized double framesPerSecond() {
        long elapsedNanos = System.nanoTime() - startedNanos;
        if (elapsedNanos <= 0) {
            return 0D;
        }
        if (elapsedNanos < WINDOW_NANOS) {
            return completedFrames * NANOS_PER_SECOND / elapsedNanos;
        }

        // 最近一个完整时间片的结束边界也是当前片的起点。
        long endSlot = elapsedNanos / SLOT_NANOS;
        int endIndex = (int) (endSlot % slotNumbers.length);
        long windowEndCount = slotNumbers[endIndex] == endSlot
                ? countsBeforeSlot[endIndex] : completedFrames;
        long firstSlot = endSlot - WINDOW_SLOTS;
        long windowStartCount = windowEndCount;
        // 找到窗口内第一帧之前的累计量，空窗口自然返回零。
        for (int index = 0; index < slotNumbers.length; index++) {
            long slotNumber = slotNumbers[index];
            if (slotNumber >= firstSlot && slotNumber < endSlot) {
                windowStartCount = Math.min(windowStartCount, countsBeforeSlot[index]);
            }
        }
        return (windowEndCount - windowStartCount) * NANOS_PER_SECOND / WINDOW_NANOS;
    }
}
