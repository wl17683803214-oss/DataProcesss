package com.example.dataadmin.dto.collection;

import java.util.List;

/** 管理服务向数据处理服务发送的采集接口同步请求。 */
public class CollectInterfaceSyncRequest {

    /** 当前全部启用且未删除的采集接口主键。 */
    private List<Long> interfaceIds;

    public CollectInterfaceSyncRequest(List<Long> interfaceIds) {
        this.interfaceIds = interfaceIds;
    }

    public List<Long> getInterfaceIds() {
        return interfaceIds;
    }

    public void setInterfaceIds(List<Long> interfaceIds) {
        this.interfaceIds = interfaceIds;
    }
}
