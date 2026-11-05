package com.example.dataadmin.mapper;

import com.example.common.formula.FormulaScript;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 公式的所有数据库操作通过XML执行。 */
public interface FormulaScriptMapper {
    /** 分页查询与计数复用同一筛选条件。 */
    long countPage(@Param("code") String code, @Param("name") String name);
    /** 只查询本页元数据，不加载脚本源码。 */
    List<FormulaScript> findPage(@Param("code") String code, @Param("name") String name,
            @Param("limit") int limit, @Param("offset") long offset);
    /** 按编号取得当前脚本，供导出、试算和覆盖导入使用。 */
    FormulaScript findByCode(@Param("code") String code);
    /** 有界读取全部脚本，超过单次导出上限时由业务层提示筛选。 */
    List<FormulaScript> findForExport(@Param("limit") int limit);
    /** 复用数据库事务锁，将多个实例的公式导入及删除串行化。 */
    String lockManagement();
    /** 查询批次中实际存在的公式编号。 */
    List<String> findExistingCodes(@Param("codes") List<String> codes);
    /** 同编号覆盖更新，保留首次导入时间，不保存历史源码。 */
    int upsert(FormulaScript script);
    /** 按已校验的编号批量物理删除公式。 */
    int deleteByCodes(@Param("codes") List<String> codes);
}
