package com.example.dataprocess.protocol.command;

import SatDataCenter.DataExchange.TmTc.TelecommandMessages.CommandExecResultMsg;
import com.example.dataprocess.tool.PdxpParser;
import com.google.protobuf.Timestamp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

/** 将PDXP原始指令响应数据域转换为单指令执行结果消息。 */
public final class CommandResponseParser {

    /** 指令响应协议规定的业务标识。 */
    public static final long RESPONSE_DATA_ID = 0x05040001L;
    /** 协议指定的发送方和接收方地址。 */
    private static final long RESPONSE_SYSTEM_ID = 0x00000006L;
    /** 积日和当日时标使用北京时间解释。 */
    private static final ZoneOffset SEND_TIME_OFFSET = ZoneOffset.ofHours(8);

    /** 工具类不允许实例化。 */
    private CommandResponseParser() {
    }

    /** 识别指令响应业务标识，其他业务报文由接收入口跳过。 */
    public static boolean supports(PdxpParser.PdxpPacket packet) {
        return packet != null && packet.getDataId() == RESPONSE_DATA_ID;
    }

    /** 按协议顺序读取数据域，只为约定的五个消息字段赋值。 */
    public static CommandExecResultMsg parse(PdxpParser.PdxpPacket packet) {
        // 第一步：校验响应业务标识和协议约定的固定包头字段。
        if (!supports(packet)) {
            throw new IllegalArgumentException("收到的报文不是指令响应业务报文");
        }
        if (packet.getVersionByte() != 0x80
                || packet.getSourceId() != RESPONSE_SYSTEM_ID
                || packet.getDestinationId() != RESPONSE_SYSTEM_ID) {
            throw new IllegalArgumentException("指令响应版本、信源或信宿不符合协议");
        }

        // 第二步：沿用PDXP小端规则，先按两字节长度读取完整指令标识。
        ByteBuffer data = ByteBuffer.wrap(packet.getData()).order(ByteOrder.LITTLE_ENDIAN);
        String commandId = readString(data, "指令标识");

        // 第三步：读取四字节指令号，再按声明长度解码指令代号字符串。
        requireRemaining(data, Integer.BYTES, "指令号");
        data.getInt();
        String commandSymbol = readString(data, "指令代号");

        // 第四步：结果码先转字符串，再显式映射到现有消息枚举。
        requireRemaining(data, Byte.BYTES, "指令结果");
        String resultCode = Integer.toString(Byte.toUnsignedInt(data.get()));
        CommandResponseResult result = CommandResponseResult.fromProtocolCode(resultCode);

        // 第五步：结果字段应当位于数据域末尾，校验整段数据已经读取完整。
        if (data.hasRemaining()) {
            throw new IllegalArgumentException("指令响应数据域包含多余字节：" + data.remaining());
        }

        // 第六步：复用PDXP日期和时长换算，保留每单位十万纳秒的精度。
        Instant sendingTime = packet.getSendDate().atTime(LocalTime.MIDNIGHT)
                .plus(packet.getTimeSinceMidnight()).toInstant(SEND_TIME_OFFSET);
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(sendingTime.getEpochSecond())
                .setNanos(sendingTime.getNano())
                .build();

        // 第七步：卫星代号转十进制字符串，只组装约定的五个字段。
        return CommandExecResultMsg.newBuilder()
                .setSatCode(Integer.toString(packet.getMissionId()))
                .setSendingResult(result.getProtoResult())
                .setSendingTime(timestamp)
                .setCmSymbol(commandSymbol)
                .setCommandId(commandId)
                .build();
    }

    /** 读取两字节无符号长度及对应字符串，长度单位为字节。 */
    private static String readString(ByteBuffer data, String fieldName) {
        // 第一步：无符号解析长度，拒绝空字段和超过剩余报文的长度。
        requireRemaining(data, Short.BYTES, fieldName + "长度");
        int length = Short.toUnsignedInt(data.getShort());
        if (length == 0) {
            throw new IllegalArgumentException("指令响应" + fieldName + "不能为空");
        }
        requireRemaining(data, length, fieldName);

        // 第二步：只解码字段声明的字节，非法编码不能被静默替换成其他字符。
        byte[] bytes = new byte[length];
        data.get(bytes);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("指令响应" + fieldName + "字符编码不正确", exception);
        }
    }

    /** 校验读取边界，防止截断报文或错误长度进入消息组装流程。 */
    private static void requireRemaining(ByteBuffer data, int length, String fieldName) {
        // 所有字段统一在读取前校验，异常日志提供具体字段位置。
        if (data.remaining() < length) {
            throw new IllegalArgumentException("指令响应" + fieldName + "不完整，需要字节数："
                    + length + "，剩余字节数：" + data.remaining());
        }
    }
}
