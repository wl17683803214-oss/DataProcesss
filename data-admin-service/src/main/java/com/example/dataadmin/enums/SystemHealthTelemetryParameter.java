package com.example.dataadmin.enums;

import com.example.dataadmin.entity.SysMonitorSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

/** 健康上报参数定义，发送与参数表使用相同的顺序、位宽和缩放规则。 */
public enum SystemHealthTelemetryParameter implements LabeledEnum {
    /** 注册中心查询的处理服务状态。 */
    SERVICE("SJCL001", "处理服务状态", 8, 1, SystemHealthTelemetryParameter::serviceValue),
    /** 当前管理服务进程的运行秒数。 */
    UPTIME("SJCL002", "管理服务运行时长", 64, 1, SysMonitorSnapshot::getUptimeSeconds),
    /** 已启用的采集接口数量。 */
    ENABLED_INTERFACES("SJCL003", "已启用采集接口数", 32, 1, SysMonitorSnapshot::getOnlineNodeCount),
    /** 未删除的采集接口数量。 */
    TOTAL_INTERFACES("SJCL004", "采集接口总数", 32, 1, SysMonitorSnapshot::getTotalNodeCount),
    /** 数据库连接查询耗时，单位毫秒。 */
    DATABASE("SJCL005", "数据库连接耗时", 32, 1, SysMonitorSnapshot::getDatabaseLatencyMs),
    /** 处理服务最后一次心跳的有效状态。 */
    HEARTBEAT("SJCL006", "处理服务心跳状态", 8, 1, SystemHealthTelemetryParameter::heartbeatValue),
    /** 管理服务所在机器的整体处理器使用率。 */
    CPU("SJCL007", "CPU使用率", 32, 100, SysMonitorSnapshot::getCpuUsagePercent),
    /** 管理服务所在机器的物理内存使用率。 */
    MEMORY("SJCL008", "内存使用率", 32, 100, SysMonitorSnapshot::getMemoryUsagePercent),
    /** 管理服务工作目录所在磁盘的使用率。 */
    DISK("SJCL009", "磁盘使用率", 32, 100, SysMonitorSnapshot::getDiskUsagePercent),
    /** 现有规则计算得到的健康评分。 */
    HEALTH_SCORE("SJCL010", "健康评分", 8, 1, SysMonitorSnapshot::getHealthScore);

    /** 遥测代号。 */
    private final String code;
    /** 中文名称。 */
    private final String label;
    /** 原码位宽。 */
    private final int bitWidth;
    /** 物理量转为整数原码时的倍率。 */
    private final int scale;
    /** 从健康快照提取物理量的函数。 */
    private final Function<SysMonitorSnapshot, Number> reader;

    /** 保存每项参数的固定定义。 */
    SystemHealthTelemetryParameter(String code, String label, int bitWidth, int scale,
            Function<SysMonitorSnapshot, Number> reader) {
        // 固定定义供消息编码与参数表导出共同使用。
        this.code = code;
        this.label = label;
        this.bitWidth = bitWidth;
        this.scale = scale;
        this.reader = reader;
    }

    /** 返回遥测代号。 */
    @Override
    public String getValue() { return code; }
    /** 返回中文名称。 */
    @Override
    public String getLabel() { return label; }
    /** 返回从一开始的表序号。 */
    public int getTableIndex() { return ordinal() + 1; }
    /** 返回原码位宽。 */
    public int getBitWidth() { return bitWidth; }
    /** 返回物理量的原码倍率。 */
    public int getScale() { return scale; }

    /** 提取数值并拒绝缺失数据，避免把采样失败编码成正常零值。 */
    public BigDecimal read(SysMonitorSnapshot snapshot) {
        // 先验证采样结果，再按参数表约定统一小数精度。
        Number value = reader.apply(snapshot);
        if (value == null || !Double.isFinite(value.doubleValue()) || value.doubleValue() < 0) {
            throw new IllegalArgumentException("健康参数采样不可用：" + label);
        }
        BigDecimal result = new BigDecimal(value.toString()).setScale(
                scale == 100 ? 2 : 0, RoundingMode.HALF_UP);
        // 百分比和健康评分必须符合既有业务范围。
        if ((scale == 100 || this == HEALTH_SCORE) && result.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("健康参数超出有效范围：" + label);
        }
        return result;
    }

    /** 复用已有服务状态枚举，映射成约定的遥测数值。 */
    private static Number serviceValue(SysMonitorSnapshot snapshot) {
        // 不改变数据库中的英文状态，仅在遥测编码时换为数字。
        SystemMonitorServiceStatus status = SystemMonitorServiceStatus.valueOf(snapshot.getServiceStatus());
        switch (status) {
            case DOWN: return 0;
            case RUNNING: return 1;
            case ERROR: return 2;
            default: throw new IllegalArgumentException("不支持的处理服务状态");
        }
    }

    /** 复用已有心跳枚举，正常为一，等待为零。 */
    private static Number heartbeatValue(SysMonitorSnapshot snapshot) {
        // 对未识别的状态拒绝发送，避免误标为正常。
        SystemHeartbeatStatus status = SystemHeartbeatStatus.valueOf(snapshot.getHeartbeatStatus());
        return status == SystemHeartbeatStatus.NORMAL ? 1 : 0;
    }

    /** 返回状态参数的中文文本，普通数值返回规范化文本。 */
    public String valueText(SysMonitorSnapshot snapshot, BigDecimal value) {
        // 枚举文本沿用现有中文标签。
        if (this == SERVICE) {
            return SystemMonitorServiceStatus.valueOf(snapshot.getServiceStatus()).getLabel();
        }
        if (this == HEARTBEAT) {
            return SystemHeartbeatStatus.valueOf(snapshot.getHeartbeatStatus()).getLabel();
        }
        return value.toPlainString();
    }
}
