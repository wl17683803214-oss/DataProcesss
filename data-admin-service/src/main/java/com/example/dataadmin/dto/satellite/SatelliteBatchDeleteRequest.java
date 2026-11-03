package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/** 按本地主键批量删除，任一记录不存在时整批回滚。 */
public class SatelliteBatchDeleteRequest {
    /** 待删除的卫星主键，不对卫星代号做去重。 */
    @NotEmpty(message = "卫星主键列表不能为空")
    private List<Long> ids;

    /** 获取主键列表。 */
    public List<Long> getIds() { return ids; }
    /** 设置主键列表。 */
    public void setIds(List<Long> ids) { this.ids = ids; }
}
