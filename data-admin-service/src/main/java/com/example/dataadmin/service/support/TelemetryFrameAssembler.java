package com.example.dataadmin.service.support;

import com.example.common.enums.TelemetryFrameStatus;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.greptime.TelemetryIdentity;
import com.example.dataadmin.entity.InvalidTelemetryFrame;
import com.example.dataadmin.vo.processing.RealtimeTelemetryFrameVO;
import java.util.ArrayList;
import java.util.List;

/** 历史与缓存整帧共用的页面字段组装，检查结果统一返回中文。 */
public final class TelemetryFrameAssembler {
    /** 使用固定数字表转换原码，避免逐字节创建格式化对象。 */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private TelemetryFrameAssembler() {
    }

    /** 正常帧保留原有时间字段和设备身份约定。 */
    public static List<RealtimeTelemetryFrameVO> normal(List<TelemetryFrameRecord> rows) {
        List<RealtimeTelemetryFrameVO> result = new ArrayList<>(rows.size());
        for (TelemetryFrameRecord row : rows) {
            // 从完整字节组装原码和长度，远程卫星不伪造本地设备主键。
            RealtimeTelemetryFrameVO frame = new RealtimeTelemetryFrameVO();
            frame.setDeviceSatelliteId(TelemetryIdentity.deviceId(row.getSourceType(), row.getSourceIdentity()));
            frame.setTime(row.getSourceTime() == null ? null : row.getSourceTime().toLocalDateTime());
            frame.setDeviceSatelliteName(row.getDeviceSatelliteName());
            frame.setChannelName(row.getChannelName());
            frame.setRawLength(row.getRaw() == null ? 0 : row.getRaw().length);
            frame.setRawFrame(toHex(row.getRaw()));
            frame.setCheckResultName(TelemetryFrameStatus.labelOf(row.getCheckStatus()));
            result.add(frame);
        }
        return result;
    }

    /** 异常帧沿用receiveTime字段，正常和异常快照不能混用。 */
    public static List<InvalidTelemetryFrame> abnormal(List<TelemetryFrameRecord> rows) {
        List<InvalidTelemetryFrame> result = new ArrayList<>(rows.size());
        for (TelemetryFrameRecord row : rows) {
            // 保留原有响应字段，枚举编码只在存储和缓存中使用。
            InvalidTelemetryFrame frame = new InvalidTelemetryFrame();
            frame.setDeviceSatelliteId(TelemetryIdentity.deviceId(row.getSourceType(), row.getSourceIdentity()));
            frame.setReceiveTime(row.getSourceTime() == null ? null : row.getSourceTime().toLocalDateTime());
            frame.setDeviceSatelliteName(row.getDeviceSatelliteName());
            frame.setChannelName(row.getChannelName());
            frame.setRawLength(row.getRaw() == null ? 0 : row.getRaw().length);
            frame.setRawFrame(toHex(row.getRaw()));
            frame.setCheckResultName(TelemetryFrameStatus.labelOf(row.getCheckStatus()));
            result.add(frame);
        }
        return result;
    }

    /** 保持大写十六进制和单空格分隔，与原有数据库接口输出一致。 */
    private static String toHex(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        char[] output = new char[data.length * 3 - 1];
        for (int index = 0; index < data.length; index++) {
            int value = data[index] & 0xFF;
            output[index * 3] = HEX[value >>> 4];
            output[index * 3 + 1] = HEX[value & 0x0F];
            if (index < data.length - 1) {
                output[index * 3 + 2] = ' ';
            }
        }
        return new String(output);
    }
}
