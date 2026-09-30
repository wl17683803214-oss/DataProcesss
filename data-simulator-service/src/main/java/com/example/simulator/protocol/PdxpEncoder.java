package com.example.simulator.protocol;

import com.example.simulator.model.PdxpConfig;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** 每次执行创建一个编码器，包序号不会在模拟源之间共享。 */
public class PdxpEncoder {
    private final PdxpConfig config;
    private long sequence;

    public PdxpEncoder(PdxpConfig config) {
        // 先验证完整配置，再初始化本实例的无符号包序号。
        validate(config);
        this.config = config;
        sequence = Long.parseLong(config.initialNo, 16);
    }

    /** 保存配置和执行前共用同一套十六进制校验。 */
    public static void validate(PdxpConfig config) {
        // 先限制各段及总长度，再解析文本，保证一个报文可以放入UDP载荷。
        requireLength(config.inputFrameLength, 1, 65475, "输入帧长度");
        requireLength(config.transportHeaderLength, 0, 65475, "传输头长度");
        requireLength(config.telemetryHeaderLength, 0, config.inputFrameLength, "原帧头长度");
        requireLength(config.customDataLength, 0, 65475, "自定义数据长度");
        long packetLength = (long) config.inputFrameLength + config.transportHeaderLength
                + 32 + config.customDataLength;
        if (packetLength > 65507) {
            throw new IllegalArgumentException("拼装后的UDP报文不能超过65507字节");
        }
        config.transportHeader = normalize(config.transportHeader, config.transportHeaderLength);
        config.customData = normalize(config.customData, config.customDataLength);
        config.ver = normalize(config.ver, 1);
        config.mid = normalize(config.mid, 2);
        config.sid = normalize(config.sid, 4);
        config.did = normalize(config.did, 4);
        config.bid = normalize(config.bid, 4);
        config.initialNo = normalize(config.initialNo, 4);
        config.flag = normalize(config.flag, 1);
    }

    /** 各长度都属于单实例配置，原帧头不能超过输入帧。 */
    private static void requireLength(Integer value, int minimum, int maximum, String name) {
        if (value == null || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + "必须在" + minimum + "到" + maximum + "之间");
        }
    }

    /** 校验字段长度并统一大写。 */
    private static String normalize(String value, int length) {
        if (value == null || !value.matches("[0-9a-fA-F]{" + length * 2 + "}")) {
            throw new IllegalArgumentException("协议字段必须是" + length * 2 + "位十六进制文本");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    /** 按当前实例配置构造报文，PDXP包头仍固定为32字节。 */
    public byte[] encode(byte[] frame) {
        if (frame == null || frame.length != config.inputFrameLength) {
            throw new IllegalArgumentException("输入帧长度与模拟源配置不一致");
        }
        int dataLength = config.inputFrameLength + config.customDataLength;
        int packetLength = config.transportHeaderLength + 32 + dataLength;
        ByteBuffer packet = ByteBuffer.allocate(packetLength).order(ByteOrder.LITTLE_ENDIAN);
        // 写入传输头和PDXP固定字段。
        putHex(packet, config.transportHeader);
        putHex(packet, config.ver);
        putHex(packet, config.mid);
        putHex(packet, config.sid);
        putHex(packet, config.did);
        putHex(packet, config.bid);
        packet.putInt((int) sequence);
        sequence = (sequence + 1) & 0xFFFFFFFFL;
        putHex(packet, config.flag);
        packet.putInt(0);
        // 一次读取日期与时间，避免午夜跨日造成不一致。
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(LocalDate.of(2000, 1, 1), now.toLocalDate()) + 1;
        if (days < 1 || days > 65535) {
            throw new IllegalArgumentException("日期超出PDXP积日范围");
        }
        packet.putShort((short) days);
        packet.putInt((int) (now.toLocalTime().toNanoOfDay() / 100000));
        packet.putShort((short) dataLength);
        // 自定义字段前置，原帧头移至数据域末尾。
        putHex(packet, config.customData);
        packet.put(frame, config.telemetryHeaderLength,
                frame.length - config.telemetryHeaderLength);
        packet.put(frame, 0, config.telemetryHeaderLength);
        return packet.array();
    }

    /** 每个字段单独逆序，不对整个报文逆序。 */
    private void putHex(ByteBuffer buffer, String value) {
        for (int i = value.length() - 2; i >= 0; i -= 2) {
            buffer.put((byte) Integer.parseInt(value.substring(i, i + 2), 16));
        }
    }
}

