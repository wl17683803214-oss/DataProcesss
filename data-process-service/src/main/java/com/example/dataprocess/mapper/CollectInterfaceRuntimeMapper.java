package com.example.dataprocess.mapper;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 采集接口运行配置数据访问接口。 */
public interface CollectInterfaceRuntimeMapper {

    /** 查询服务启动时当前最大任务下应该运行的全部已启用接口。 */
    List<CollectInterfaceRuntimeConfig> findAllEnabledInterfaces();

    /** 按Admin通知的接口主键列表查询全部已启用接口。 */
    List<CollectInterfaceRuntimeConfig> findEnabledInterfacesByIds(
            @Param("interfaceIds") List<Long> interfaceIds);

}
