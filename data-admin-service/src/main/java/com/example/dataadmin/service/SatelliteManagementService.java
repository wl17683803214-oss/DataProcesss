package com.example.dataadmin.service;

import com.example.dataadmin.dto.satellite.SatelliteEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteInfoRequest;
import com.example.dataadmin.dto.satellite.SatelliteUpdateRequest;
import com.example.dataadmin.vo.satellite.SatelliteInfoVO;
import java.util.List;

/** 本地卫星档案及即时远程配置下发。 */
public interface SatelliteManagementService {
    /** 查询本地卫星元数据，允许按重复的名称及代号筛选。 */
    List<SatelliteInfoVO> listSatellites(String name, String code, String enable);
    /** 按本地主键查询包含配置原文的详情。 */
    SatelliteInfoVO getSatellite(Long id);
    /** 新建本地档案，配置了参数表时立即下发，返回新主键。 */
    Long createSatellite(SatelliteInfoRequest request);
    /** 编辑档案，发生配置变化时立即下发远程服务。 */
    void updateSatellite(SatelliteUpdateRequest request);
    /** 按主键批量删除，仅操作本地数据库。 */
    int deleteSatellites(List<Long> ids);
    /** 立即下发启用状态，失败时回滚本地修改。 */
    void setSatelliteEnabled(SatelliteEnabledRequest request);
    /** 下发已保存到本机的参数表，并保存实际使用的路径。 */
    void uploadTable(Long id, String tablePath);
    /** 上传配置原文，远程成功后保存，不拆解配置字段。 */
    void uploadJson(Long id, String fileName, byte[] content);
    /** 每条基线卫星独立新增并下发参数表，不对名称或代号去重。 */
    Long createBaselineSatellite(String name, String code, String tablePath);
}
