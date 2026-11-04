package com.example.dataadmin.storage;

import com.example.dataadmin.config.BaselineSsoProperties;
import com.example.dataadmin.config.SatelliteManagementRpcProperties;
import com.example.dataadmin.config.SatelliteTableStorageProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** 管理卫星参数表文件，保留原文件名并通过独立目录避免重名覆盖。 */
@Component
public class SatelliteTableStorage {
    /** 本地文件保存配置。 */
    private final SatelliteTableStorageProperties properties;
    /** 复用已有上传大小配置，限制文件读取时的资源占用。 */
    private final int maxFileSize;
    /** 使用已有基线超时进行流式下载，不将整份远程文件读入内存。 */
    private final RestTemplate downloadClient;

    /** 注入已有文件大小及基线超时配置。 */
    public SatelliteTableStorage(SatelliteTableStorageProperties properties,
            SatelliteManagementRpcProperties rpcProperties, BaselineSsoProperties baselineProperties) {
        // 第一步：复用项目已有配置，不引入新的网络或文件依赖。
        this.properties = properties;
        this.maxFileSize = rpcProperties.getMaxUploadFileSize();
        // 第二步：为下载设置与现有基线查询一致的超时。
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(baselineProperties.getConnectTimeoutMillis());
        factory.setReadTimeout(baselineProperties.getReadTimeoutMillis());
        this.downloadClient = new RestTemplate(factory);
    }

    /** 下载基线参数表，文件名直接采用基线返回的遥测文件名。 */
    public String download(String url, String fileName) {
        // 在响应流关闭前保存文件，供卫星档案关联本地路径。
        return download(url, response -> save(fileName, response.getBody(),
                response.getHeaders().getContentLength()));
    }

    /** 下载任务参数导入所需的内容，复用基线网络和文件大小校验。 */
    public byte[] downloadContent(String baseUrl, String url) {
        // 兼容基线返回的绝对地址和相对于基线根地址的路径。
        String address;
        try {
            address = URI.create(baseUrl.trim().replaceAll("/+$", "") + "/").resolve(url).toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("基线遥测参数表下载地址格式错误", exception);
        }
        return download(address, response -> {
            // 解析配置只需要文件内容，读取时限制实际字节数。
            InputStream input = response.getBody();
            validateInput(input, response.getHeaders().getContentLength());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            copyFile(input, output);
            return output.toByteArray();
        });
    }

    /** 统一校验下载地址，并在响应有效期内交给调用方读取。 */
    private <T> T download(String url, ResponseExtractor<T> reader) {
        // 第一步：只接受可下载的网络地址，不将基线地址直接传给远程服务。
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("基线遥测参数表下载地址格式错误", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("基线遥测参数表下载地址必须使用HTTP或HTTPS");
        }
        try {
            // 第二步：用同一个基线客户端读取响应。
            return downloadClient.execute(uri, HttpMethod.GET, null, reader);
        } catch (RestClientException exception) {
            throw new IllegalStateException("下载基线遥测参数表失败", exception);
        }
    }

    /** 保存人工上传或基线下载的文件，独立目录允许重复卫星代号和文件名。 */
    public String save(String fileName, InputStream input, long declaredSize) {
        // 第一步：校验文件信息，禁止文件名携带路径跳出保存目录。
        String normalizedName = requireFileName(fileName);
        validateInput(input, declaredSize);
        String directory = properties.getTableDirectory();
        if (directory == null || directory.trim().isEmpty()) {
            throw new IllegalArgumentException("卫星参数表保存目录未配置");
        }
        Path target = null;
        try {
            // 第二步：每次上传使用独立目录，原文件和同名卫星互不覆盖。
            Path root = Paths.get(directory).toAbsolutePath().normalize();
            Path folder = root.resolve(UUID.randomUUID().toString());
            target = folder.resolve(normalizedName).normalize();
            if (!target.getParent().equals(folder)) {
                throw new IllegalArgumentException("遥测参数表文件名不能包含路径");
            }
            Files.createDirectories(folder);
            // 第三步：逐段写入并核对实际大小，防止响应长度不准确导致无限读取。
            try (OutputStream output = Files.newOutputStream(target)) {
                copyFile(input, output);
            }
            return target.toString();
        } catch (IOException | RuntimeException exception) {
            // 第四步：只清理本次未保存完整的文件，不影响任何已有参数表。
            if (target != null) {
                try {
                    Files.deleteIfExists(target);
                    Files.deleteIfExists(target.getParent());
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            if (exception instanceof InvalidPathException) {
                throw new IllegalArgumentException("参数表保存目录或文件名格式错误", exception);
            }
            if (exception instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) exception;
            }
            throw new IllegalStateException("保存遥测参数表文件失败", exception);
        }
    }

    /** 读取文件前检查声明长度，允许响应省略长度。 */
    private void validateInput(InputStream input, long declaredSize) {
        if (input == null || declaredSize == 0) {
            throw new IllegalArgumentException("遥测参数表文件不能为空");
        }
        if (declaredSize > maxFileSize) {
            throw new IllegalArgumentException("遥测参数表超过配置的文件大小");
        }
    }

    /** 分段复制文件并检查实际长度，避免无长度响应占用过多资源。 */
    private void copyFile(InputStream input, OutputStream output) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        int length;
        while ((length = input.read(buffer)) != -1) {
            total += length;
            if (total > maxFileSize) {
                throw new IllegalArgumentException("遥测参数表超过配置的文件大小");
            }
            output.write(buffer, 0, length);
        }
        if (total == 0) {
            throw new IllegalArgumentException("遥测参数表文件不能为空");
        }
    }

    /** 将已存在的本机或共享文件转换为可下发的绝对路径。 */
    public String existingPath(String value) {
        // 第一步：规范化路径，拒绝不存在或无法读取的文件。
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("遥测参数表地址不能为空");
        }
        try {
            Path path = Paths.get(value.trim()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException("遥测参数表文件不存在或无法读取");
            }
            return path.toString();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("遥测参数表地址格式错误", exception);
        }
    }

    /** 提取实际本地文件名，确保远程文件名与保存的文件一致。 */
    public String fileName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    /** 文件名只用于命名本地文件，不作为卫星去重条件。 */
    private String requireFileName(String fileName) {
        // 校验的是文件系统路径边界，不限制卫星名称或卫星代号重复。
        if (fileName == null || fileName.trim().isEmpty()
                || ".".equals(fileName) || "..".equals(fileName)
                || fileName.contains("/") || fileName.contains("\\")
                || fileName.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("遥测参数表文件名不能为空或包含路径");
        }
        return fileName;
    }
}
