package com.example.dataadmin.vo.processing;

/** DeviceSatelliteOptionVO对应的业务数据。 */
public class DeviceSatelliteOptionVO {
    /** 主键。 */
    private Long id;
    /** 设备卫星编码。 */
    private String code;
    /** 设备卫星名称。 */
    private String name;
    /** 类型：1卫星，2设备。 */
    private String type;

    /** 获取主键。 */
    public Long getId() {
        return id;
    }

    /** 设置主键。 */
    public void setId(Long id) {
        this.id = id;
    }

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

    /** 获取类型：1卫星，2设备。 */
    public String getType() {
        return type;
    }

    /** 设置类型：1卫星，2设备。 */
    public void setType(String type) {
        this.type = type;
    }
}
