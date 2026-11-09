package com.example.common.greptime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

/** 保留原生动态映射和绑定变量，仅替换固定表标记，不使用配置占位表达式。 */
public class GreptimeTableLanguageDriver extends XMLLanguageDriver {
    /** 映射中固定的分表标记，只能被受控名称替换。 */
    private static final String TABLE_TOKEN = "__greptime_table__";
    /** 初始化数据库时使用的固定标记。 */
    private static final String DATABASE_TOKEN = "__greptime_database__";

    /** 先由原生语言驱动解析条件和循环，再为本次语句解析物理表。 */
    @Override
    public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
        SqlSource delegate = super.createSqlSource(configuration, script, parameterType);
        return parameter -> route(configuration, delegate.getBoundSql(parameter), parameter);
    }

    /** 每次返回独立绑定语句，避免并发查询修改共用映射。 */
    private BoundSql route(Configuration configuration, BoundSql original, Object parameter) {
        String sql = original.getSql();
        if (!sql.contains(TABLE_TOKEN) && !sql.contains(DATABASE_TOKEN)) {
            return original;
        }
        if (!(parameter instanceof Map)) {
            throw new IllegalArgumentException("遥测分表语句缺少路由参数");
        }
        Map<?, ?> query = (Map<?, ?>) parameter;
        // 第一步：数据库标识符独立校验，不接受路径、引号或分隔符。
        if (sql.contains(DATABASE_TOKEN)) {
            Object database = query.get("databaseName");
            if (!(database instanceof String) || !((String) database).matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("遥测数据库名称不合法");
            }
            sql = sql.replace(DATABASE_TOKEN, "`" + database + "`");
        }
        // 第二步：收集单表路由或联合查询的表名，顺序与映射循环保持一致。
        if (sql.contains(TABLE_TOKEN)) {
            List<String> names = tableNames(query);
            StringBuilder routed = new StringBuilder();
            int offset = 0;
            for (String name : names) {
                int position = sql.indexOf(TABLE_TOKEN, offset);
                if (position < 0) {
                    throw new IllegalArgumentException("遥测分表数量与查询分支不一致");
                }
                routed.append(sql, offset, position).append('`').append(name).append('`');
                offset = position + TABLE_TOKEN.length();
            }
            routed.append(sql.substring(offset));
            sql = routed.toString();
            if (sql.contains(TABLE_TOKEN)) {
                throw new IllegalArgumentException("遥测查询存在未解析的分表");
            }
        }
        // 第三步：完整保留原生循环生成的附加绑定变量，不拼接任何业务值。
        BoundSql result = new BoundSql(configuration, sql, original.getParameterMappings(), original.getParameterObject());
        Map<?, ?> additional = (Map<?, ?>) configuration.newMetaObject(original).getValue("additionalParameters");
        for (Map.Entry<?, ?> entry : additional.entrySet()) {
            result.setAdditionalParameter((String) entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** 名称只能来自后端对象路由或分表目录，并逐个经过固定格式校验。 */
    private List<String> tableNames(Map<?, ?> query) {
        List<String> names = new ArrayList<>();
        Object tableName = query.get("tableName");
        if (tableName instanceof String) {
            names.add(GreptimeTableRoute.requireTable((String) tableName));
        } else if (query.get("tables") instanceof List) {
            for (Object table : (List<?>) query.get("tables")) {
                if (!(table instanceof Map) || !(((Map<?, ?>) table).get("tableName") instanceof String)) {
                    throw new IllegalArgumentException("遥测查询分表目录不完整");
                }
                names.add(GreptimeTableRoute.requireTable((String) ((Map<?, ?>) table).get("tableName")));
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("遥测语句缺少有效的对象分表");
        }
        return names;
    }
}
