package com.example.dataadmin.dto.processing;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/** 按公式编号批量删除，任一编号无效或被引用时整批不删除。 */
public class FormulaBatchDeleteRequest {
    /** 单次提交1到100个公式编号。 */
    @NotNull(message = "公式编号列表不能为空")
    @Size(min = 1, max = 100, message = "单次删除数量必须为1到100个")
    private List<String> codes;

    /** 读取公式编号列表。 */
    public List<String> getCodes() { return codes; }
    /** 设置公式编号列表。 */
    public void setCodes(List<String> codes) { this.codes = codes; }
}
