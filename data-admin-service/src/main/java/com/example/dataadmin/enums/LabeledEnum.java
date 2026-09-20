package com.example.dataadmin.enums;

/** 为前端枚举选项统一提供取值和中文名称。 */
public interface LabeledEnum {

    /** 返回接口传输和数据库保存使用的枚举值。 */
    Object getValue();

    /** 返回前端展示使用的中文名称。 */
    String getLabel();
}
