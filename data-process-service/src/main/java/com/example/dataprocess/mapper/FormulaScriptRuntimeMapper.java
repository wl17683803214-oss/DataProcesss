package com.example.dataprocess.mapper;

import com.example.common.formula.FormulaScript;
import java.util.List;

/** 仅在配置加载阶段读取公式，不在逐帧计算时查询数据库。 */
public interface FormulaScriptRuntimeMapper {
    /** 每个编号只读取当前唯一的已导入脚本。 */
    List<FormulaScript> findAll();
}
