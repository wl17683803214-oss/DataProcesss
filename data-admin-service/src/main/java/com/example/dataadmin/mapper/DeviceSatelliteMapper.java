package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.DeviceSatellite;
import com.example.dataadmin.vo.processing.DeviceSatelliteOptionVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 设备卫星数据访问。 */
public interface DeviceSatelliteMapper {
    /** 按任务和设备卫星主键逻辑删除当前有效记录。 */
    int deleteByDevice(@Param("taskId") String taskId, @Param("deviceSatelliteId") Long deviceSatelliteId);
    /** 根据唯一主键定位有效设备卫星及其所属任务。 */
    DeviceSatellite findActiveById(@Param("id") Long id);
    /** 锁定当前试验任务，防止同时导入产生两套有效数据。 */
    String lockForReplacement(@Param("taskId") String taskId);
    /** 查询指定任务和类型的筛选项。 */
    List<DeviceSatelliteOptionVO> findOptions(@Param("taskId") String taskId, @Param("type") String type);
    /** 在当前任务中查找有效关联。 */
    DeviceSatellite findActive(@Param("taskId") String taskId, @Param("id") Long id);
    /** 导入前读取同类型有效设备，以便同名工作表复用主键。 */
    List<DeviceSatellite> findByType(@Param("taskId") String taskId, @Param("type") String type);
    /** 恢复本次导入仍存在的设备卫星主键。 */
    int restore(@Param("taskId") String taskId, @Param("id") Long id);
    /** 插入并回填主键。 */
    int insert(DeviceSatellite device);
    /** 逻辑删除当前任务的同类型记录。 */
    int deleteByType(@Param("taskId") String taskId, @Param("type") String type);
}
