package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.TelemetrySystemConfig;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 所属系统层级数据访问。 */
public interface TelemetrySystemConfigMapper {
    /** 插入节点并回填主键。 */
    int insert(TelemetrySystemConfig system);
    /** 替换前删除同类型设备的旧系统。 */
    int deleteByType(@Param("taskId") String taskId, @Param("type") String type);
    /** 查询设备下的全部有效系统，供层级展示。 */
    List<TelemetrySystemConfig> findByDevice(@Param("taskId") String taskId,
                                              @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 查询当前系统的直属子系统。 */
    List<TelemetrySystemConfig> findChildren(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId, @Param("parentId") Long parentId);
    /** 验证展开的系统属于当前设备。 */
    TelemetrySystemConfig findActive(@Param("taskId") String taskId,
            @Param("deviceSatelliteId") Long deviceSatelliteId, @Param("id") Long id);
}
