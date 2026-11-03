package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotBlank;

/** 下发已经保存到本机或共享目录的参数表配置。 */
public class SatelliteTableRequest extends SatelliteIdRequest {
    /** 管理服务与远程服务均可读取的参数表文件地址。 */
    @NotBlank(message = "遥测参数表地址不能为空")
    private String tablePath;

    /** 获取文件地址。 */
    public String getTablePath() { return tablePath; }
    /** 设置文件地址。 */
    public void setTablePath(String tablePath) { this.tablePath = tablePath; }
}
