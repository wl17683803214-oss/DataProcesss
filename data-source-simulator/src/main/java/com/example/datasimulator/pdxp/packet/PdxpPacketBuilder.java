package com.example.datasimulator.pdxp.packet;

import com.example.datasimulator.pdxp.config.PdxpSimulatorConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** 构造32字节PDXP固定包头。 */
public final class PdxpPacketBuilder {

    /** PDXP日期积日的起算日期。 */
    private static final LocalDate DATE_EPOCH = LocalDate.of(2000, 1, 1);
    /** 当前待发送的无符号四字节包序号。 */
    private long nextSequence;

    /** 从配置的十六进制NO字段读取包序号初始值。 */
    public PdxpPacketBuilder() {
        // 包序号配置为四字节无符号值，使用长整数保存。
        this.nextSequence = HexValueCodec.decodeUnsignedLong(
                PdxpSimulatorConfig.NO, 4, "NO");
    }

    /** 使用当前时间和指定数据域长度构造PDXP包头。 */
    public byte[] build(int dataLength) {
        // 第一步：PDXP长度字段为无符号两字节，先检查数据域边界。
        if (dataLength < 0 || dataLength > 0xFFFF) {
            throw new IllegalArgumentException("PDXP数据域长度超出两字节范围");
        }
        LocalDate sendDate = LocalDate.now();
        LocalTime sendTime = LocalTime.now();

        // 第二步：按固定偏移逐字段写入32字节包头。
        byte[] header = new byte[PdxpSimulatorConfig.PDXP_HEADER_LENGTH];
        copyConfiguredField(header, 0, PdxpSimulatorConfig.VER, 1, "VER");
        copyConfiguredField(header, 1, PdxpSimulatorConfig.MID, 2, "MID");
        copyConfiguredField(header, 3, PdxpSimulatorConfig.SID, 4, "SID");
        copyConfiguredField(header, 7, PdxpSimulatorConfig.DID, 4, "DID");
        copyConfiguredField(header, 11, PdxpSimulatorConfig.BID, 4, "BID");

        // 第三步：包序号使用配置初值并按小端写入，每构造一帧自然加一。
        ByteBuffer buffer = ByteBuffer.wrap(header)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(15, (int) nextSequence);
        nextSequence = (nextSequence + 1L) & 0xFFFFFFFFL;
        copyConfiguredField(header, 19, PdxpSimulatorConfig.FLAG, 1, "FLAG");

        // 第四步：偏移20到23的保留字段保持数组默认的四字节零值。
        long dateDays = ChronoUnit.DAYS.between(DATE_EPOCH, sendDate) + 1L;
        if (dateDays > 0xFFFFL) {
            throw new IllegalArgumentException("当前日期超出PDXP积日两字节范围");
        }
        buffer.putShort(24, (short) dateDays);

        // 第五步：当日时标按0.1毫秒计数写入四字节小端字段。
        long timeUnits = sendTime.toSecondOfDay() * 10000L
                + sendTime.getNano() / 100000L;
        buffer.putInt(26, (int) timeUnits);

        // 第六步：最后两字节写入本次实际数据域长度。
        buffer.putShort(30, (short) dataLength);
        return header;
    }

    /** 把用户填写的十六进制字段按小端复制到包头。 */
    private void copyConfiguredField(
            byte[] target,
            int offset,
            String hexValue,
            int expectedLength,
            String fieldName) {
        // 每个配置字段独立反转，不对完整PDXP包头整体反转。
        byte[] fieldData = HexValueCodec.decodeLittleEndian(
                hexValue, expectedLength, fieldName);
        System.arraycopy(fieldData, 0, target, offset, fieldData.length);
    }
}
