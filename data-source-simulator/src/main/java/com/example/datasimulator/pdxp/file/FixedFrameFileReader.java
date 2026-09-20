package com.example.datasimulator.pdxp.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** 按固定长度连续读取二进制文件。 */
public final class FixedFrameFileReader implements AutoCloseable {

    /** 文件输入流。 */
    private final InputStream inputStream;
    /** 每帧固定长度。 */
    private final int frameLength;

    /** 打开文件并校验逐帧读取所需的基础条件。 */
    public FixedFrameFileReader(Path filePath, int frameLength)
            throws IOException {
        // 第一步：帧长度必须大于零，避免出现无法推进的读取循环。
        if (frameLength <= 0) {
            throw new IllegalArgumentException("文件帧长度必须大于0");
        }
        // 第二步：文件必须存在且是普通文件。
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("模拟源文件不存在：" + filePath);
        }
        // 第三步：不预读整个文件长度，直接打开输入流并在读取时判断帧是否完整。
        this.inputStream = Files.newInputStream(filePath);
        this.frameLength = frameLength;
    }

    /** 读取下一帧，文件结束时返回空值。 */
    public byte[] readNextFrame() throws IOException {
        // 第一步：先读取首字节，用于区分正常帧和文件结束。
        int firstByte = inputStream.read();
        if (firstByte == -1) {
            return null;
        }

        // 第二步：把首字节和剩余字节完整写入固定长度数组。
        byte[] frame = new byte[frameLength];
        frame[0] = (byte) firstByte;
        int totalRead = 1;
        while (totalRead < frameLength) {
            int readCount = inputStream.read(
                    frame, totalRead, frameLength - totalRead);
            if (readCount == -1) {
                throw new IOException(
                        "读取模拟源文件时遇到不完整的"
                                + frameLength + "字节帧");
            }
            totalRead += readCount;
        }
        return frame;
    }

    /** 关闭文件输入流。 */
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
