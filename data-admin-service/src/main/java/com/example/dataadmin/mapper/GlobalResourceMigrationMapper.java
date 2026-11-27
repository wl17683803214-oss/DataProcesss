package com.example.dataadmin.mapper;

import java.util.List;

/** 全局配置升级使用显式映射语句，不在业务代码中拼装数据库结构语句。 */
public interface GlobalResourceMigrationMapper {
    /** 在冲突检查之前锁定待升级表，禁止并发旧实例写入。 */
    void lockTables();

    /** 事务内先补充范围列，便于重复升级时识别已生效的全局配置。 */
    void prepareScope();

    /** 升级前只读列出冲突，不自动覆盖不同任务遗留的同名配置。 */
    List<String> findConflicts();

    /** 在同一事务中调整兼容字段、全局唯一约束及初始配置。 */
    void migrate();
}
