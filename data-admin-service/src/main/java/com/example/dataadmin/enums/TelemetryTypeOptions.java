package com.example.dataadmin.enums;

import SatDataCenter.DataExchange.TmTc.TelemetryMessages.TelemetryType;
import com.example.dataadmin.vo.EnumOptionVO;

import java.util.ArrayList;
import java.util.List;

/** 复用协议遥测类型，统一筛选校验和前端中文选项。 */
public final class TelemetryTypeOptions {

    /** 工具类不允许实例化。 */
    private TelemetryTypeOptions() {
    }

    /** 空白表示查询全部，其他值必须是协议定义的有效英文枚举名。 */
    public static String normalize(String value) {
        // 第一步：统一可选筛选参数的空白语义。
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // 第二步：复用协议枚举校验，排除协议解析失败的占位值。
        try {
            TelemetryType type = TelemetryType.valueOf(value.trim());
            if (type != TelemetryType.UNRECOGNIZED) {
                return type.name();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("遥测类型不正确");
        }
        throw new IllegalArgumentException("遥测类型不正确");
    }

    /** 按协议顺序提供英文值与中文名称，不重复定义业务枚举。 */
    public static List<EnumOptionVO> options() {
        List<EnumOptionVO> options = new ArrayList<>();
        // 只向前端提供协议可存储的类型，未指定类型也可以精确筛选。
        for (TelemetryType type : TelemetryType.values()) {
            if (type != TelemetryType.UNRECOGNIZED) {
                options.add(new EnumOptionVO(type.name(), label(type)));
            }
        }
        return options;
    }

    /** 返回协议遥测类型的中文显示名称。 */
    private static String label(TelemetryType type) {
        switch (type) {
            case TM_TYPE_UNSPECIFIED:
                return "未指定";
            case TM_TYPE_REAL_TM:
                return "实时遥测";
            case TM_TYPE_DELAY_TM:
                return "延时遥测";
            case TM_TYPE_MEMORY_TM:
                return "存储遥测";
            case TM_TYPE_DEVICE_TM:
                return "设备遥测";
            default:
                throw new IllegalArgumentException("遥测类型不正确");
        }
    }
}
