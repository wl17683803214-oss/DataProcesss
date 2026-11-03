package com.example.dataadmin.vo.satellite;

import com.example.dataadmin.entity.SatelliteConfig;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 列表省略配置原文，启用状态返回中文，并提供英文值供编辑回填。 */
public class SatelliteInfoVO extends SatelliteConfig {
    /** 编辑时使用的英文枚举值。 */
    private String enableValue;

    /** 获取英文状态。 */
    public String getEnableValue() { return enableValue; }
    /** 设置英文状态。 */
    public void setEnableValue(String enableValue) { this.enableValue = enableValue; }

    /** 配置原文仅在已上传文件的详情中返回。 */
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getJsonContent() {
        return super.getJsonContent();
    }
}
