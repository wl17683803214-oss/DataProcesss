package com.example.dataadmin.service.impl;

import com.example.dataadmin.config.SatelliteManagementRpcProperties;
import com.example.dataadmin.dto.satellite.ChannelEnabledRequest;
import com.example.dataadmin.dto.satellite.DescrambleRuleRequest;
import com.example.dataadmin.dto.satellite.SatelliteEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteInfoRequest;
import com.example.dataadmin.dto.satellite.SatelliteUpdateRequest;
import com.example.dataadmin.dto.satellite.TmChannelRequest;
import com.example.dataadmin.enums.SatelliteTableType;
import com.example.dataadmin.exception.SatelliteManagementException;
import com.example.dataadmin.grpc.SatelliteManagementGrpcClient;
import com.example.dataadmin.service.SatelliteManagementService;
import com.example.dataadmin.vo.satellite.DescrambleRuleVO;
import com.example.dataadmin.vo.satellite.SatelliteInfoVO;
import com.example.dataadmin.vo.satellite.TmChannelVO;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import tm.processing.v1.Tm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 卫星目录管理服务实现。 */
@Service
public class SatelliteManagementServiceImpl
        implements SatelliteManagementService {

    /** 十六进制通道代号格式。 */
    private static final String CHANNEL_ID_PATTERN = "^[0-9A-Fa-f]+$";
    /** 卫星目录管理远程客户端。 */
    private final SatelliteManagementGrpcClient grpcClient;
    /** 参数表上传允许的最大字节数。 */
    private final int maxUploadFileSize;

    public SatelliteManagementServiceImpl(
            SatelliteManagementGrpcClient grpcClient,
            SatelliteManagementRpcProperties properties) {
        this.grpcClient = grpcClient;
        this.maxUploadFileSize = properties.getMaxUploadFileSize();
    }

    @Override
    public List<SatelliteInfoVO> listSatellites() {
        // 第一步：调用远程服务查询卫星完整配置列表。
        Tm.ListSatellitesResponse response = grpcClient.listSatellites();
        requireResponseSuccess(response.getCode(), response.getMessage());
        // 第二步：逐项转换成不暴露Protobuf类型的前端返回对象。
        List<SatelliteInfoVO> result = new ArrayList<SatelliteInfoVO>();
        for (Tm.SatelliteInfo satellite : response.getSatellitesList()) {
            result.add(toSatelliteInfoVO(satellite));
        }
        return result;
    }

    @Override
    public SatelliteInfoVO getSatellite(String code) {
        // 第一步：按卫星代号构造详情查询请求。
        Tm.GetSatelliteRequest request = Tm.GetSatelliteRequest.newBuilder()
                .setCode(requireText(code, "卫星代号不能为空"))
                .build();
        // 第二步：校验远程结果并转换卫星详情。
        Tm.GetSatelliteResponse response = grpcClient.getSatellite(request);
        requireResponseSuccess(response.getCode(), response.getMessage());
        if (!response.hasSatellite()) {
            throw new SatelliteManagementException(404, "未找到指定的卫星配置");
        }
        return toSatelliteInfoVO(response.getSatellite());
    }

    @Override
    public void createSatellite(SatelliteInfoRequest request) {
        // 将完整前端配置转换为创建卫星请求后转发。
        grpcClient.createSatellite(Tm.CreateSatelliteRequest.newBuilder()
                .setInfo(toSatelliteInfo(request))
                .build());
    }

    @Override
    public void updateSatellite(SatelliteUpdateRequest request) {
        // 同时传递原卫星代号和完整新配置，支持卫星代号变更。
        grpcClient.updateSatellite(Tm.UpdateSatelliteRequest.newBuilder()
                .setCode(request.getCode().trim())
                .setInfo(toSatelliteInfo(request.getInfo()))
                .build());
    }

    @Override
    public void deleteSatellite(String code) {
        // 按卫星代号调用远程删除接口。
        grpcClient.deleteSatellite(Tm.DeleteSatelliteRequest.newBuilder()
                .setCode(requireText(code, "卫星代号不能为空"))
                .build());
    }

    @Override
    public void setSatelliteEnabled(SatelliteEnabledRequest request) {
        // 转发卫星目标启用状态。
        grpcClient.setSatelliteEnabled(Tm.SetEnabledRequest.newBuilder()
                .setCode(request.getCode().trim())
                .setEnable(request.getEnable())
                .build());
    }

    @Override
    public void setChannelEnabled(ChannelEnabledRequest request) {
        // 第一步：统一校验并规范化十六进制通道代号。
        String channelId = normalizeChannelId(request.getChannelId());
        // 第二步：转发通道目标启用状态。
        grpcClient.setChannelEnabled(Tm.SetChannelEnabledRequest.newBuilder()
                .setSatCode(request.getSatelliteCode().trim())
                .setChannelId(channelId)
                .setEnable(request.getEnable())
                .build());
    }

    @Override
    public void uploadTable(
            String code,
            SatelliteTableType tableType,
            String fileName,
            byte[] content) {
        // 第一步：校验文件基础信息和配置的大小限制。
        String normalizedCode = requireText(code, "卫星代号不能为空");
        String normalizedFileName = validateFileName(fileName);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("参数表文件不能为空");
        }
        if (content.length > maxUploadFileSize) {
            throw new IllegalArgumentException("参数表文件超过允许的最大大小");
        }
        // 第二步：将文件字节直接转发给远程卫星管理服务。
        grpcClient.uploadTable(Tm.UploadTableRequest.newBuilder()
                .setCode(normalizedCode)
                .setTable(tableType.getValue())
                .setFileName(normalizedFileName)
                .setContent(ByteString.copyFrom(content))
                .build());
    }

    /** 将前端卫星完整配置转换为Protobuf对象。 */
    private Tm.SatelliteInfo toSatelliteInfo(SatelliteInfoRequest request) {
        // 第一步：转换卫星基础信息和可选路径字段。
        Tm.SatelliteInfo.Builder builder = Tm.SatelliteInfo.newBuilder()
                .setName(request.getName().trim())
                .setCode(request.getCode().trim())
                .setEnable(request.getEnable())
                .setTableA(emptyIfNull(request.getTableA()))
                .setTableB(emptyIfNull(request.getTableB()))
                .setLuaPath(emptyIfNull(request.getLuaPath()))
                .setTmLen(request.getTmLen());
        // 第二步：存在解扰规则时追加规则配置。
        if (request.getDescrambleRule() != null) {
            builder.setDescrambleRule(
                    toDescrambleRule(request.getDescrambleRule()));
        }
        // 第三步：校验并转换完整通道列表。
        putChannels(builder, request.getTmChannels());
        return builder.build();
    }

    /** 将解扰规则请求转换为Protobuf对象。 */
    private Tm.DescrambleRule toDescrambleRule(
            DescrambleRuleRequest request) {
        return Tm.DescrambleRule.newBuilder()
                .setStartPos(request.getStartPos())
                .setLength(request.getLength())
                .setFeedBack(request.getFeedBack().trim())
                .setInitialValue(request.getInitialValue())
                .build();
    }

    /** 向卫星配置写入通道Map。 */
    private void putChannels(
            Tm.SatelliteInfo.Builder builder,
            List<TmChannelRequest> channels) {
        Set<String> channelIds = new HashSet<String>();
        if (channels == null) {
            return;
        }
        // 按通道代号生成tm前缀键，并拒绝同一请求中的重复通道。
        for (TmChannelRequest channel : channels) {
            String channelId = normalizeAndRequireUnique(
                    channel.getChannelId(), channelIds);
            builder.putTmChannels(
                    "tm." + channelId,
                    toTmChannel(channel, channelId));
        }
    }

    /** 将单个通道请求转换为Protobuf对象。 */
    private Tm.TmChannel toTmChannel(
            TmChannelRequest request,
            String channelId) {
        return Tm.TmChannel.newBuilder()
                .setName(request.getName().trim())
                .setEnable(request.getEnable())
                .setChannelId(channelId)
                .setToDescramble(request.getToDescramble())
                .setTmSyncHead(request.getTmSyncHead().trim())
                .build();
    }

    /** 将远程卫星配置转换为前端返回对象。 */
    private SatelliteInfoVO toSatelliteInfoVO(Tm.SatelliteInfo source) {
        // 第一步：转换卫星基础字段。
        SatelliteInfoVO target = new SatelliteInfoVO();
        target.setName(source.getName());
        target.setCode(source.getCode());
        target.setEnable(source.getEnable());
        target.setTableA(source.getTableA());
        target.setTableB(source.getTableB());
        target.setLuaPath(source.getLuaPath());
        target.setTmLen(source.getTmLen());
        // 第二步：仅在远程结果包含规则时返回解扰规则。
        if (source.hasDescrambleRule()) {
            target.setDescrambleRule(
                    toDescrambleRuleVO(source.getDescrambleRule()));
        }
        // 第三步：将协议Map转换成前端便于遍历的通道数组。
        List<TmChannelVO> channels = new ArrayList<TmChannelVO>();
        for (Map.Entry<String, Tm.TmChannel> entry
                : source.getTmChannelsMap().entrySet()) {
            channels.add(toTmChannelVO(entry.getValue()));
        }
        target.setTmChannels(channels);
        return target;
    }

    /** 将远程解扰规则转换为前端返回对象。 */
    private DescrambleRuleVO toDescrambleRuleVO(Tm.DescrambleRule source) {
        DescrambleRuleVO target = new DescrambleRuleVO();
        target.setStartPos(source.getStartPos());
        target.setLength(source.getLength());
        target.setFeedBack(source.getFeedBack());
        target.setInitialValue(source.getInitialValue());
        return target;
    }

    /** 将远程遥测通道转换为前端返回对象。 */
    private TmChannelVO toTmChannelVO(Tm.TmChannel source) {
        TmChannelVO target = new TmChannelVO();
        target.setName(source.getName());
        target.setEnable(source.getEnable());
        target.setChannelId(source.getChannelId());
        target.setToDescramble(source.getToDescramble());
        target.setTmSyncHead(source.getTmSyncHead());
        return target;
    }

    /** 规范化通道代号并校验同一请求内唯一。 */
    private String normalizeAndRequireUnique(
            String channelId,
            Set<String> channelIds) {
        String normalized = normalizeChannelId(channelId);
        if (!channelIds.add(normalized)) {
            throw new IllegalArgumentException(
                    "通道代号不能重复：" + normalized);
        }
        return normalized;
    }

    /** 校验并统一十六进制通道代号为大写。 */
    private String normalizeChannelId(String channelId) {
        String normalized = requireText(
                channelId,
                "通道代号不能为空").toUpperCase();
        if (!normalized.matches(CHANNEL_ID_PATTERN)) {
            throw new IllegalArgumentException("通道代号必须是十六进制字符串");
        }
        return normalized;
    }

    /** 校验上传文件名不包含路径信息。 */
    private String validateFileName(String fileName) {
        String normalized = requireText(fileName, "参数表文件名不能为空");
        if (normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("参数表文件名不能包含路径");
        }
        return normalized;
    }

    /** 校验远程查询响应中的业务状态。 */
    private void requireResponseSuccess(int code, String message) {
        if (code != 0) {
            throw new SatelliteManagementException(
                    400,
                    message == null || message.isEmpty()
                            ? "卫星目录查询失败"
                            : message);
        }
    }

    /** 校验文本并返回去除首尾空格后的值。 */
    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /** 将可选字符串空值转换为协议默认空字符串。 */
    private String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
    }
}
