package com.example.dataadmin.enums;

import com.example.dataadmin.vo.EnumOptionVO;

import java.util.List;
import java.util.Map;

/** 按控制器整理其页面涉及的全部固定枚举。 */
public final class ControllerEnumData {

    /** 工具类不允许实例化。 */
    private ControllerEnumData() {
    }

    /** 返回不包含固定枚举的空数据。 */
    public static Map<String, List<EnumOptionVO>> empty() {
        return EnumData.create();
    }

    /** 返回试验任务模块枚举。 */
    public static Map<String, List<EnumOptionVO>> experimentTask() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "taskStatus", ExperimentTaskStatus.values());
        EnumData.add(data, "endReason", TaskEndReason.values());
        return data;
    }

    /** 返回首页总览模块枚举。 */
    public static Map<String, List<EnumOptionVO>> dashboard() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "range", DashboardRange.values());
        EnumData.add(data, "dataType", BusinessEnums.ProcessingDataType.values());
        EnumData.add(data, "status", BusinessEnums.InterfaceStatus.values());
        return data;
    }

    /** 返回采集管理模块枚举。 */
    public static Map<String, List<EnumOptionVO>> collection() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "interfaceType", BusinessEnums.InterfaceType.values());
        EnumData.add(data, "transferType", BusinessEnums.TransferType.values());
        EnumData.add(data, "transferProtocol", BusinessEnums.TransferProtocol.values());
        EnumData.add(data, "protocolType", BusinessEnums.ProtocolType.values());
        EnumData.add(data, "dataSourceType", BusinessEnums.DataSourceType.values());
        EnumData.add(data, "status", BusinessEnums.InterfaceStatus.values());
        EnumData.add(data, "enabled", BusinessEnums.Enabled.values());
        return data;
    }

    /** 返回处理管理模块枚举。 */
    public static Map<String, List<EnumOptionVO>> processing() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "taskStatus", BusinessEnums.ProcessTaskStatus.values());
        EnumData.add(data, "logLevel", DataProcessLogLevel.values());
        EnumData.add(data, "ruleEnabled", BusinessEnums.Enabled.values());
        EnumData.add(data, "storeFlag", BusinessEnums.YesNo.values());
        EnumData.add(data, "alarmFlag", BusinessEnums.YesNo.values());
        EnumData.add(data, "deviceSatelliteType", DeviceSatelliteType.values());
        EnumData.add(data, "selectionType", BusinessEnums.SelectionType.values());
        return data;
    }

    /** 返回卫星目录管理模块枚举。 */
    public static Map<String, List<EnumOptionVO>> satelliteManagement() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "tableType", SatelliteTableType.values());
        return data;
    }

    /** 返回校准管理模块枚举。 */
    public static Map<String, List<EnumOptionVO>> calibration() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "channelType", BusinessEnums.ChannelType.values());
        EnumData.add(data, "detectMethod", BusinessEnums.DetectMethod.values());
        EnumData.add(data, "dynamicUpdate", BusinessEnums.YesNo.values());
        EnumData.add(data, "autoClean", BusinessEnums.YesNo.values());
        EnumData.add(data, "enabled", BusinessEnums.Enabled.values());
        EnumData.add(data, "channelStatus", BusinessEnums.ChannelStatus.values());
        EnumData.add(data, "isOutlier", BusinessEnums.YesNo.values());
        return data;
    }

    /** 返回告警管理模块枚举。 */
    public static Map<String, List<EnumOptionVO>> alarm() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "dataAlarmLevel", BusinessEnums.DataAlarmLevel.values());
        EnumData.add(data, "dataAlarmStatus", BusinessEnums.DataAlarmStatus.values());
        EnumData.add(data, "systemAlarmLevel", BusinessEnums.SystemAlarmLevel.values());
        EnumData.add(data, "systemAlarmStatus", BusinessEnums.SystemAlarmStatus.values());
        return data;
    }

    /** 返回可视化模块枚举。 */
    public static Map<String, List<EnumOptionVO>> visualization() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "widgetType", BusinessEnums.WidgetType.values());
        EnumData.add(data, "enabled", BusinessEnums.Visible.values());
        EnumData.add(data, "isSelected", BusinessEnums.Selected.values());
        EnumData.add(data, "selectionType", BusinessEnums.SelectionType.values());
        return data;
    }

    /** 返回用户模块枚举。 */
    public static Map<String, List<EnumOptionVO>> user() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "status", BusinessEnums.Enabled.values());
        return data;
    }

    /** 返回角色与菜单模块枚举。 */
    public static Map<String, List<EnumOptionVO>> role() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "status", BusinessEnums.Enabled.values());
        return data;
    }

    /** 返回系统日志模块枚举。 */
    public static Map<String, List<EnumOptionVO>> systemLog() {
        Map<String, List<EnumOptionVO>> data = EnumData.create();
        EnumData.add(data, "logLevel", SystemLogLevel.values());
        return data;
    }
}
