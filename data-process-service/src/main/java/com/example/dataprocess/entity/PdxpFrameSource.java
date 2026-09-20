package com.example.dataprocess.entity;

/** PDXP本地处理关联的设备卫星编码和工作表名称。 */
public class PdxpFrameSource {

    /** 设备卫星编码。 */
    private String code;
    /** 导入工作表对应的设备卫星名称。 */
    private String name;

    /** 获取设备卫星编码。 */
    public String getCode() {
        return code;
    }

    /** 设置设备卫星编码。 */
    public void setCode(String code) {
        this.code = code;
    }

    /** 获取设备卫星名称。 */
    public String getName() {
        return name;
    }

    /** 设置设备卫星名称。 */
    public void setName(String name) {
        this.name = name;
    }
}
