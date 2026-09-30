package com.example.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;

/** 所有实例共用的服务容量和FEP参数。 */
@Validated
@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {
    @Min(1) @Max(256)
    private int maxConcurrent = 16;
    @Min(1) @Max(4096)
    private int dataUnitLength = 4096;
    @Min(1)
    private int connectTimeoutMillis = 5000;
    @Min(1)
    private int responseTimeoutMillis = 10000;
    @NotBlank
    private String lockFile = "./data-simulator-service.lock";

    /** 读取公共配置。 */
    public int getMaxConcurrent() {
        return maxConcurrent;
    }
    /** 绑定配置文件中的配置值。 */
    public void setMaxConcurrent(int value) {
        this.maxConcurrent = value;
    }

    /** 读取公共配置。 */
    public int getDataUnitLength() {
        return dataUnitLength;
    }
    /** 绑定配置文件中的配置值。 */
    public void setDataUnitLength(int value) {
        this.dataUnitLength = value;
    }

    /** 读取公共配置。 */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }
    /** 绑定配置文件中的配置值。 */
    public void setConnectTimeoutMillis(int value) {
        this.connectTimeoutMillis = value;
    }

    /** 读取公共配置。 */
    public int getResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }
    /** 绑定配置文件中的配置值。 */
    public void setResponseTimeoutMillis(int value) {
        this.responseTimeoutMillis = value;
    }

    /** 读取公共配置。 */
    public String getLockFile() {
        return lockFile;
    }
    /** 绑定配置文件中的配置值。 */
    public void setLockFile(String value) {
        this.lockFile = value;
    }
}

