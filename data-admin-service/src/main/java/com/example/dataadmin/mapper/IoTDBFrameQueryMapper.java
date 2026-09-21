package com.example.dataadmin.mapper;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** 使用MyBatis映射生成整帧查询，由IoTDB会话执行而非关系数据库连接。 */
@Component
public class IoTDBFrameQueryMapper {
    /** 复用应用已经加载的映射配置。 */
    private final SqlSessionFactory sqlSessionFactory;

    /** 注入映射工厂。 */
    public IoTDBFrameQueryMapper(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /** 生成任务范围内的整帧查询；文字字面量由调用方统一转义。 */
    public String realtimeFrames(String taskIdLiteral, Long interfaceId) {
        // IoTDB会话接口不接受绑定参数，只传入已转义的任务字面量及强类型编号。
        if (taskIdLiteral == null || taskIdLiteral.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("taskIdLiteral", taskIdLiteral);
        parameters.put("interfaceId", interfaceId);
        return sqlSessionFactory.getConfiguration()
                .getMappedStatement(IoTDBFrameQueryMapper.class.getName() + ".realtimeFrames")
                .getBoundSql(parameters).getSql();
    }
}
