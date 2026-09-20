package com.example.dataadmin.vo.processing;

/** 数据处理页面的已启用采集接口筛选项。 */
public class CollectInterfaceOptionVO {

    /** 采集接口主键，作为数据查询参数。 */
    private Long interfaceId;

    /** 采集接口名称，作为页面展示文本。 */
    private String interfaceName;

    public Long getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
}
