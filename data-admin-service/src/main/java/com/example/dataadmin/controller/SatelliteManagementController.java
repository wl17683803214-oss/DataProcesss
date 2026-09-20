package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.dto.satellite.ChannelEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteCodeRequest;
import com.example.dataadmin.dto.satellite.SatelliteEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteInfoRequest;
import com.example.dataadmin.dto.satellite.SatelliteUpdateRequest;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.enums.SatelliteTableType;
import com.example.dataadmin.service.SatelliteManagementService;
import com.example.dataadmin.vo.EnumOptionVO;
import com.example.dataadmin.vo.satellite.SatelliteInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** 卫星目录管理REST转gRPC接口。 */
@RestController
@RequestMapping("/satellite-management")
public class SatelliteManagementController {

    /** 卫星目录管理业务服务。 */
    private final SatelliteManagementService satelliteManagementService;

    public SatelliteManagementController(
            SatelliteManagementService satelliteManagementService) {
        this.satelliteManagementService = satelliteManagementService;
    }

    /** 查询卫星目录页面使用的枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(
                ControllerEnumData.satelliteManagement());
    }

    /** 查询全部卫星完整配置。 */
    @GetMapping("/satellites")
    public ApiResponse<List<SatelliteInfoVO>> listSatellites() {
        return ApiResponse.success(
                satelliteManagementService.listSatellites());
    }

    /** 按卫星代号查询完整配置。 */
    @GetMapping("/satellites/{code}")
    public ApiResponse<SatelliteInfoVO> getSatellite(
            @PathVariable String code) {
        return ApiResponse.success(
                satelliteManagementService.getSatellite(code));
    }

    /** 创建卫星完整配置。 */
    @PostMapping("/satellites/create")
    public ApiResponse<Void> createSatellite(
            @Valid @RequestBody SatelliteInfoRequest request) {
        satelliteManagementService.createSatellite(request);
        return ApiResponse.success();
    }

    /** 整体更新卫星完整配置。 */
    @PostMapping("/satellites/update")
    public ApiResponse<Void> updateSatellite(
            @Valid @RequestBody SatelliteUpdateRequest request) {
        satelliteManagementService.updateSatellite(request);
        return ApiResponse.success();
    }

    /** 删除指定卫星。 */
    @PostMapping("/satellites/delete")
    public ApiResponse<Void> deleteSatellite(
            @Valid @RequestBody SatelliteCodeRequest request) {
        satelliteManagementService.deleteSatellite(request.getCode());
        return ApiResponse.success();
    }

    /** 修改卫星启用状态。 */
    @PostMapping("/satellites/enabled")
    public ApiResponse<Void> setSatelliteEnabled(
            @Valid @RequestBody SatelliteEnabledRequest request) {
        satelliteManagementService.setSatelliteEnabled(request);
        return ApiResponse.success();
    }

    /** 修改遥测通道启用状态。 */
    @PostMapping("/channels/enabled")
    public ApiResponse<Void> setChannelEnabled(
            @Valid @RequestBody ChannelEnabledRequest request) {
        satelliteManagementService.setChannelEnabled(request);
        return ApiResponse.success();
    }

    /** 上传遥测参数表A或帧结构表B。 */
    @PostMapping(value = "/tables/upload", consumes = "multipart/form-data")
    public ApiResponse<Void> uploadTable(
            @RequestParam String code,
            @RequestParam String tableType,
            @RequestParam("file") MultipartFile file) {
        // 第一步：在读取文件前校验用户是否选择了有效文件。
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的参数表文件");
        }
        try {
            // 第二步：解析参数表枚举并将文件内容交给远程转发服务。
            satelliteManagementService.uploadTable(
                    code,
                    SatelliteTableType.fromValue(tableType),
                    file.getOriginalFilename(),
                    file.getBytes());
            return ApiResponse.success();
        } catch (IOException exception) {
            // 第三步：将本地文件读取失败转换为统一中文参数异常。
            throw new IllegalArgumentException("读取参数表文件失败", exception);
        }
    }

}
