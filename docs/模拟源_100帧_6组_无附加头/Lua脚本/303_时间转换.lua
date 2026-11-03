-- 公式303：小端无符号整数作为十分之一毫秒计数。
-- 时间起点为2000-01-01 12:00:00，不进行时区偏移。
return {
    code = "303",
    name = "时间计数转换",
    calculate = function(input)
        -- 以精确整数文本保存计数，避免Lua浮点数提前损失精度。
        local ticks = tools.unsigned(input.rawBytes, "LE")
        -- 脚本显式选择时间工具，Java不再按公式编号硬编码时间行为。
        return {
            valueType = "TIME",
            value = ticks,
            valueText = tools.timeText(ticks)
        }
    end
}
