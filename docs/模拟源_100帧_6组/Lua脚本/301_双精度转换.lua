-- 公式301：将小端无符号整数转换为双精度，不使用处理参数。
return {
    code = "301",
    name = "双精度数值转换",
    calculate = function(input)
        -- 先返回精确整数文本，Java输出数值时再按双精度转换。
        return {
            valueType = "DOUBLE",
            value = tools.unsigned(input.rawBytes, "LE")
        }
    end
}
