package com.example.dataadmin.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 全局配置升级使用显式映射语句，不在业务代码中拼装数据库结构语句。 */
public interface GlobalResourceMigrationMapper {
    /** 在冲突检查之前锁定待升级表，禁止并发旧实例写入。 */
    void lockTables();

    /** 识别已添加过的旧范围列，不向未迁移环境添加废弃字段。 */
    boolean hasLegacyScope();

    /** 升级前只读列出冲突，不自动覆盖不同任务遗留的同名配置。 */
    List<String> findConflicts();

    /** 在同一事务中调整兼容字段、全局唯一约束及初始配置。 */
    void migrate(@Param("legacyScopePresent") boolean legacyScopePresent);

    /** 单独保留当前开关值并移除范围列，不触碰其他全局表结构。 */
    void migrateProcessingRuleConfig(@Param("legacyScopePresent") boolean legacyScopePresent);
}
