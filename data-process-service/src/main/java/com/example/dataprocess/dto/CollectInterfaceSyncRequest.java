package com.example.dataprocess.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/** Admin通知数据处理服务同步采集接口的请求参数。 */
public class CollectInterfaceSyncRequest {

    /** 当前应该运行的完整采集接口主键列表，空列表表示全部停止。 */
    @NotNull(message = "采集接口主键列表不能为空")
    @Size(max = 1000, message = "单次同步的采集接口不能超过1000个")
    private List<Long> interfaceIds;

    public List<Long> getInterfaceIds() {
        return interfaceIds;
    }

    public void setInterfaceIds(List<Long> interfaceIds) {
        this.interfaceIds = interfaceIds;
    }
}
