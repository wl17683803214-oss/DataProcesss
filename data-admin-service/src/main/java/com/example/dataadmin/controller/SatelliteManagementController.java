package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.dataadmin.config.SatelliteManagementRpcProperties;
import com.example.dataadmin.dto.satellite.SatelliteBatchDeleteRequest;
import com.example.dataadmin.dto.satellite.SatelliteEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteIdRequest;
import com.example.dataadmin.dto.satellite.SatelliteInfoRequest;
import com.example.dataadmin.dto.satellite.SatelliteTableRequest;
import com.example.dataadmin.dto.satellite.SatelliteUpdateRequest;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.service.BaselineSatelliteCatalogSyncService;
import com.example.dataadmin.service.SatelliteManagementService;
import com.example.dataadmin.storage.SatelliteTableStorage;
import com.example.dataadmin.vo.EnumOptionVO;
import com.example.dataadmin.vo.satellite.SatelliteBaselineSyncVO;
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
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 卫星档案在本地管理，上传配置及启停立即调用远程服务。 */
@RestController
@RequestMapping("/satellite-management")
public class SatelliteManagementController {
    /** 本地档案与远程下发业务。 */
    private final SatelliteManagementService satelliteManagementService;
    /** 独立的基线档案同步，不影响原任务参数同步接口。 */
    private final BaselineSatelliteCatalogSyncService baselineSyncService;
    /** 复用已有配置，在读取配置文件前检查大小。 */
    private final int maxUploadFileSize;
    /** 上传文件与基线下载使用相同的文件保存目录。 */
    private final SatelliteTableStorage storage;

    /** 注入已有业务及文件保存能力。 */
    public SatelliteManagementController(SatelliteManagementService satelliteManagementService,
            BaselineSatelliteCatalogSyncService baselineSyncService, SatelliteTableStorage storage,
            SatelliteManagementRpcProperties properties) {
        this.satelliteManagementService = satelliteManagementService;
        this.baselineSyncService = baselineSyncService;
        this.storage = storage;
        this.maxUploadFileSize = properties.getMaxUploadFileSize();
    }

    /** 查询英文启用值和中文标签。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.satelliteManagement());
    }

    /** 查询本地档案列表，允许重复名称及代号，不返回配置原文。 */
    @GetMapping("/satellites")
    public ApiResponse<List<SatelliteInfoVO>> listSatellites(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String enable) {
        return ApiResponse.success(satelliteManagementService.listSatellites(name, code, enable));
    }

    /** 详情改用本地主键，避免重复代号定位不明确。 */
    @GetMapping("/satellites/{id}")
    public ApiResponse<SatelliteInfoVO> getSatellite(@PathVariable Long id) {
        return ApiResponse.success(satelliteManagementService.getSatellite(id));
    }

    /** 创建本地卫星档案，返回主键供后续上传配置文件。 */
    @PostMapping("/satellites/create")
    public ApiResponse<Long> createSatellite(@Valid @RequestBody SatelliteInfoRequest request) {
        return ApiResponse.success(satelliteManagementService.createSatellite(request));
    }

    /** 根据本地主键编辑档案，名称和代号均允许重复。 */
    @PostMapping("/satellites/update")
    public ApiResponse<Void> updateSatellite(@Valid @RequestBody SatelliteUpdateRequest request) {
        satelliteManagementService.updateSatellite(request);
        return ApiResponse.success();
    }

    /** 单条删除与批量删除复用同一事务及校验流程。 */
    @PostMapping("/satellites/delete")
    public ApiResponse<Integer> deleteSatellite(@Valid @RequestBody SatelliteIdRequest request) {
        return ApiResponse.success(satelliteManagementService.deleteSatellites(
                Collections.singletonList(request.getId())));
    }

    /** 按主键批量删除，只删除本地档案。 */
    @PostMapping("/satellites/batch-delete")
    public ApiResponse<Integer> deleteSatellites(@Valid @RequestBody SatelliteBatchDeleteRequest request) {
        return ApiResponse.success(satelliteManagementService.deleteSatellites(request.getIds()));
    }

    /** 修改当前选中档案对应远程代号的启用状态。 */
    @PostMapping("/satellites/enabled")
    public ApiResponse<Void> setSatelliteEnabled(@Valid @RequestBody SatelliteEnabledRequest request) {
        satelliteManagementService.setSatelliteEnabled(request);
        return ApiResponse.success();
    }

    /** 使用已经保存的文件地址立即下发参数表配置。 */
    @PostMapping(value = "/tables/upload", consumes = "application/json")
    public ApiResponse<Void> uploadTable(@Valid @RequestBody SatelliteTableRequest request) {
        satelliteManagementService.uploadTable(request.getId(), request.getTablePath());
        return ApiResponse.success();
    }

    /** 人工上传参数表时先保存本地文件，再立即下发本地路径。 */
    @PostMapping(value = "/tables/upload", consumes = "multipart/form-data")
    public ApiResponse<Void> uploadTableFile(@RequestParam Long id, @RequestParam("file") MultipartFile file) {
        // 第一步：校验文件存在，后续流式保存负责实际大小检查。
        requireFile(file, "请选择遥测参数表文件");
        satelliteManagementService.getSatellite(id);
        try (InputStream input = file.getInputStream()) {
            // 第二步：保存到独立目录，不使用原来的远程文件字节上传协议。
            String localPath = storage.save(file.getOriginalFilename(), input, file.getSize());
            satelliteManagementService.uploadTable(id, localPath);
            return ApiResponse.success();
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取遥测参数表文件失败", exception);
        }
    }

    /** 上传配置文件，远程成功后在本地保存完整文本。 */
    @PostMapping(value = "/json/upload", consumes = "multipart/form-data")
    public ApiResponse<Void> uploadJson(@RequestParam Long id, @RequestParam("file") MultipartFile file) {
        // 第一步：读取前先检查文件大小，避免将超大配置全部读入内存。
        requireFile(file, "请选择卫星配置文件");
        try {
            // 第二步：配置编码、语法及远程下发统一由业务服务处理。
            satelliteManagementService.uploadJson(id, file.getOriginalFilename(), file.getBytes());
            return ApiResponse.success();
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取卫星配置文件失败", exception);
        }
    }

    /** 将基线返回的每条卫星新增到本地，并下载和下发参数表。 */
    @PostMapping("/satellites/sync-baseline")
    public ApiResponse<SatelliteBaselineSyncVO> synchronizeBaseline() {
        return ApiResponse.success(baselineSyncService.synchronize());
    }

    /** 仅校验必需的文件存在，不对卫星代号做任何唯一限制。 */
    private void requireFile(MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        // 沿用已有文件大小配置，在读取配置原文之前控制资源占用。
        if (file.getSize() > maxUploadFileSize) {
            throw new IllegalArgumentException("上传文件超过配置的文件大小");
        }
    }
}
