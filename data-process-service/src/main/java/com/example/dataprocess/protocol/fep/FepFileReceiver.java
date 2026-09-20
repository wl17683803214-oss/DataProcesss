package com.example.dataprocess.protocol.fep;

import com.example.common.protocol.fep.FepProtocolTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 根据FEP协议接收文件数据的工具。 */
public final class FepFileReceiver {

    /** FEP文件接收日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            FepFileReceiver.class);
    /** 未完成文件使用的临时文件后缀。 */
    private static final String TEMP_FILE_SUFFIX = ".fep.part";
    /** 接收文件保存目录。 */
    private final Path saveDirectory;
    /** 当前双方约定的数据单元长度。 */
    private final int dataUnitLength;
    /** 下一个候选文件标识。 */
    private final AtomicInteger nextFileId = new AtomicInteger(1);
    /** 当前正在接收的文件会话，键为文件标识。 */
    private final Map<Integer, ReceiveSession> sessions =
            new ConcurrentHashMap<Integer, ReceiveSession>();

    /** 使用默认4096字节数据单元创建文件接收工具。 */
    public FepFileReceiver(Path saveDirectory) {
        this(saveDirectory, FepProtocolTool.DEFAULT_DATA_UNIT_LENGTH);
    }

    /** 使用指定数据单元长度创建文件接收工具。 */
    public FepFileReceiver(Path saveDirectory, int dataUnitLength) {
        // 保存目录不能为空，数据单元长度必须处于协议允许范围内。
        if (saveDirectory == null) {
            throw new IllegalArgumentException("FEP文件保存目录不能为空");
        }
        if (dataUnitLength < 1
                || dataUnitLength > FepProtocolTool.DEFAULT_DATA_UNIT_LENGTH) {
            throw new IllegalArgumentException("FEP数据单元长度必须在1到4096之间");
        }

        // 保存规范化目录，后续所有文件路径都限制在该目录下。
        this.saveDirectory = saveDirectory.toAbsolutePath().normalize();
        this.dataUnitLength = dataUnitLength;
    }

    /**
     * 处理发送请求包，并返回接收方应发送的请求应答包。
     *
     * @param requestPacket 完整的FEP发送请求包
     * @return 请求应答结果
     * @throws IOException 检查目录、文件或接收空间失败
     */
    public synchronized RequestAcceptance acceptRequest(byte[] requestPacket)
            throws IOException {
        // 默认入口继续使用本地文件状态判断，保持原有调用方式兼容。
        return acceptRequest(requestPacket, false);
    }

    /**
     * 处理发送请求，并允许数据库声明该文件已经可靠接收。
     *
     * @param requestPacket 完整的FEP发送请求包
     * @param externallyCompleted 数据库是否已经记录该文件
     * @return 请求应答结果
     * @throws IOException 检查目录、文件或接收空间失败
     */
    public synchronized RequestAcceptance acceptRequest(
            byte[] requestPacket,
            boolean externallyCompleted) throws IOException {
        // 第一步：解析发送请求中的文件名和文件长度。
        FepProtocolTool.SendRequest request =
                FepProtocolTool.parseSendRequest(requestPacket);
        validateSafeFileName(request.getFileName());

        // 第二步：创建并校验接收目录，确保目标路径没有逃出保存目录。
        Files.createDirectories(saveDirectory);
        Path targetFile = resolveTargetFile(request.getFileName());
        Path temporaryFile = resolveTemporaryFile(request.getFileName());

        // 第三步：为本次FEP文件连接分配唯一的两字节文件标识。
        int fileId = allocateFileId();

        // 第四步：判断完整文件、断点文件或接收方不可用状态。
        int resumeUnitNumber = externallyCompleted
                ? FepProtocolTool.FILE_ALREADY_COMPLETE
                : resolveResumeUnitNumber(
                        targetFile,
                        temporaryFile,
                        request.getFileLength());
        if (resumeUnitNumber >= 0
                && !hasEnoughSpace(temporaryFile, request.getFileLength())) {
            resumeUnitNumber = FepProtocolTool.RECEIVER_UNAVAILABLE;
        }

        // 第五步：只有允许传输时才保存接收会话。
        if (resumeUnitNumber >= 0) {
            normalizeTemporaryFile(temporaryFile, resumeUnitNumber);
            ReceiveSession session = new ReceiveSession(
                    fileId,
                    request.getFileName(),
                    request.getFileLength(),
                    resumeUnitNumber,
                    temporaryFile,
                    targetFile);
            sessions.put(fileId, session);
        }

        // 第六步：按照02包格式返回文件状态、续传单元号和文件标识。
        byte[] responsePacket = FepProtocolTool.buildRequestResponse(
                request.getFileName(), resumeUnitNumber, fileId);
        LOGGER.info("FEP文件请求处理完成，文件名：{}，文件标识：{}，续传数据单元号：{}",
                request.getFileName(), fileId, resumeUnitNumber);
        return new RequestAcceptance(
                request.getFileName(),
                fileId,
                resumeUnitNumber,
                targetFile,
                responsePacket);
    }

    /**
     * 处理一个完整的数据包，并在文件完成时返回结束确认包。
     *
     * @param dataPacket 完整的FEP数据包
     * @return 本次数据单元的接收结果
     * @throws IOException 文件定位、写入或完成操作失败
     */
    public DataReceiveResult receiveData(byte[] dataPacket) throws IOException {
        // 第一步：解析04包中的数据单元号、文件标识和数据内容。
        FepProtocolTool.DataPacket packet =
                FepProtocolTool.parseDataPacket(dataPacket);

        // 第二步：根据文件标识定位发送请求阶段建立的接收会话。
        ReceiveSession session = sessions.get(packet.getFileId());
        if (session == null) {
            throw new IllegalArgumentException(
                    "未找到FEP文件标识对应的接收会话：" + packet.getFileId());
        }

        // 同一文件的单元必须串行校验和写入，避免并发破坏接收进度。
        synchronized (session) {
            return receiveSessionData(session, packet);
        }
    }

    /** 处理同一文件会话中的一个数据单元。 */
    private DataReceiveResult receiveSessionData(
            ReceiveSession session,
            FepProtocolTool.DataPacket packet) throws IOException {
        // 第一步：数据单元必须从应答给发送方的位置开始顺序到达。
        if (packet.getUnitNumber() != session.nextUnitNumber) {
            throw new IllegalArgumentException(
                    "FEP数据单元号不连续，期望：" + session.nextUnitNumber
                            + "，实际：" + packet.getUnitNumber());
        }

        // 第二步：根据数据单元号计算文件中的实际字节偏移。
        long writeOffset = (long) packet.getUnitNumber() * dataUnitLength;
        long remainingLength = (long) session.fileLength - writeOffset;
        if (remainingLength < 0) {
            throw new IllegalArgumentException("FEP数据单元位置超过声明的文件长度");
        }

        // 第三步：校验本单元数据长度，防止数据越过文件声明长度。
        byte[] unitData = packet.getData();
        validateUnitDataLength(remainingLength, unitData.length);

        // 第四步：将非空数据准确写入临时文件的对应位置。
        if (unitData.length > 0) {
            writeUnitData(session.temporaryFile, writeOffset, unitData);
        }

        // 第五步：判断短单元或整倍数文件的空单元结束标志。
        boolean completed = isTransferCompleted(remainingLength, unitData.length);
        if (!completed) {
            session.nextUnitNumber++;
            LOGGER.info("FEP文件数据单元接收完成，文件名：{}，数据单元号：{}，数据长度：{}",
                    session.fileName, packet.getUnitNumber(), unitData.length);
            return DataReceiveResult.receiving(
                    session.fileName,
                    session.fileId,
                    packet.getUnitNumber(),
                    session.targetFile);
        }

        // 第六步：核对最终长度并将临时文件转为正式文件。
        completeFile(session);
        sessions.remove(session.fileId);

        // 第七步：构造03结束确认包，通知发送方文件已经完整接收。
        byte[] confirmationPacket =
                FepProtocolTool.buildFinishConfirmation(session.fileId);
        LOGGER.info("FEP文件接收完成，文件名：{}，文件标识：{}，文件长度：{}",
                session.fileName, session.fileId, session.fileLength);
        return DataReceiveResult.completed(
                session.fileName,
                session.fileId,
                packet.getUnitNumber(),
                session.targetFile,
                confirmationPacket);
    }

    /** 判断已有文件对应的请求应答状态。 */
    private int resolveResumeUnitNumber(
            Path targetFile,
            Path temporaryFile,
            int expectedFileLength) throws IOException {
        // 正式文件存在时，只把长度完全一致的文件认定为已完整接收。
        if (Files.exists(targetFile)) {
            if (Files.isRegularFile(targetFile)
                    && Files.size(targetFile) == expectedFileLength) {
                return FepProtocolTool.FILE_ALREADY_COMPLETE;
            }

            // 不覆盖来源不明或长度不符的正式文件，返回接收方暂不可用。
            return FepProtocolTool.RECEIVER_UNAVAILABLE;
        }

        // 没有临时文件时，从第0个数据单元开始接收。
        if (!Files.exists(temporaryFile)) {
            return FepProtocolTool.START_FROM_BEGINNING;
        }
        if (!Files.isRegularFile(temporaryFile)) {
            return FepProtocolTool.RECEIVER_UNAVAILABLE;
        }

        // 临时文件只按已经完整保存的数据单元计算续传位置。
        long receivedLength = Math.min(
                Files.size(temporaryFile), (long) expectedFileLength);
        return (int) (receivedLength / dataUnitLength);
    }

    /** 将临时文件规范到已完整接收的数据单元边界。 */
    private void normalizeTemporaryFile(
            Path temporaryFile,
            int resumeUnitNumber) throws IOException {
        // 没有临时文件时先创建，后续随机写入可以直接使用。
        if (!Files.exists(temporaryFile)) {
            Files.createFile(temporaryFile);
            return;
        }

        // 断线可能留下半个数据单元，续传前截断并由发送方重新发送该单元。
        long normalizedLength = (long) resumeUnitNumber * dataUnitLength;
        try (RandomAccessFile file = new RandomAccessFile(
                temporaryFile.toFile(), "rw")) {
            file.setLength(normalizedLength);
        }
    }

    /** 检查保存目录是否还有足够空间。 */
    private boolean hasEnoughSpace(
            Path temporaryFile,
            int expectedFileLength) throws IOException {
        // 已接收的临时数据不需要再次占用空间，只检查剩余文件长度。
        long receivedLength = Files.exists(temporaryFile)
                ? Math.min(Files.size(temporaryFile), (long) expectedFileLength)
                : 0L;
        long requiredLength = expectedFileLength - receivedLength;
        FileStore fileStore = Files.getFileStore(saveDirectory);
        return fileStore.getUsableSpace() >= requiredLength;
    }

    /** 校验当前数据单元长度。 */
    private void validateUnitDataLength(
            long remainingLength,
            int actualDataLength) {
        // 单元内容不能超过双方约定的数据单元长度。
        if (actualDataLength > dataUnitLength) {
            throw new IllegalArgumentException(
                    "FEP数据单元长度不能超过" + dataUnitLength + "字节");
        }

        // 文件尚有超过一个单元的数据时，当前数据必须是完整定长单元。
        if (remainingLength > dataUnitLength
                && actualDataLength != dataUnitLength) {
            throw new IllegalArgumentException("FEP非末尾数据单元长度必须为固定单元长度");
        }

        // 最后部分不足一个单元时，数据长度必须与声明的剩余长度一致。
        if (remainingLength > 0
                && remainingLength < dataUnitLength
                && actualDataLength != remainingLength) {
            throw new IllegalArgumentException("FEP末尾数据单元长度与文件剩余长度不一致");
        }

        // 文件数据已写满后，只允许协议规定的空数据单元结束标志。
        if (remainingLength == 0 && actualDataLength != 0) {
            throw new IllegalArgumentException("FEP文件数据已写满，结束数据单元必须为空");
        }
    }

    /** 判断当前数据单元是否结束整个文件传输。 */
    private boolean isTransferCompleted(
            long remainingLength,
            int actualDataLength) {
        // 非整倍数文件由最后一个短数据单元直接表示传输结束。
        if (remainingLength > 0
                && remainingLength < dataUnitLength
                && actualDataLength == remainingLength) {
            return true;
        }

        // 整倍数文件写满后，下一单元必须携带0字节数据作为结束标志。
        return remainingLength == 0 && actualDataLength == 0;
    }

    /** 将一个数据单元写入临时文件。 */
    private void writeUnitData(
            Path temporaryFile,
            long writeOffset,
            byte[] unitData) throws IOException {
        // 使用随机访问文件按Num对应的字节偏移写入。
        try (RandomAccessFile file = new RandomAccessFile(
                temporaryFile.toFile(), "rw")) {
            file.seek(writeOffset);
            file.write(unitData);
        }
    }

    /** 完成临时文件并移动为正式文件。 */
    private void completeFile(ReceiveSession session) throws IOException {
        // 最终长度必须与发送请求声明的文件长度完全一致。
        try (RandomAccessFile file = new RandomAccessFile(
                session.temporaryFile.toFile(), "rw")) {
            if (file.length() != session.fileLength) {
                throw new IOException(
                        "FEP文件实际长度与发送请求声明长度不一致，实际："
                                + file.length() + "，声明：" + session.fileLength);
            }
        }

        // 优先使用原子移动，文件系统不支持时退化为普通移动。
        try {
            Files.move(
                    session.temporaryFile,
                    session.targetFile,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(session.temporaryFile, session.targetFile);
        }
    }

    /** 分配当前未使用的两字节文件标识。 */
    private synchronized int allocateFileId() {
        // 两字节无符号标识从1循环到65535，跳过仍在使用的会话标识。
        for (int count = 0; count < 0xFFFF; count++) {
            int candidate = nextFileId.getAndIncrement();
            if (candidate > 0xFFFF) {
                nextFileId.set(2);
                candidate = 1;
            }
            if (!sessions.containsKey(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("FEP文件标识已经全部占用");
    }

    /** 校验文件名不能包含目录或特殊路径。 */
    private void validateSafeFileName(String fileName) {
        // FEP传输只接受文件名，不允许发送方指定接收方目录。
        if (fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains(":")
                || ".".equals(fileName)
                || "..".equals(fileName)) {
            throw new IllegalArgumentException("FEP文件名不能包含目录或特殊路径");
        }
    }

    /** 解析并校验正式文件路径。 */
    private Path resolveTargetFile(String fileName) {
        // 规范化后再次检查路径仍处于接收目录内。
        Path targetFile = saveDirectory.resolve(fileName).normalize();
        if (!targetFile.getParent().equals(saveDirectory)) {
            throw new IllegalArgumentException("FEP文件保存路径超出接收目录");
        }
        return targetFile;
    }

    /** 解析并校验临时文件路径。 */
    private Path resolveTemporaryFile(String fileName) {
        // 临时文件与正式文件位于同一目录，完成移动时无需跨文件系统。
        Path temporaryFile = saveDirectory
                .resolve(fileName + TEMP_FILE_SUFFIX)
                .normalize();
        if (!temporaryFile.getParent().equals(saveDirectory)) {
            throw new IllegalArgumentException("FEP临时文件路径超出接收目录");
        }
        return temporaryFile;
    }

    /** 发送请求的处理结果。 */
    public static final class RequestAcceptance {
        private final String fileName;
        private final int fileId;
        private final int resumeUnitNumber;
        private final Path filePath;
        private final byte[] responsePacket;

        private RequestAcceptance(
                String fileName,
                int fileId,
                int resumeUnitNumber,
                Path filePath,
                byte[] responsePacket) {
            this.fileName = fileName;
            this.fileId = fileId;
            this.resumeUnitNumber = resumeUnitNumber;
            this.filePath = filePath;
            this.responsePacket = Arrays.copyOf(
                    responsePacket, responsePacket.length);
        }

        public String getFileName() {
            return fileName;
        }

        public int getFileId() {
            return fileId;
        }

        public int getResumeUnitNumber() {
            return resumeUnitNumber;
        }

        public Path getFilePath() {
            return filePath;
        }

        public byte[] getResponsePacket() {
            return Arrays.copyOf(responsePacket, responsePacket.length);
        }
    }

    /** 单个数据单元的接收结果。 */
    public static final class DataReceiveResult {
        private final String fileName;
        private final int fileId;
        private final int unitNumber;
        private final boolean completed;
        private final Path filePath;
        private final byte[] confirmationPacket;

        private DataReceiveResult(
                String fileName,
                int fileId,
                int unitNumber,
                boolean completed,
                Path filePath,
                byte[] confirmationPacket) {
            this.fileName = fileName;
            this.fileId = fileId;
            this.unitNumber = unitNumber;
            this.completed = completed;
            this.filePath = filePath;
            this.confirmationPacket = confirmationPacket == null
                    ? null
                    : Arrays.copyOf(
                            confirmationPacket, confirmationPacket.length);
        }

        private static DataReceiveResult receiving(
                String fileName,
                int fileId,
                int unitNumber,
                Path filePath) {
            return new DataReceiveResult(
                    fileName, fileId, unitNumber, false, filePath, null);
        }

        private static DataReceiveResult completed(
                String fileName,
                int fileId,
                int unitNumber,
                Path filePath,
                byte[] confirmationPacket) {
            return new DataReceiveResult(
                    fileName,
                    fileId,
                    unitNumber,
                    true,
                    filePath,
                    confirmationPacket);
        }

        public String getFileName() {
            return fileName;
        }

        public int getFileId() {
            return fileId;
        }

        public int getUnitNumber() {
            return unitNumber;
        }

        public boolean isCompleted() {
            return completed;
        }

        public Path getFilePath() {
            return filePath;
        }

        public byte[] getConfirmationPacket() {
            return confirmationPacket == null
                    ? null
                    : Arrays.copyOf(
                            confirmationPacket, confirmationPacket.length);
        }
    }

    /** 当前正在接收的文件会话。 */
    private static final class ReceiveSession {
        private final int fileId;
        private final String fileName;
        private final int fileLength;
        private final Path temporaryFile;
        private final Path targetFile;
        private int nextUnitNumber;

        private ReceiveSession(
                int fileId,
                String fileName,
                int fileLength,
                int nextUnitNumber,
                Path temporaryFile,
                Path targetFile) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileLength = fileLength;
            this.nextUnitNumber = nextUnitNumber;
            this.temporaryFile = temporaryFile;
            this.targetFile = targetFile;
        }
    }
}
