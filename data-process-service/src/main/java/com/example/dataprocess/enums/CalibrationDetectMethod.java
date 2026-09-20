package com.example.dataprocess.enums;

/** 野值检测方法。 */
public enum CalibrationDetectMethod {
    /** 莱特准则。 */
    WRIGHT(1),
    /** 阈值法。 */
    THRESHOLD(2),
    /** 肖维涅法。 */
    CHAUVENET(3);

    /** 数据库存储值。 */
    private final int value;

    CalibrationDetectMethod(int value) {
        this.value = value;
    }

    /** 按数据库值查找检测方法。 */
    public static CalibrationDetectMethod fromValue(Integer value) {
        for (CalibrationDetectMethod item : values()) {
            if (value != null && item.value == value.intValue()) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的野值检测方法：" + value);
    }
}
