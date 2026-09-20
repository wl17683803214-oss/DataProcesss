package com.example.common.protocol.fep;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** FEP协议包编解码工具。 */
public final class FepProtocolTool {

    /** 文件名固定字段长度。 */
    public static final int FILE_NAME_LENGTH = 64;
    /** 文件长度字段长度。 */
    public static final int FILE_LENGTH_FIELD_LENGTH = 4;
    /** 数据单元号字段长度。 */
    public static final int UNIT_NUMBER_FIELD_LENGTH = 4;
    /** 文件标识字段长度。 */
    public static final int FILE_ID_FIELD_LENGTH = 2;
    /** 默认文件数据单元长度。 */
    public static final int DEFAULT_DATA_UNIT_LENGTH = 4_096;
    /** 文件不存在，从第0个数据单元开始传输。 */
    public static final int START_FROM_BEGINNING = 0;
    /** 文件已经完整存在。 */
    public static final int FILE_ALREADY_COMPLETE = -1;
    /** 接收方暂时不能接收文件。 */
    public static final int RECEIVER_UNAVAILABLE = -2;

    /** 工具类不允许实例化。 */
    private FepProtocolTool() {
    }

    /** 读取FEP包的类型标识。 */
    public static FepPacketType readPacketType(byte[] packetData) {
        // 所有FEP包至少包含一个类型字节。
        validateMinimumLength(packetData, 1, "FEP协议包");
        return FepPacketType.fromCode(packetData[0] & 0xFF);
    }

    /** 构造发送请求包：01、文件名64字节、文件长度4字节。 */
    public static byte[] buildSendRequest(String fileName, int fileLength) {
        // 文件长度字段采用有符号int保存，因此当前实现不接受负数。
        if (fileLength < 0) {
            throw new IllegalArgumentException("FEP文件长度不能小于0");
        }

        // 按协议固定长度依次写入类型、文件名和小端文件长度。
        ByteBuffer buffer = littleEndianBuffer(
                1 + FILE_NAME_LENGTH + FILE_LENGTH_FIELD_LENGTH);
        buffer.put((byte) FepPacketType.SEND_REQUEST.getCode());
        buffer.put(encodeFileName(fileName));
        buffer.putInt(fileLength);
        return buffer.array();
    }

    /** 解析发送请求包。 */
    public static SendRequest parseSendRequest(byte[] packetData) {
        // 发送请求包长度固定为69字节，长度不符表示包边界或协议内容错误。
        int expectedLength = 1 + FILE_NAME_LENGTH + FILE_LENGTH_FIELD_LENGTH;
        validateExactPacket(packetData, FepPacketType.SEND_REQUEST, expectedLength);

        // 跳过类型字段，依次读取文件名和小端文件长度。
        ByteBuffer buffer = littleEndianBuffer(packetData);
        buffer.get();
        byte[] fileNameData = new byte[FILE_NAME_LENGTH];
        buffer.get(fileNameData);
        String fileName = decodeFileName(fileNameData);
        int fileLength = buffer.getInt();
        if (fileLength < 0) {
            throw new IllegalArgumentException("FEP发送请求中的文件长度不能小于0");
        }
        return new SendRequest(fileName, fileLength);
    }

    /** 构造请求应答包：02、文件名64字节、数据单元号4字节、文件标识2字节。 */
    public static byte[] buildRequestResponse(
            String fileName,
            int unitNumber,
            int fileId) {
        // 文件标识在协议中占两个字节，按无符号数范围校验。
        validateFileId(fileId);

        // 按纸质协议图C.3的顺序写入全部字段。
        ByteBuffer buffer = littleEndianBuffer(
                1 + FILE_NAME_LENGTH
                        + UNIT_NUMBER_FIELD_LENGTH
                        + FILE_ID_FIELD_LENGTH);
        buffer.put((byte) FepPacketType.REQUEST_RESPONSE.getCode());
        buffer.put(encodeFileName(fileName));
        buffer.putInt(unitNumber);
        buffer.putShort((short) fileId);
        return buffer.array();
    }

    /** 解析请求应答包。 */
    public static RequestResponse parseRequestResponse(byte[] packetData) {
        // 请求应答包长度固定为71字节。
        int expectedLength = 1 + FILE_NAME_LENGTH
                + UNIT_NUMBER_FIELD_LENGTH + FILE_ID_FIELD_LENGTH;
        validateExactPacket(packetData, FepPacketType.REQUEST_RESPONSE, expectedLength);

        // 按协议字段顺序读取文件名、续传数据单元号和文件标识。
        ByteBuffer buffer = littleEndianBuffer(packetData);
        buffer.get();
        byte[] fileNameData = new byte[FILE_NAME_LENGTH];
        buffer.get(fileNameData);
        String fileName = decodeFileName(fileNameData);
        int unitNumber = buffer.getInt();
        int fileId = Short.toUnsignedInt(buffer.getShort());
        return new RequestResponse(fileName, unitNumber, fileId);
    }

    /** 构造结束确认包：03、文件标识2字节。 */
    public static byte[] buildFinishConfirmation(int fileId) {
        // 结束确认中的文件标识必须处于两个字节的无符号范围内。
        validateFileId(fileId);

        // 按协议写入类型和小端文件标识。
        ByteBuffer buffer = littleEndianBuffer(1 + FILE_ID_FIELD_LENGTH);
        buffer.put((byte) FepPacketType.FINISH_CONFIRMATION.getCode());
        buffer.putShort((short) fileId);
        return buffer.array();
    }

    /** 解析结束确认包。 */
    public static FinishConfirmation parseFinishConfirmation(byte[] packetData) {
        // 结束确认包长度固定为3字节。
        validateExactPacket(
                packetData,
                FepPacketType.FINISH_CONFIRMATION,
                1 + FILE_ID_FIELD_LENGTH);

        // 跳过类型字段并读取小端文件标识。
        ByteBuffer buffer = littleEndianBuffer(packetData);
        buffer.get();
        return new FinishConfirmation(Short.toUnsignedInt(buffer.getShort()));
    }

    /** 构造数据包：04、数据单元号4字节、文件标识2字节、数据内容。 */
    public static byte[] buildDataPacket(
            int unitNumber,
            int fileId,
            byte[] data) {
        // 数据单元号从0开始，文件标识和数据内容也必须有效。
        if (unitNumber < 0) {
            throw new IllegalArgumentException("FEP数据单元号不能小于0");
        }
        validateFileId(fileId);
        byte[] safeData = data == null ? new byte[0] : Arrays.copyOf(data, data.length);
        if (safeData.length > DEFAULT_DATA_UNIT_LENGTH) {
            throw new IllegalArgumentException("FEP数据内容不能超过4096字节");
        }

        // 按纸质协议图C.5规定的Num、ID、Data顺序写入。
        ByteBuffer buffer = littleEndianBuffer(
                1 + UNIT_NUMBER_FIELD_LENGTH
                        + FILE_ID_FIELD_LENGTH + safeData.length);
        buffer.put((byte) FepPacketType.DATA.getCode());
        buffer.putInt(unitNumber);
        buffer.putShort((short) fileId);
        buffer.put(safeData);
        return buffer.array();
    }

    /** 解析数据包。 */
    public static DataPacket parseDataPacket(byte[] packetData) {
        // 数据包固定头为7字节，后续内容允许为0至4096字节。
        int headerLength = 1 + UNIT_NUMBER_FIELD_LENGTH + FILE_ID_FIELD_LENGTH;
        validateMinimumLength(packetData, headerLength, "FEP数据包");
        validatePacketType(packetData, FepPacketType.DATA);
        if (packetData.length - headerLength > DEFAULT_DATA_UNIT_LENGTH) {
            throw new IllegalArgumentException("FEP数据包内容不能超过4096字节");
        }

        // 先读取小端数据单元号和文件标识，再复制剩余的数据内容。
        ByteBuffer buffer = littleEndianBuffer(packetData);
        buffer.get();
        int unitNumber = buffer.getInt();
        int fileId = Short.toUnsignedInt(buffer.getShort());
        if (unitNumber < 0) {
            throw new IllegalArgumentException("FEP数据包中的数据单元号不能小于0");
        }
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new DataPacket(unitNumber, fileId, data);
    }

    /** 将文件名编码到固定64字节字段。 */
    private static byte[] encodeFileName(String fileName) {
        // 文件名不能为空，否则文件标识无法对应实际文件。
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("FEP文件名不能为空");
        }

        // 使用双方约定的UTF-8编码，并按字节长度而不是字符数量校验。
        byte[] encodedData = fileName.getBytes(StandardCharsets.UTF_8);
        if (encodedData.length > FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("FEP文件名UTF-8编码后不能超过64字节");
        }

        // Arrays.copyOf会将不足64字节的尾部自动补00。
        return Arrays.copyOf(encodedData, FILE_NAME_LENGTH);
    }

    /** 从固定64字节字段解码文件名。 */
    private static String decodeFileName(byte[] fileNameData) {
        // 找到第一个00，后面的字节视为协议填充内容。
        int contentLength = 0;
        while (contentLength < fileNameData.length
                && fileNameData[contentLength] != 0) {
            contentLength++;
        }

        // 使用双方约定的UTF-8解码实际文件名内容。
        String fileName = new String(
                fileNameData, 0, contentLength, StandardCharsets.UTF_8);
        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("FEP协议包中的文件名不能为空");
        }
        return fileName;
    }

    /** 校验固定长度协议包。 */
    private static void validateExactPacket(
            byte[] packetData,
            FepPacketType expectedType,
            int expectedLength) {
        // 先校验长度，避免读取类型时越界。
        validateMinimumLength(packetData, expectedLength, expectedType.getName());
        if (packetData.length != expectedLength) {
            throw new IllegalArgumentException(
                    expectedType.getName() + "长度必须为" + expectedLength + "字节");
        }

        // 再校验实际类型，防止把其他固定长度包按错误结构解析。
        validatePacketType(packetData, expectedType);
    }

    /** 校验协议包最小长度。 */
    private static void validateMinimumLength(
            byte[] packetData,
            int minimumLength,
            String packetName) {
        // 空数据或长度不足都不能构成有效FEP协议包。
        if (packetData == null || packetData.length < minimumLength) {
            throw new IllegalArgumentException(
                    packetName + "长度不能小于" + minimumLength + "字节");
        }
    }

    /** 校验协议包类型。 */
    private static void validatePacketType(
            byte[] packetData,
            FepPacketType expectedType) {
        // 类型字段必须与当前解析入口对应。
        FepPacketType actualType = readPacketType(packetData);
        if (actualType != expectedType) {
            throw new IllegalArgumentException(
                    "FEP包类型错误，期望" + expectedType.getName()
                            + "，实际为" + actualType.getName());
        }
    }

    /** 校验两个字节的无符号文件标识。 */
    private static void validateFileId(int fileId) {
        // 0保留为无效标识，正常文件标识使用1至65535。
        if (fileId < 1 || fileId > 0xFFFF) {
            throw new IllegalArgumentException("FEP文件标识必须在1到65535之间");
        }
    }

    /** 创建指定容量的小端字节缓冲区。 */
    private static ByteBuffer littleEndianBuffer(int capacity) {
        // FEP整数字段按当前双方约定使用小端字节序。
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** 包装现有字节的小端字节缓冲区。 */
    private static ByteBuffer littleEndianBuffer(byte[] data) {
        // 解析时统一使用小端字节序读取所有整数。
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** 发送请求包内容。 */
    public static final class SendRequest {
        private final String fileName;
        private final int fileLength;

        private SendRequest(String fileName, int fileLength) {
            this.fileName = fileName;
            this.fileLength = fileLength;
        }

        public String getFileName() {
            return fileName;
        }

        public int getFileLength() {
            return fileLength;
        }
    }

    /** 请求应答包内容。 */
    public static final class RequestResponse {
        private final String fileName;
        private final int unitNumber;
        private final int fileId;

        private RequestResponse(String fileName, int unitNumber, int fileId) {
            this.fileName = fileName;
            this.unitNumber = unitNumber;
            this.fileId = fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public int getUnitNumber() {
            return unitNumber;
        }

        public int getFileId() {
            return fileId;
        }
    }

    /** 结束确认包内容。 */
    public static final class FinishConfirmation {
        private final int fileId;

        private FinishConfirmation(int fileId) {
            this.fileId = fileId;
        }

        public int getFileId() {
            return fileId;
        }
    }

    /** 数据包内容。 */
    public static final class DataPacket {
        private final int unitNumber;
        private final int fileId;
        private final byte[] data;

        private DataPacket(int unitNumber, int fileId, byte[] data) {
            this.unitNumber = unitNumber;
            this.fileId = fileId;
            this.data = Arrays.copyOf(data, data.length);
        }

        public int getUnitNumber() {
            return unitNumber;
        }

        public int getFileId() {
            return fileId;
        }

        public byte[] getData() {
            return Arrays.copyOf(data, data.length);
        }
    }
}
