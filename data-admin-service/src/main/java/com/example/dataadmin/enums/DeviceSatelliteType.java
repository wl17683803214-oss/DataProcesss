package com.example.dataadmin.enums;

/** 设备卫星类型，与其他卫星目录的参数表类型分别管理。 */
public enum DeviceSatelliteType implements LabeledEnum {
    SATELLITE("1", "卫星"), DEVICE("2", "设备");

    /** 数据库存储编码。 */
    private final String value;
    /** 中文名称。 */
    private final String label;

    /** 初始化类型。 */
    DeviceSatelliteType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 返回编码。 */
    public String getValue() { return value; }

    /** 返回中文名称。 */
    public String getLabel() { return label; }

    /** 校验前端选择的类型，禁止未知类型触发替换。 */
    public static void requireValid(String value) {
        if (EnumData.labelOf(values(), value) == null) {
            throw new IllegalArgumentException("类型只能选择1（卫星）或2（设备）");
        }
    }
}
