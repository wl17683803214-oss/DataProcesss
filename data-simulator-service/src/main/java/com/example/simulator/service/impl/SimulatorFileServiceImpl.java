package com.example.simulator.service.impl;

import com.example.simulator.config.SimulatorProperties;
import com.example.simulator.model.UploadedFile;
import com.example.simulator.service.SimulatorFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 上传文件独立保存，继续复用模拟源现有的本地文件发送流程。 */
@Service
public class SimulatorFileServiceImpl implements SimulatorFileService {

    /** 文件保存和失败清理的中文日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorFileServiceImpl.class);
    /** 与现有模拟源文件路径入参保持一致的最大长度。 */
    private static final int MAX_FILE_PATH_LENGTH = 1000;
    /** 常见文件系统允许的单个文件名最大字节数。 */
    private static final int MAX_FILE_NAME_BYTES = 255;

    /** 模拟源上传目录配置。 */
    private final SimulatorProperties properties;
    /** 复用服务上传限制，避免维护两套大小配置。 */
    private final MultipartProperties multipartProperties;

    public SimulatorFileServiceImpl(
            SimulatorProperties properties,
            MultipartProperties multipartProperties) {
        this.properties = properties;
        this.multipartProperties = multipartProperties;
    }

    /** 文件完整写入成功后才返回路径，不覆盖任何已有上传文件。 */
    @Override
    public UploadedFile upload(MultipartFile file) throws IOException {
        // 第一步：校验上传文件名和大小，允许具有有效文件名的空文件用于FEP。
        if (file == null) {
            throw new IllegalArgumentException("请选择需要上传的文件");
        }
        String fileName = validateFileName(file.getOriginalFilename());
        long maxFileSize = multipartProperties.getMaxFileSize().toBytes();
        if (maxFileSize >= 0 && file.getSize() > maxFileSize) {
            throw new MaxUploadSizeExceededException(maxFileSize);
        }

        // 第二步：在配置目录中创建独立子目录，保留原文件名且隔离同名上传。
        Path root = Paths.get(properties.getUploadDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        root = root.toRealPath();
        Path directory = Files.createTempDirectory(root, "file-");
        Path destination = directory.resolve(fileName).normalize();
        try {
            // 第三步：校验目标范围及现有字段长度，再以流式方式写入文件。
            if (!destination.getParent().equals(directory)
                    || destination.toString().length() > MAX_FILE_PATH_LENGTH) {
                throw new IllegalArgumentException("上传文件保存路径无效或超过1000字符");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination);
            }

            // 第四步：读取实际保存大小并返回结果，后续循环发送不删除上传文件。
            long fileSize = Files.size(destination);
            LOGGER.info("模拟源文件上传成功，文件名：{}，文件大小：{}字节", fileName, fileSize);
            return new UploadedFile(fileName, fileSize, destination.toString());
        } catch (IOException | RuntimeException exception) {
            // 第五步：上传失败时只清理本次创建的文件和目录，避免残留半个文件。
            cleanFailedUpload(root, directory, destination);
            throw exception;
        }
    }

    /** 保留原文件名，拒绝路径、控制字符和跨平台不可保存的名称。 */
    private String validateFileName(String fileName) {
        // 先拒绝空名称和特殊目录名，不替换用户文件名中的有效字符。
        if (fileName == null || fileName.trim().isEmpty()
                || ".".equals(fileName) || "..".equals(fileName)) {
            throw new IllegalArgumentException("上传文件名不能为空或为特殊目录名");
        }

        // 文件名必须是单独名称，不能携带路径或Windows不支持的字符。
        for (int index = 0; index < fileName.length(); index++) {
            char character = fileName.charAt(index);
            if (Character.isISOControl(character) || "/\\<>:\"|?*".indexOf(character) >= 0) {
                throw new IllegalArgumentException("上传文件名不能包含路径、控制字符或非法字符");
            }
        }

        // 拒绝会被文件系统截断、改名或解释为系统设备的名称。
        String stem = fileName.split("\\.", 2)[0];
        if (fileName.endsWith(".") || fileName.endsWith(" ")
                || stem.matches("(?i)CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")
                || fileName.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_NAME_BYTES) {
            throw new IllegalArgumentException("上传文件名不受支持或长度超过255字节");
        }
        return fileName;
    }

    /** 只清理上传根目录下由当前请求创建的直接子目录，不递归删除。 */
    private void cleanFailedUpload(Path root, Path directory, Path destination) {
        // 清理前检查解析后的绝对路径，保证不会删除配置目录或其他文件。
        if (!directory.getParent().equals(root) || !destination.getParent().equals(directory)) {
            return;
        }
        try {
            Files.deleteIfExists(destination);
            Files.deleteIfExists(directory);
        } catch (IOException exception) {
            // 清理失败保留原上传异常，并通过中文日志提示运维检查磁盘。
            LOGGER.warn("模拟源上传失败后的临时文件清理失败，目录：{}", directory, exception);
        }
    }
}
