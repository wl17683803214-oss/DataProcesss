-- 公式100：小端无符号整数线性转换。
-- 参数按顺序为系数a、偏移量b，默认a=1、b=0。
return {
    code = "100",
    name = "整数线性转换",
    calculate = function(input)
        -- 保留原码精度，以十进制文本完成整数解码。
        local x = tools.unsigned(input.rawBytes, "LE")
        -- 单个参数只设置系数，与原公式100保持一致。
        local a = input.params[1] or "1"
        local b = input.params[2] or "0"
        -- 精确计算后交给Java转换为双精度消息数值。
        return {
            valueType = "DOUBLE",
            value = tools.linear(x, a, b)
        }
    end
}
