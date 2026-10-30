package com.example.dataprocess.collection;

import com.example.common.formula.FormulaScript;
import com.example.common.formula.LuaFormulaEngine;
import com.example.dataprocess.mapper.FormulaScriptRuntimeMapper;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 配置同步时创建新的只读公式快照，正在执行的帧继续使用旧快照。 */
@Component
public class FormulaRuntimeLoader {
    private final FormulaScriptRuntimeMapper mapper;

    /** 注入运行脚本查询组件。 */
    public FormulaRuntimeLoader(FormulaScriptRuntimeMapper mapper) { this.mapper = mapper; }

    /** 整批编译成功才发布，失败时管理器不会替换现有运行配置。 */
    public Map<String, LuaFormulaEngine.Compiled> load() {
        Map<String, LuaFormulaEngine.Compiled> compiled = new LinkedHashMap<>();
        for (FormulaScript script : mapper.findAll()) {
            LuaFormulaEngine.Compiled formula = LuaFormulaEngine.compile(script.getSource());
            if (!script.getCode().equals(formula.getCode())) {
                throw new IllegalArgumentException("公式存储编号与脚本声明不一致：" + script.getCode());
            }
            compiled.put(script.getCode(), formula);
        }
        return Collections.unmodifiableMap(compiled);
    }
}
