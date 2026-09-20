package com.example.dataprocess.tool;

/** 按字节低位到高位连续读取，字段最低位先到，允许跨字节。 */
public final class LittleEndianBitReader {

    /** 待读取的原始数据。 */
    private final byte[] data;
    /** 下一位的绝对位置。 */
    private long position;

    /** 创建当前帧独立使用的读取器。 */
    public LittleEndianBitReader(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("遥测数据不能为空");
        }
        this.data = data;
    }

    /** 返回低位对齐的小端字段原码，末字节未使用的高位补零。 */
    public byte[] read(int bitWidth) {
        // 先校验剩余位数，失败时保持游标不变。
        if (bitWidth <= 0 || bitWidth > data.length * 8L - position) {
            throw new IllegalArgumentException("参数位宽无效或遥测数据不足，位偏移："
                    + position + "，位宽：" + bitWidth);
        }
        byte[] raw = new byte[(int) ((bitWidth + 7L) / 8)];
        // 将源数据中的连续位依次放到结果的低位，跨字节也不跳过填充位。
        for (int bit = 0; bit < bitWidth; bit++) {
            long source = position + bit;
            int value = (data[(int) (source / 8)] >>> (int) (source % 8)) & 1;
            raw[bit / 8] |= value << (bit % 8);
        }
        position += bitWidth;
        return raw;
    }
}
