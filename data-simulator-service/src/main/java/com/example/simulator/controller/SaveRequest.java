package com.example.simulator.controller;

import com.example.simulator.enums.ProtocolProfile;
import com.example.simulator.enums.SendMode;
import java.net.URI;
import java.net.URISyntaxException;
import com.example.simulator.model.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** 创建与完整更新的请求，禁止客户端写入运行统计。 */
public class SaveRequest {
    @Positive
    public Long id;
    @NotBlank(message = "模拟源名称不能为空") @Size(max = 100)
    public String sourceName;
    @NotNull(message = "协议不能为空")
    public Integer transferProtocol;
    public Integer transferType;
    /** PDXP和FEP发送所需的服务器文件路径。 */
    @Size(max = 1000)
    public String filePath;
    /** UDP发送模式，英文枚举值入库。 */
    public String sendMode;
    /** 组播时可指定本机出口网卡IPv4地址。 */
    @Size(max = 100)
    public String multicastInterface;
    @NotBlank(message = "目标地址不能为空") @Size(max = 100)
    public String targetHost = "127.0.0.1";
    @Min(1) @Max(65535)
    public Integer targetPort;
    /** HTTP目标路径。 */
    @Size(max = 1000)
    public String targetPath;
    /** HTTP请求JSON正文。 */
    @Size(max = 1000000)
    public String messageContent;
    @Min(0) @Max(86400000)
    public Long sendIntervalMillis;
    @Min(0) @Max(1)
    public Integer loopEnabled;
    @Size(max = 500)
    public String remark;
    @Valid
    public PdxpConfig pdxp;

    /** 将允许编辑的字段复制到实体，并补充协议默认值。 */
    public SourceConfig toConfig() {
        ProtocolProfile profile = ProtocolProfile.of(transferProtocol);
        if (transferType != null && transferType != profile.transport) {
            throw new IllegalArgumentException("协议与传输方式不匹配");
        }
        SourceConfig config = new SourceConfig();
        config.id = id;
        config.sourceName = sourceName.trim();
        config.transferProtocol = profile.protocol;
        config.transferType = profile.transport;
        config.filePath = filePath == null || filePath.trim().isEmpty() ? null : filePath.trim();
        config.sendMode = SendMode.of(sendMode).value;
        config.multicastInterface = trimToNull(multicastInterface);
        config.targetHost = targetHost.trim();
        config.targetPort = targetPort == null ? profile.port : targetPort;
        config.sendIntervalMillis = sendIntervalMillis == null ? profile.interval : sendIntervalMillis;
        config.loopEnabled = loopEnabled == null ? profile.loop : loopEnabled;
        config.targetPath = trimToNull(targetPath);
        config.messageContent = messageContent == null ? null : messageContent.trim();
        config.remark = remark;
        validateTransportFields(profile, config);
        // FEP不接受无关参数，防止前端误保存隐藏字段。
        if (profile != ProtocolProfile.PDXP && pdxp != null) {
            throw new IllegalArgumentException("当前协议不需要PDXP参数");
        }
        config.pdxp = profile == ProtocolProfile.PDXP
                ? (pdxp == null ? new PdxpConfig() : pdxp) : null;
        return config;
    }

    /** 按协议组合验证必填字段，避免不同发送模式混用配置。 */
    private void validateTransportFields(ProtocolProfile profile, SourceConfig config) {
        if (profile == ProtocolProfile.JSON) {
            if (config.filePath != null) {
                throw new IllegalArgumentException("HTTP/JSON模拟源不需要上传文件");
            }
            if (config.sendMode != null && !SendMode.UNICAST.value.equals(config.sendMode)) {
                throw new IllegalArgumentException("HTTP/JSON模拟源不支持UDP发送模式");
            }
            config.sendMode = null;
            config.multicastInterface = null;
            if (config.targetPath == null || !config.targetPath.startsWith("/")
                    || config.targetPath.contains("?") || config.targetPath.contains("#")) {
                throw new IllegalArgumentException("HTTP目标路径必须以/开头");
            }
            if (config.messageContent == null || config.messageContent.isEmpty()) {
                throw new IllegalArgumentException("HTTP请求JSON正文不能为空");
            }
            try {
                URI target = new URI("http", null, config.targetHost, config.targetPort,
                        config.targetPath, null, null);
                if (target.getHost() == null) {
                    throw new IllegalArgumentException("HTTP目标主机地址无效");
                }
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("HTTP目标地址或路径无效");
            }
            return;
        }
        if (config.filePath == null || config.filePath.isEmpty()) {
            throw new IllegalArgumentException("PDXP和FEP模拟源必须选择有效的数据文件");
        }
        if (config.targetPath != null || config.messageContent != null) {
            throw new IllegalArgumentException("PDXP和FEP模拟源不能设置HTTP路径或JSON正文");
        }
        if (profile == ProtocolProfile.FEP && !SendMode.UNICAST.value.equals(config.sendMode)) {
            throw new IllegalArgumentException("FEP模拟源不支持UDP发送模式");
        }
        if (profile == ProtocolProfile.FEP) {
            config.sendMode = null;
            config.multicastInterface = null;
        }
        if (profile == ProtocolProfile.PDXP && SendMode.UNICAST.value.equals(config.sendMode)
                && config.multicastInterface != null) {
            throw new IllegalArgumentException("单播模式不能设置组播出口网卡");
        }
    }

    /** 去除可选文本字段的首尾空白并统一空值。 */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

