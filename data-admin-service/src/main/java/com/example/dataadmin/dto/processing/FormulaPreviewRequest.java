package com.example.dataadmin.dto.processing;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 公式试算使用报文字节顺序的十六进制文本，不改变正在运行的配置。 */
public class FormulaPreviewRequest {
    /** 已导入的公式编号。 */
    @NotBlank(message = "公式编号不能为空")
    private String code;
    /** 每两个十六进制字符表示一个字节，允许空格分隔。 */
    @NotBlank(message = "原码不能为空")
    @Size(max = 196608, message = "原码文本过长")
    private String rawHex;
    /** 原始字段位宽。 */
    @Min(value = 1, message = "位宽必须为正整数")
    @Max(value = 524288, message = "位宽不能超过524288")
    private int bitWidth;
    /** 斜线分隔的有序数值参数，未传时使用脚本默认规则。 */
    @Size(max = 8192, message = "处理参数不能超过8192个字符")
    private String processParam;

    /** 读取公式编号。 */
    public String getCode() { return code; }
    /** 设置公式编号。 */
    public void setCode(String code) { this.code = code; }
    /** 读取原码。 */
    public String getRawHex() { return rawHex; }
    /** 设置原码。 */
    public void setRawHex(String rawHex) { this.rawHex = rawHex; }
    /** 读取位宽。 */
    public int getBitWidth() { return bitWidth; }
    /** 设置位宽。 */
    public void setBitWidth(int bitWidth) { this.bitWidth = bitWidth; }
    /** 读取有序参数。 */
    public String getProcessParam() { return processParam; }
    /** 设置有序参数。 */
    public void setProcessParam(String processParam) { this.processParam = processParam; }
}
