package com.example.dataadmin.service;

import com.example.dataadmin.dto.satellite.ChannelEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteEnabledRequest;
import com.example.dataadmin.dto.satellite.SatelliteInfoRequest;
import com.example.dataadmin.dto.satellite.SatelliteUpdateRequest;
import com.example.dataadmin.enums.SatelliteTableType;
import com.example.dataadmin.vo.satellite.SatelliteInfoVO;

import java.util.List;

/** 卫星目录管理服务。 */
public interface SatelliteManagementService {

    List<SatelliteInfoVO> listSatellites();

    SatelliteInfoVO getSatellite(String code);

    void createSatellite(SatelliteInfoRequest request);

    void updateSatellite(SatelliteUpdateRequest request);

    void deleteSatellite(String code);

    void setSatelliteEnabled(SatelliteEnabledRequest request);

    void setChannelEnabled(ChannelEnabledRequest request);

    void uploadTable(
            String code,
            SatelliteTableType tableType,
            String fileName,
            byte[] content);

}
