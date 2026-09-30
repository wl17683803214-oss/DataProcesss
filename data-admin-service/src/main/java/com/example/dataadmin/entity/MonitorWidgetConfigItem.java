package com.example.dataadmin.entity;

import com.example.dataadmin.enums.BusinessEnums;
import com.example.dataadmin.enums.EnumData;

import java.time.LocalDateTime;

/**
 * 监测页面组件数据项配置表实体。
 *
 * 对应数据库表 monitor_widget_config_item，用于承载业务数据和MyBatis查询结果。
 */
public class MonitorWidgetConfigItem {
    /** 配置项主键ID。 */
    private Long id;
    /** 所属页面组件ID，对应monitor_dashboard_widget.id。 */
    private Long widgetId;
    /** 参数解析配置ID，对应telemetry_parse_rule_config.id。 */
    private Long parseRuleId;
    /** 采集接口ID；参数解析配置自动同步项为空。 */
    private Long interfaceId;
    /** 协议配置ID；参数解析配置自动同步项为空。 */
    private Long protocolConfigId;
    /** 数据项编码，如T001、A001、V001或C001。 */
    private String itemCode;
    /** 数据项名称，如温度、姿态角X、电压或电流。 */
    private String itemName;
    /** 非遥测项直接存储状态；遥测项查询时从场景勾选表计算状态。 */
    private Integer isSelected;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(Long widgetId) {
        this.widgetId = widgetId;
    }

    public Long getParseRuleId() {
        return parseRuleId;
    }

    public void setParseRuleId(Long parseRuleId) {
        this.parseRuleId = parseRuleId;
    }

    public Long getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
    }

    public Long getProtocolConfigId() {
        return protocolConfigId;
    }

    public void setProtocolConfigId(Long protocolConfigId) {
        this.protocolConfigId = protocolConfigId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getIsSelected() {
        return isSelected;
    }

    public void setIsSelected(Integer isSelected) {
        this.isSelected = isSelected;
    }

    /** 返回组件数据项选择状态中文名称。 */
    public String getIsSelectedName() {
        return EnumData.labelOf(BusinessEnums.Selected.values(), isSelected);
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

}
