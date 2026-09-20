package com.example.dataprocess.collection;

import com.example.dataprocess.mapper.CollectInterfaceRuntimeMapper;
import com.example.dataprocess.processing.handler.ProtocolHandlerRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Admin通知采集接口同步逻辑测试。 */
class CollectInterfaceManagerTest {

    /** 验证空列表表示停止全部接口且不会生成空IN查询。 */
    @Test
    void shouldNotQueryDatabaseForEmptyList() {
        // 准备没有运行接口的管理器和空Admin通知列表。
        CollectInterfaceRuntimeMapper mapper = mock(CollectInterfaceRuntimeMapper.class);
        CollectInterfaceManager manager = manager(mapper);

        // 执行完整列表同步，空列表表示当前不运行任何采集接口。
        manager.syncInterfaces(Collections.<Long>emptyList());

        // 空列表不应调用带IN条件的MyBatis查询。
        verify(mapper, never()).findEnabledInterfacesByIds(
                Collections.<Long>emptyList());
    }

    /** 验证通知列表会清除空值和重复主键后再查询数据库。 */
    @Test
    void shouldNormalizeInterfaceIdsBeforeQuery() {
        // 准备包含重复值和空值的Admin通知列表。
        CollectInterfaceRuntimeMapper mapper = mock(CollectInterfaceRuntimeMapper.class);
        when(mapper.findEnabledInterfacesByIds(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Collections.emptyList());
        CollectInterfaceManager manager = manager(mapper);

        // 执行接口列表同步。
        manager.syncInterfaces(Arrays.asList(11L, null, 11L, 12L));

        // MyBatis只接收去重后的有效接口主键。
        verify(mapper).findEnabledInterfacesByIds(
                eq(Arrays.asList(11L, 12L)));
    }

    /** 创建不实际打开网络端点的接口管理器。 */
    private CollectInterfaceManager manager(CollectInterfaceRuntimeMapper mapper) {
        return new CollectInterfaceManager(
                mapper,
                mock(CollectInterfaceStatistics.class),
                mock(ThreadPoolExecutor.class),
                mock(ProtocolHandlerRegistry.class),
                mock(CalibrationRuntimeConfigManager.class));
    }
}
