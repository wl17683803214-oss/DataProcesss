-- 公式300：小端无符号整数执行y=a*x+b，结果为单精度。
-- 不使用历史处理公式字段，参数按顺序为a、b，默认1、0。
return {
    code = "300",
    name = "单精度线性转换",
    calculate = function(input)
        -- 解码为精确整数文本，不将原始字节直接解释为浮点源码。
        local x = tools.unsigned(input.rawBytes, "LE")
        -- 根据参数位置提供默认系数及偏移量。
        local a = input.params[1] or "1"
        local b = input.params[2] or "0"
        -- 先精确计算，消息输出阶段统一转换单精度并处理小数位。
        return {
            valueType = "FLOAT",
            value = tools.linear(x, a, b)
        }
    end
}
