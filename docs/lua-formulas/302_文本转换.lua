-- 公式302：按UTF-8严格解码原始字节，不使用处理参数。
return {
    code = "302",
    name = "UTF-8文本转换",
    calculate = function(input)
        -- 文本要求完整字节，无效编码会产生参数处理告警。
        assert(input.bitWidth % 8 == 0, "文本参数位宽必须是8的整数倍")
        -- 只返回文本，不生成默认数值零。
        return {
            valueType = "TEXT",
            valueText = tools.utf8(input.rawBytes)
        }
    end
}
