package com.example.dataadmin.service.monitor.support;

import com.example.common.tool.HttpRequestTool;
import com.example.dataadmin.config.SystemMonitorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.util.function.DoubleSupplier;

/** 系统监控和处理页面共用的资源指标读取组件。 */
@Component
public class SystemResourceMetrics {

    /** 当前查询使用的处理服务内网地址配置。 */
    private final SystemMonitorProperties monitorProperties;
    /** 内部 CPU 查询的连接和读取超时，单位毫秒。 */
    private final HttpRequestTool processCpuClient = new HttpRequestTool(500, 500);

    public SystemResourceMetrics(SystemMonitorProperties monitorProperties) {
        this.monitorProperties = monitorProperties;
    }

    /** 查询处理进程 CPU，地址留空时使用调用方提供的本机读数。 */
    public Double readCurrentCpuUsage(DoubleSupplier localCpuUsage) {
        // 本机部署允许留空地址，按原方式返回本机读数。
        String processCpuUrl = monitorProperties.getProcessCpuUrl();
        if (!StringUtils.hasText(processCpuUrl)) {
            return localCpuUsage.getAsDouble();
        }

        try {
            // 复用处理服务的后台采样值，只接受正常响应中的数值。
            JsonNode response = processCpuClient.get(processCpuUrl, JsonNode.class);
            if (response != null && response.path("code").asInt() == 200) {
                JsonNode value = response.path("data");
                if (value.isNumber()) {
                    return value.doubleValue();
                }
            }
        } catch (IllegalStateException | IllegalArgumentException exception) {
            // 采样服务暂时不可达时，用空值表示，接口仍可返回其他指标。
            return null;
        }
        return null;
    }

    /** 读取机器整体CPU使用百分比，无法采样时沿用系统告警的零值口径。 */
    public double readCpuUsage() {
        // 第一步：检查当前运行环境是否支持系统资源采集。
        java.lang.management.OperatingSystemMXBean bean =
                ManagementFactory.getOperatingSystemMXBean();
        if (!(bean instanceof com.sun.management.OperatingSystemMXBean)) {
            return 0D;
        }
        // 第二步：将系统CPU负载换算为百分比，保留两位小数。
        double value = ((com.sun.management.OperatingSystemMXBean) bean)
                .getSystemCpuLoad();
        return Math.round(Math.max(0D, value * 100D) * 100D) / 100D;
    }

    /** 读取机器已使用物理内存占总物理内存的百分比。 */
    public double readMemoryUsage() {
        // 第一步：检查当前运行环境是否支持物理内存采集。
        java.lang.management.OperatingSystemMXBean operatingSystem =
                ManagementFactory.getOperatingSystemMXBean();
        if (!(operatingSystem instanceof com.sun.management.OperatingSystemMXBean)) {
            return 0D;
        }
        // 第二步：按总物理内存和空闲物理内存计算使用百分比。
        com.sun.management.OperatingSystemMXBean bean =
                (com.sun.management.OperatingSystemMXBean) operatingSystem;
        long total = bean.getTotalPhysicalMemorySize();
        return total <= 0L ? 0D : Math.round(
                (total - bean.getFreePhysicalMemorySize()) * 100D / total * 100D) / 100D;
    }
}
