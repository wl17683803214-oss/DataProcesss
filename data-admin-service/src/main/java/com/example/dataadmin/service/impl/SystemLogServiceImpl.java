package com.example.dataadmin.service.impl;

import com.example.common.response.PageResult;
import com.example.dataadmin.entity.*;
import com.example.dataadmin.enums.SystemLogLevel;
import com.example.dataadmin.mapper.*;
import com.example.dataadmin.service.SystemLogService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志页面业务实现。
 *
 * 查询默认使用只读事务，新增、修改和删除操作单独开启写事务。
 */
@Service
@Transactional(readOnly = true)
public class SystemLogServiceImpl implements SystemLogService {

    /** SysRuntimeLog数据访问组件。 */
    private final SysRuntimeLogMapper logMapper;

    public SystemLogServiceImpl(SysRuntimeLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    /** 按页面条件组合筛选并分页查询系统运行日志。 */
    @Override
    public PageResult<SysRuntimeLog> pageLogs(
            String taskId,
            String logLevel,
            String logSource,
            String operatorId,
            String keyword,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {
        validateQuery(taskId, startTime, endTime);
        String normalizedLevel = SystemLogLevel.normalize(logLevel);
        int safePageNum = pageNum == null ? 1 : Math.max(1, pageNum);
        int safePageSize = pageSize == null ? 20
                : Math.min(100, Math.max(1, pageSize));

        // PageHelper只分页紧随其后的第一条MyBatis查询，中间不能插入其他查询。
        PageHelper.startPage(safePageNum, safePageSize);
        List<SysRuntimeLog> records = logMapper.findAll(
                taskId,
                normalizedLevel,
                trimToNull(logSource),
                trimToNull(operatorId),
                trimToNull(keyword),
                startTime,
                endTime);
        PageInfo<SysRuntimeLog> pageInfo = new PageInfo<>(records);

        return new PageResult<SysRuntimeLog>(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                records);
    }

    /** 查询日志来源下拉选项。 */
    @Override
    public List<String> listLogSources(String taskId) {
        validateTaskId(taskId);
        return logMapper.findSourceNames(taskId);
    }

    /** 查询指定试验任务的系统运行日志。 */
    @Override
    public List<SysRuntimeLog> listLogs(String taskId) {
        return logMapper.findAllByTaskId(taskId);
    }

    /** 查询系统运行日志详情。 */
    @Override
    public SysRuntimeLog getLog(Long id) {
        return logMapper.findById(id);
    }

    /** 新增系统运行日志。 */
    @Override
    @Transactional
    public Long createLog(SysRuntimeLog log) {
        if (log == null) {
            throw new IllegalArgumentException("日志内容不能为空");
        }
        String normalizedLevel = SystemLogLevel.normalize(log.getLogLevel());
        if (normalizedLevel == null) {
            throw new IllegalArgumentException("日志级别不能为空");
        }
        if (trimToNull(log.getLogSource()) == null) {
            throw new IllegalArgumentException("日志来源不能为空");
        }
        if (trimToNull(log.getLogContent()) == null) {
            throw new IllegalArgumentException("日志内容不能为空");
        }
        log.setLogLevel(normalizedLevel);
        log.setLogSource(log.getLogSource().trim());
        log.setLogContent(log.getLogContent().trim());
        if (log.getLogTime() == null) {
            log.setLogTime(LocalDateTime.now());
        }
        logMapper.insert(log);
        return log.getId();
    }

    /** 更新系统运行日志。 */
    @Override
    @Transactional
    public boolean updateLog(SysRuntimeLog log) {
        if (log == null || log.getId() == null) {
            throw new IllegalArgumentException("日志主键不能为空");
        }
        if (log.getLogLevel() != null) {
            log.setLogLevel(SystemLogLevel.normalize(log.getLogLevel()));
        }
        return logMapper.update(log) > 0;
    }

    /** 删除系统运行日志。 */
    @Override
    @Transactional
    public boolean deleteLog(Long id) {
        return logMapper.delete(id) > 0;
    }

    /** 清空指定试验任务的系统运行日志。 */
    @Override
    @Transactional
    public boolean clearLogs(String taskId) {
        return logMapper.deleteByTaskId(taskId) >= 0;
    }

    /** 校验分页查询中的任务和时间范围。 */
    private void validateQuery(
            String taskId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        validateTaskId(taskId);
        if (startTime != null && endTime != null
                && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    /** 任务ID可不传；传入时必须为正数。 */
    private void validateTaskId(String taskId) {
        if (taskId != null && taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
    }

    /** 将空白查询文本统一转换为null。 */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
