package com.example.dataadmin.enums;

/** 卫星参数表类型。 */
public enum SatelliteTableType implements LabeledEnum {

    TABLE_A("A", "遥测参数表"),
    TABLE_B("B", "帧结构表");

    /** gRPC协议使用的参数表编码。 */
    private final String value;
    /** 前端展示使用的中文名称。 */
    private final String label;

    SatelliteTableType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /** 将接口参数转换为参数表类型。 */
    public static SatelliteTableType fromValue(String value) {
        // 第一步：按协议编码忽略大小写匹配参数表类型。
        for (SatelliteTableType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        // 第二步：无法匹配时返回明确的中文参数提示。
        throw new IllegalArgumentException("参数表类型只能为A或B");
    }
}
