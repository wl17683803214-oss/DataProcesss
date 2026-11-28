-- 公式304：将16位小端宽字符按UTF-16LE解码为文本。
-- 位宽为整个文本字段的位数，须为16的整数倍，处理参数留空。
-- 遇到双字节零结束符停止读取，支持中文及代理对表示的表情字符。
return {
    code = "304",
    name = "16位小端宽字符转换",
    calculate = function(input)
        -- 第一步：校验字段由完整的双字节编码单元组成。
        assert(input.bitWidth % 16 == 0, "公式304要求位宽为16的整数倍")
        local raw = input.rawBytes
        local output = {}
        local count = 0
        local index = 1

        -- 第二步：输出UTF-8字节，复用现有文本解码工具。
        local function append(value)
            count = count + 1
            output[count] = value
        end

        -- 第三步：逐个读取小端编码单元，代理对组合为完整字符。
        while index <= #raw do
            local value = raw[index] + raw[index + 1] * 256
            index = index + 2
            if value == 0 then
                break
            end
            if value >= 55296 and value <= 56319 then
                assert(index + 1 <= #raw, "公式304遇到不完整的UTF-16代理对")
                local low = raw[index] + raw[index + 1] * 256
                assert(low >= 56320 and low <= 57343, "公式304遇到无效的UTF-16代理对")
                value = 65536 + (value - 55296) * 1024 + low - 56320
                index = index + 2
            else
                assert(value < 56320 or value > 57343, "公式304遇到孤立的UTF-16低代理项")
            end

            -- 第四步：按字符范围编码为一至四个UTF-8字节。
            if value < 128 then
                append(value)
            elseif value < 2048 then
                append(192 + math.floor(value / 64))
                append(128 + value % 64)
            elseif value < 65536 then
                append(224 + math.floor(value / 4096))
                append(128 + math.floor(value / 64) % 64)
                append(128 + value % 64)
            else
                append(240 + math.floor(value / 262144))
                append(128 + math.floor(value / 4096) % 64)
                append(128 + math.floor(value / 64) % 64)
                append(128 + value % 64)
            end
        end

        -- 第五步：返回文本结果，空字符串同样有效。
        return {
            valueType = "TEXT",
            valueText = count == 0 and "" or tools.utf8(output)
        }
    end
}
