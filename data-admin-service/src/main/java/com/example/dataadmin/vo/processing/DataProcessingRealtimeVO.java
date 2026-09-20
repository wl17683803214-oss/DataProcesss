package com.example.dataadmin.vo.processing;

/** 数据处理页面实时监控指标。 */
public class DataProcessingRealtimeVO {

    /** 当前处理速率，返回值直接包含条/秒单位。 */
    private String processRate = "0 条/秒";

    /** 数据处理服务CPU使用率，返回值直接包含百分号。 */
    private String cpuUsage = "0.00%";

    /** 数据处理服务JVM已使用内存，返回值直接包含GB单位。 */
    private String memoryUsage = "0.00 GB";

    public String getProcessRate() {
        return processRate;
    }

    public void setProcessRate(String processRate) {
        this.processRate = processRate;
    }

    public String getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(String cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public String getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(String memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
}
