package com.example.datasimulator.pdxp.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 固定512字节文件读取测试。 */
class FixedFrameFileReaderTest {

    /** 测试临时目录。 */
    @TempDir
    Path temporaryDirectory;

    /** 验证连续文件可以按512字节准确拆成多帧。 */
    @Test
    void shouldReadMultipleFramesInOrder() throws Exception {
        // 第一步：准备两个内容不同的连续512字节帧。
        byte[] firstFrame = new byte[512];
        byte[] secondFrame = new byte[512];
        java.util.Arrays.fill(firstFrame, (byte) 0x11);
        java.util.Arrays.fill(secondFrame, (byte) 0x22);
        byte[] fileData = new byte[1024];
        System.arraycopy(firstFrame, 0, fileData, 0, 512);
        System.arraycopy(secondFrame, 0, fileData, 512, 512);
        Path filePath = temporaryDirectory.resolve("source.bin");
        Files.write(filePath, fileData);

        // 第二步：依次读取两帧，第三次读取应明确到达文件末尾。
        try (FixedFrameFileReader reader =
                     new FixedFrameFileReader(filePath, 512)) {
            assertArrayEquals(firstFrame, reader.readNextFrame());
            assertArrayEquals(secondFrame, reader.readNextFrame());
            assertNull(reader.readNextFrame());
        }
    }

    /** 验证打开时不校验总长度，读取到末尾残缺帧时才报错。 */
    @Test
    void shouldRejectIncompleteFrame() throws Exception {
        // 第一步：准备一帧完整数据和一个残留字节。
        Path filePath = temporaryDirectory.resolve("incomplete.bin");
        Files.write(filePath, new byte[513]);

        // 第二步：文件可以正常打开并读出首帧，读取下一帧时报告残缺。
        try (FixedFrameFileReader reader =
                     new FixedFrameFileReader(filePath, 512)) {
            assertArrayEquals(new byte[512], reader.readNextFrame());
            java.io.IOException exception = assertThrows(
                    java.io.IOException.class,
                    reader::readNextFrame);
            assertEquals("读取模拟源文件时遇到不完整的512字节帧",
                    exception.getMessage());
        }
    }
}
