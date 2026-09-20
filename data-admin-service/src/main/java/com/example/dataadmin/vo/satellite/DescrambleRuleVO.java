package com.example.dataadmin.vo.satellite;

/** 解扰规则返回数据。 */
public class DescrambleRuleVO {

    private Integer startPos;
    private Integer length;
    private String feedBack;
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
