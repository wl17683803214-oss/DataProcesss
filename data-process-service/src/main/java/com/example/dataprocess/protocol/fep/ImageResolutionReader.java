package com.example.dataprocess.protocol.fep;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 从接收完成的图片头读取像素尺寸，识别失败不阻断文件后续处理。 */
public final class ImageResolutionReader {
    /** 分辨率识别日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageResolutionReader.class);
    /** 与图片组件查询使用的图片后缀保持一致。 */
    private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff"));

    /** 工具类不允许实例化。 */
    private ImageResolutionReader() {
    }

    /** 返回宽乘高的像素尺寸，普通文件、不支持的格式或异常图片返回空值。 */
    public static String read(Path file, String fileType) {
        // 第一步：普通文件不尝试解析图片，避免影响文件接收效率。
        if (!IMAGE_TYPES.contains(fileType)) {
            return null;
        }
        // 第二步：直接读取本地文件头，不解码整张位图，也不访问对象存储。
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width > 0 && height > 0 ? width + "×" + height : null;
            } finally {
                // 第三步：无论成功或失败都释放图片解析资源。
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            // 第四步：异常只记录中文提示，文件仍按原流程上传和发布。
            LOGGER.warn("图片分辨率识别失败，文件：{}，原因：{}", file, exception.getMessage());
            return null;
        }
    }
}
