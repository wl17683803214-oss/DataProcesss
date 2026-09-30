package com.example.dataadmin.vo.processing;

import com.example.dataadmin.enums.TelemetryFilterNodeType;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 逐级查询返回的单个筛选树节点，不附带子节点。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemetryFilterNodeVO {
    /** 当前节点对应的设备、系统或参数主键。 */
    private Long id;
    /** 节点显示名称。 */
    private String label;
    /** 节点类型编码。 */
    private String nodeType;
    /** 节点类型中文名称。 */
    private String nodeTypeName;
    /** 仅参数节点允许勾选。 */
    private boolean selectable;
    /** 参数的持久勾选状态。 */
    private boolean checked;
    /** 是否允许继续展开。 */
    private boolean hasChildren;
    /** 搜索结果所属设备卫星主键。 */
    private Long deviceSatelliteId;
    /** 搜索结果所属设备卫星名称。 */
    private String deviceSatelliteName;
    /** 搜索结果所属系统完整路径；未分类时为空。 */
    private String systemName;

    public static TelemetryFilterNodeVO of(Long id, String label,
            TelemetryFilterNodeType type, boolean checked, boolean hasChildren) {
        TelemetryFilterNodeVO node = new TelemetryFilterNodeVO();
        node.id = id;
        node.label = label;
        node.nodeType = type.getCode();
        node.nodeTypeName = type.getLabel();
        node.selectable = type == TelemetryFilterNodeType.PARAMETER;
        node.checked = checked;
        node.hasChildren = hasChildren;
        return node;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getNodeType() { return nodeType; }
    public String getNodeTypeName() { return nodeTypeName; }
    public boolean isSelectable() { return selectable; }
    public boolean isChecked() { return checked; }
    public boolean isHasChildren() { return hasChildren; }
    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long value) { this.deviceSatelliteId = value; }
    public String getDeviceSatelliteName() { return deviceSatelliteName; }
    public void setDeviceSatelliteName(String value) { this.deviceSatelliteName = value; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String value) { this.systemName = value; }
}
