package com.example.dataadmin.dto.calibration;

import com.example.dataadmin.dto.collection.EnabledUpdateRequest;
import javax.validation.constraints.NotBlank;

/** 校准通道启用切换参数，复用已有主键和启用状态校验。 */
public class CalibrationChannelEnabledRequest extends EnabledUpdateRequest {


    /** 兼容既有内部调用，配置及全局记录不再具有任务归属。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getTaskId() { return null; }

    /** 兼容旧调用参数，不接收或保存任务绑定。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setTaskId(String ignored) { }
}
