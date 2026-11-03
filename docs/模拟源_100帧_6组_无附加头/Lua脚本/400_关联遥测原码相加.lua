-- 公式400：将处理参数指定的两个同帧遥测原码相加。
-- 处理参数示例：TM002/TM003，按小端无符号整数解释两项原码。
-- 缺少代号或当前帧没有对应原码时由Java报告参数处理失败，不补零。
return {
    code = "400",
    name = "两个关联遥测原码相加",
    -- 类型声明在加载脚本时读取，Java替换参数后才调用计算入口。
    parameterTypes = {
        "telemetry_raw",
        "telemetry_raw"
    },
    calculate = function(input)
        -- 参数必须恰好为两个已解析的关联原码，不能使用当前遥测原码兜底。
        assert(#input.params == 2, "公式400必须配置两个关联遥测代号")
        -- 精确计算第一个原码乘以1再加第二个，避免Lua双精度提前丢失整数精度。
        return {
            valueType = "DOUBLE",
            value = tools.linear(input.params[1], "1", input.params[2])
        }
    end
}
