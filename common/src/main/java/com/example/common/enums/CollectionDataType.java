package com.example.common.enums;

/** 采集数据类型，沿用原处理方式的数字编码并增加指令响应。 */
public enum CollectionDataType {
    /** 设备遥测使用本地参数解析。 */
    DEVICE(0, "设备"),
    /** 卫星遥测调用远程处理服务。 */
    SATELLITE(1, "卫星"),
    /** 指令响应按固定协议组装后发送消息。 */
    COMMAND_RESPONSE(2, "指令响应");

    /** 数据库存储及接口提交的兼容编码。 */
    private final int code;
    /** 查询和枚举接口返回的中文名称。 */
    private final String label;

    /** 保存编码与中文名称。 */
    CollectionDataType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 读取数字编码。 */
    public int getCode() {
        return code;
    }

    /** 读取中文名称。 */
    public String getLabel() {
        return label;
    }

    /** 旧请求未提交类型时沿用设备处理，其他编码必须明确有效。 */
    public static CollectionDataType require(Integer code) {
        // 第一步：兼容历史接口省略处理方式的调用。
        int normalizedCode = code == null ? DEVICE.code : code;
        // 第二步：集中验证三种业务类型，避免调用方误用启用开关枚举。
        for (CollectionDataType type : values()) {
            if (type.code == normalizedCode) {
                return type;
            }
        }
        throw new IllegalArgumentException("采集数据类型只能为0（设备）、1（卫星）或2（指令响应）");
    }
}
