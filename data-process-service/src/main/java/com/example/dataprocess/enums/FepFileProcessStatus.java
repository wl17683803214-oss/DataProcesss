package com.example.dataprocess.enums;

/** FEP文件接收后的处理状态。 */
public enum FepFileProcessStatus {

    /** 文件已经完整保存到本地。 */
    RECEIVED(1, "已接收"),
    /** 文件已经上传到对象存储。 */
    STORED(2, "已上传"),
    /** 文件完成消息已经发布。 */
    PUBLISHED(3, "已发布");

    /** 数据库存储值。 */
    private final int code;
    /** 中文状态名称。 */
    private final String name;

    FepFileProcessStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** 根据数据库值取得中文状态名称。 */
    public static String getNameByCode(Integer code) {
        // 逐项匹配已有枚举，避免业务代码重复维护中文名称。
        for (FepFileProcessStatus status : values()) {
            if (code != null && status.code == code) {
                return status.name;
            }
        }
        return "未知状态";
    }
}
