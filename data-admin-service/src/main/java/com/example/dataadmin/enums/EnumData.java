package com.example.dataadmin.enums;

import com.example.dataadmin.vo.EnumOptionVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 统一构造各控制器的枚举查询数据。 */
public final class EnumData {

    /** 工具类不允许实例化。 */
    private EnumData() {
    }

    /** 创建保持字段声明顺序的返回对象。 */
    public static Map<String, List<EnumOptionVO>> create() {
        return new LinkedHashMap<String, List<EnumOptionVO>>();
    }

    /** 向返回对象中加入一个枚举字段。 */
    public static Map<String, List<EnumOptionVO>> add(
            Map<String, List<EnumOptionVO>> data,
            String fieldName,
            LabeledEnum[] values) {
        List<EnumOptionVO> options = new ArrayList<EnumOptionVO>(values.length);
        // 按枚举声明顺序生成下拉选项，保证前端展示顺序稳定。
        for (LabeledEnum value : values) {
            options.add(new EnumOptionVO(value.getValue(), value.getLabel()));
        }
        data.put(fieldName, options);
        return data;
    }

    /** 根据枚举值查询中文名称，无法匹配时返回空值。 */
    public static String labelOf(LabeledEnum[] values, Object target) {
        if (target == null) {
            return null;
        }
        // 统一转为字符串比较，兼容整数和字符串两类枚举编码。
        for (LabeledEnum value : values) {
            if (String.valueOf(value.getValue()).equals(String.valueOf(target))) {
                return value.getLabel();
            }
        }
        return null;
    }
}
