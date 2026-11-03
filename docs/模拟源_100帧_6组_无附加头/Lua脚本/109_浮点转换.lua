-- 公式109：对应小端机器上原公式将源码前4字节反转后按浮点解释。
-- 输入字节保持报文顺序；直接按大端浮点解码等价，不能再额外反转一次。
-- 至少两个参数才使用k和b；不足两个时均用默认值，与原公式109一致。
return {
    code = "109",
    name = "浮点源码线性转换",
    calculate = function(input)
        -- 单精度源码要求正好32位。
        assert(input.bitWidth == 32, "公式109要求位宽为32")
        -- 按原公式约定解释4字节，例：3F 80 00 00得到1.0。
        local x = tools.float32(input.rawBytes, "BE")
        -- 一个参数也不覆盖默认值，保留原公式的特殊规则。
        local k = "1"
        local b = "0"
        if #input.params >= 2 then
            k = input.params[1]
            b = input.params[2]
        end
        -- 与原公式一样使用双精度系数计算，最终赋回单精度。
        local y = x * tonumber(k) + tonumber(b)
        return {
            valueType = "FLOAT",
            value = tools.toFloat32(y)
        }
    end
}
