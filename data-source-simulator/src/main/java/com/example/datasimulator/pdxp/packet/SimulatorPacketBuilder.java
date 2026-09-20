package com.example.datasimulator.pdxp.packet;

import com.example.datasimulator.pdxp.config.PdxpSimulatorConfig;

import java.util.Arrays;

/** 将一个512字节输入帧重新排列为最终UDP发送报文。 */
public final class SimulatorPacketBuilder {

    /** 用户自定义数据固定长度。 */
    public static final int CUSTOM_DATA_LENGTH = 5;
    /** PDXP数据域固定长度。 */
    public static final int PDXP_DATA_LENGTH = CUSTOM_DATA_LENGTH
            + PdxpSimulatorConfig.INPUT_FRAME_LENGTH;
    /** 两字节传输头固定长度。 */
    public static final int TRANSPORT_HEADER_LENGTH = 2;
    /** 最终UDP报文固定长度。 */
    public static final int UDP_PACKET_LENGTH = TRANSPORT_HEADER_LENGTH
            + PdxpSimulatorConfig.PDXP_HEADER_LENGTH
            + PDXP_DATA_LENGTH;
    /** PDXP包头构造器。 */
    private final PdxpPacketBuilder pdxpPacketBuilder;

    public SimulatorPacketBuilder(PdxpPacketBuilder pdxpPacketBuilder) {
        this.pdxpPacketBuilder = pdxpPacketBuilder;
    }

    /** 构造两字节传输头、PDXP包头和重新排列的数据域。 */
    public byte[] build(byte[] inputFrame) {
        // 第一步：输入必须是一个完整的512字节遥测帧。
        if (inputFrame == null
                || inputFrame.length != PdxpSimulatorConfig.INPUT_FRAME_LENGTH) {
            throw new IllegalArgumentException("输入遥测帧必须正好为512字节");
        }

        // 第二步：用户配置的传输头和自定义数据分别按字段整体转成小端。
        byte[] transportHeader = HexValueCodec.decodeLittleEndian(
                PdxpSimulatorConfig.TRANSPORT_HEADER,
                TRANSPORT_HEADER_LENGTH,
                "传输头");
        byte[] customData = HexValueCodec.decodeLittleEndian(
                PdxpSimulatorConfig.CUSTOM_DATA,
                CUSTOM_DATA_LENGTH,
                "自定义数据");

        // 第三步：分离原帧开头四字节遥测帧头和后续508字节数据。
        byte[] telemetryHeader = Arrays.copyOfRange(
                inputFrame, 0, PdxpSimulatorConfig.TELEMETRY_HEADER_LENGTH);
        byte[] sourceData = Arrays.copyOfRange(
                inputFrame,
                PdxpSimulatorConfig.TELEMETRY_HEADER_LENGTH,
                inputFrame.length);

        // 第四步：构造517字节数据域，自定义数据在前，原遥测帧头移动到最后。
        byte[] pdxpData = new byte[PDXP_DATA_LENGTH];
        int dataOffset = 0;
        System.arraycopy(customData, 0, pdxpData,
                dataOffset, customData.length);
        dataOffset += customData.length;
        System.arraycopy(sourceData, 0, pdxpData,
                dataOffset, sourceData.length);
        dataOffset += sourceData.length;
        System.arraycopy(telemetryHeader, 0, pdxpData,
                dataOffset, telemetryHeader.length);

        // 第五步：根据实际517字节数据域生成32字节PDXP包头。
        byte[] pdxpHeader = pdxpPacketBuilder.build(pdxpData.length);

        // 第六步：依次拼接传输头、PDXP包头和数据域，得到551字节UDP报文。
        byte[] udpPacket = new byte[UDP_PACKET_LENGTH];
        int packetOffset = 0;
        System.arraycopy(transportHeader, 0, udpPacket,
                packetOffset, transportHeader.length);
        packetOffset += transportHeader.length;
        System.arraycopy(pdxpHeader, 0, udpPacket,
                packetOffset, pdxpHeader.length);
        packetOffset += pdxpHeader.length;
        System.arraycopy(pdxpData, 0, udpPacket,
                packetOffset, pdxpData.length);
        return udpPacket;
    }
}
