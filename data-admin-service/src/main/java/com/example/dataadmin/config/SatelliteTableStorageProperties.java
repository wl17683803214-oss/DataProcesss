package com.example.dataadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 参数表文件保存在本地或双方挂载路径一致的共享目录。 */
@Component
@ConfigurationProperties(prefix = "satellite-management.storage")
public class SatelliteTableStorageProperties {
    /** 参数表保存目录，运行时转换为绝对路径后传给远程服务。 */
    private String tableDirectory = "./data/satellite-tables";

    /** 获取保存目录。 */
    public String getTableDirectory() { return tableDirectory; }
    /** 设置保存目录。 */
    public void setTableDirectory(String tableDirectory) { this.tableDirectory = tableDirectory; }
}
