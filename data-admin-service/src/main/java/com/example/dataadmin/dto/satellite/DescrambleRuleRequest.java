package com.example.dataadmin.dto.satellite;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 解扰规则请求参数。 */
public class DescrambleRuleRequest {

    /** 解扰起始位置。 */
    @NotNull(message = "解扰起始位置不能为空")
    @Min(value = 0, message = "解扰起始位置不能小于0")
    private Integer startPos;
    /** 解扰长度。 */
    @NotNull(message = "解扰长度不能为空")
    @Min(value = 1, message = "解扰长度必须大于0")
    private Integer length;
    /** 解扰多项式。 */
    @NotBlank(message = "解扰多项式不能为空")
    private String feedBack;
    /** 移位寄存器是否使用全1初始值。 */
    @NotNull(message = "解扰初始值不能为空")
    private Boolean initialValue;

    public Integer getStartPos() { return startPos; }
    public void setStartPos(Integer startPos) { this.startPos = startPos; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
    public String getFeedBack() { return feedBack; }
    public void setFeedBack(String feedBack) { this.feedBack = feedBack; }
    public Boolean getInitialValue() { return initialValue; }
    public void setInitialValue(Boolean initialValue) { this.initialValue = initialValue; }
}
