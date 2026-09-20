package com.example.dataadmin.dto.visualization;

/**
 * 新增或修改可视化组件数据项的请求参数。
 */
public class WidgetItemSaveRequest {

    /** 组件数据项主键 ID；编辑时必填。 */
    private Long id;
    /** 所属可视化组件 ID。 */
    private Long widgetId;
    /** 关联参数解析配置 ID。 */
    private Long parseRuleId;
    /** 关联采集接口 ID；解析参数自动同步项可不传。 */
    private Long interfaceId;
    /** 关联协议配置 ID；解析参数自动同步项可不传。 */
    private Long protocolConfigId;
    /** 数据项编码。 */
    private String itemCode;
    /** 数据项名称。 */
    private String itemName;
    /** 是否在组件中选中：0 否，1 是。 */
    private Integer isSelected;

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
}
