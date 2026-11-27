package com.example.dataadmin.config;

import com.example.dataadmin.mapper.GlobalResourceMigrationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 仅显式开启升级选项时执行结构调整，普通启动不擅自修改数据库。 */
@Component
@ConditionalOnProperty(name = "data-processing.migrate-global-resources", havingValue = "true")
public class GlobalResourceSchemaMigration implements InitializingBean {
    /** 数据结构升级日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalResourceSchemaMigration.class);
    /** 全部升级语句统一通过映射组件执行。 */
    private final GlobalResourceMigrationMapper mapper;
    /** 编程事务保证初始化阶段也经过真实事务管理器。 */
    private final TransactionTemplate transaction;

    /** 初始化升级组件，不在构造阶段访问数据库。 */
    public GlobalResourceSchemaMigration(GlobalResourceMigrationMapper mapper, PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
    }

    /** 先校验再升级，任何一步失败均回滚，错误信息包含待处理的配置身份。 */
    @Override
    public void afterPropertiesSet() {
        transaction.execute(status -> {
            // 第一步：保护配置表及统计表，避免检查通过后又有旧实例写入冲突数据。
            mapper.lockTables();
            mapper.prepareScope();
            List<String> conflicts = mapper.findConflicts();
            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("全局配置升级存在冲突，请先处理后重试：" + conflicts);
            }
            // 第二步：调整结构并初始化唯一全局配置，保持原有任务数据归属。
            mapper.migrate();
            return null;
        });
        LOGGER.info("全局配置及无任务筛选查询的数据结构升级完成");
    }
}
