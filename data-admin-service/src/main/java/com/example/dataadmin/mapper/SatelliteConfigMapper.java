package com.example.dataadmin.mapper;

import com.example.dataadmin.entity.SatelliteConfig;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 卫星档案全部通过对应的映射文件执行读写。 */
public interface SatelliteConfigMapper {
    /** 复用事务锁，串行化多个实例对卫星档案的修改。 */
    String lockManagement();
    /** 查询元数据，避免列表读取完整配置原文。 */
    List<SatelliteConfig> findList(@Param("name") String name, @Param("code") String code,
            @Param("enable") String enable);
    /** 按本地主键查询唯一记录。 */
    SatelliteConfig findById(@Param("id") Long id);
    /** 查询批次实际存在的主键。 */
    List<Long> findIds(@Param("ids") List<Long> ids);
    /** 插入档案并回填生成的主键。 */
    int insert(SatelliteConfig satellite);
    /** 更新指定主键的记录。 */
    int update(SatelliteConfig satellite);
    /** 按主键批量删除，空集合不匹配任何记录。 */
    int deleteByIds(@Param("ids") List<Long> ids);
}
