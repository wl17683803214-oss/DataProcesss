package com.example.dataadmin.vo;

/** 前端下拉框使用的枚举选项。 */
public class EnumOptionVO {

    /** 接口传输和数据库保存使用的枚举值。 */
    private final Object value;

    /** 前端展示使用的中文名称。 */
    private final String label;

    /** 使用枚举值和中文名称创建选项。 */
    public EnumOptionVO(Object value, String label) {
        this.value = value;
        this.label = label;
    }

    /** 返回枚举值。 */
    public Object getValue() {
        return value;
    }

    /** 返回中文名称。 */
    public String getLabel() {
        return label;
    }
}
