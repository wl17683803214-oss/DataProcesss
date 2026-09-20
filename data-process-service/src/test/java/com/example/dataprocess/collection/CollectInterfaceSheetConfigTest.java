package com.example.dataprocess.collection;

import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** 验证协议配置关联的运行配置比较及实际MyBatis语句构建。 */
class CollectInterfaceSheetConfigTest {
    /** 协议配置或所属任务切换时必须重新加载接口配置。 */
    @Test
    void shouldReloadWhenSheetOrTaskChanges() {
        // 相同任务和协议配置可以复用运行句柄。
        CollectInterfaceRuntimeConfig first = new CollectInterfaceRuntimeConfig();
        CollectInterfaceRuntimeConfig second = new CollectInterfaceRuntimeConfig();
        first.setTaskId("TASK-1");
        second.setTaskId("TASK-1");
        first.setProtocolConfigId(21L);
        second.setProtocolConfigId(21L);
        assertTrue(first.hasSameRuntimeSettings(second));
        // 协议配置主键和任务主键任一变化都要求重启。
        second.setProtocolConfigId(22L);
        assertFalse(first.hasSameRuntimeSettings(second));
        second.setProtocolConfigId(21L);
        second.setTaskId("TASK-2");
        assertFalse(first.hasSameRuntimeSettings(second));
    }

    /** 运行查询直接取得传输协议和关联协议动态配置。 */
    @Test
    void shouldBuildSheetLookupWithoutMultiplyingInterfaceRows() throws Exception {
        // 使用MyBatis自身解析器验证映射和动态列表参数可正常构建。
        String resource = "mapper/CollectInterfaceRuntimeMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String sql = configuration.getMappedStatement(
                "com.example.dataprocess.mapper.CollectInterfaceRuntimeMapper.findEnabledInterfacesByIds")
                .getBoundSql(Collections.singletonMap("interfaceIds", Collections.singletonList(1L)))
                .getSql();
        // 查询不再依赖工作表或参数解析表。
        assertTrue(sql.contains("interface_config.transfer_protocol"));
        assertTrue(sql.contains("protocol_config.config_params"));
        assertFalse(sql.contains("join telemetry_parse_rule_config"));
        assertFalse(sql.contains("sheet_name"));
    }
}
