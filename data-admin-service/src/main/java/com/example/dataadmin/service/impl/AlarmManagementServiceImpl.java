package com.example.dataadmin.service.impl;

import com.example.common.enums.DataAlarmSource;
import com.example.common.response.PageResult;
import com.example.dataadmin.entity.DmAlarmEvent;
import com.example.dataadmin.entity.SysAlarmEvent;
import com.example.dataadmin.entity.SysMonitorSnapshot;
import com.example.dataadmin.mapper.DmAlarmEventMapper;
import com.example.dataadmin.mapper.SysAlarmEventMapper;
import com.example.dataadmin.service.AlarmManagementService;
import com.example.dataadmin.service.monitor.SystemMonitorService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 告警查询和管理入口，健康状态查询委托独立的监控服务。 */
@Service
@Transactional(readOnly = true)
public class AlarmManagementServiceImpl implements AlarmManagementService {

    /** 系统告警查询组件。 */
    private final SysAlarmEventMapper systemAlarmMapper;
    /** 数据告警查询和管理组件。 */
    private final DmAlarmEventMapper dataAlarmMapper;
    /** 系统健康监控业务接口。 */
    private final SystemMonitorService systemMonitorService;

    /** 注入告警管理与健康监控组件。 */
    public AlarmManagementServiceImpl(
            SysAlarmEventMapper systemAlarmMapper,
            DmAlarmEventMapper dataAlarmMapper,
            SystemMonitorService systemMonitorService) {
        // 保存业务组件引用，健康采样状态统一由监控服务持有。
        this.systemAlarmMapper = systemAlarmMapper;
        this.dataAlarmMapper = dataAlarmMapper;
        this.systemMonitorService = systemMonitorService;
    }

    @Override
    public SysMonitorSnapshot getSystemMonitorCurrent() {
        // 委托监控服务即时采集，保持原有查询行为。
        return systemMonitorService.getSystemMonitorCurrent();
    }

    @Override
    public List<SysMonitorSnapshot> getSystemMonitorHistory(int minutes) {
        // 委托监控服务查询历史快照，保留原有时间范围处理。
        return systemMonitorService.getSystemMonitorHistory(minutes);
    }

    @Override
    public List<SysAlarmEvent> listSystemAlarms(Integer status) {
        // 只允许查询既有恢复状态，禁止无效编码静默返回空列表。
        if (status != null && status != 0 && status != 1) {
            throw new IllegalArgumentException("系统告警状态只能为0或1");
        }
        return systemAlarmMapper.findAll(status);
    }

    @Override
    public PageResult<DmAlarmEvent> pageDataAlarms(
            String taskId,
            String sourceCode,
            String keyword,
            Integer alarmLevel,
            Integer alarmStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {
        validateDataAlarmQuery(
                taskId, alarmLevel, alarmStatus, startTime, endTime);
        // 来源参数必须是枚举编码，中文关键字匹配来源名称后再以编码查询。
        String normalizedSource = DataAlarmSource.normalize(sourceCode);
        String normalizedKeyword = trimToNull(keyword);
        List<String> sourceKeywordValues = DataAlarmSource.matchingValues(normalizedKeyword);
        int safePageNum = pageNum == null ? 1 : Math.max(1, pageNum);
        int safePageSize = pageSize == null ? 20
                : Math.min(100, Math.max(1, pageSize));

        // PageHelper 只会分页紧随其后的第一条 MyBatis 查询，二者之间不要插入其他查询。
        PageHelper.startPage(safePageNum, safePageSize);
        List<DmAlarmEvent> records = dataAlarmMapper.findAll(
                taskId,
                normalizedSource,
                normalizedKeyword,
                sourceKeywordValues,
                alarmLevel,
                alarmStatus,
                startTime,
                endTime);
        PageInfo<DmAlarmEvent> pageInfo = new PageInfo<>(records);

        return new PageResult<DmAlarmEvent>(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                records);
    }

    @Override
    public DmAlarmEvent getDataAlarm(Long id) {
        return dataAlarmMapper.findById(id);
    }

    @Override
    @Transactional
    public boolean updateDataAlarmStatus(Long id, Integer alarmStatus) {
        return dataAlarmMapper.updateStatus(id, alarmStatus) > 0;
    }

    /** 校验数据监测告警列表的组合查询条件。 */
    private void validateDataAlarmQuery(
            String taskId,
            Integer alarmLevel,
            Integer alarmStatus,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (taskId != null && taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        if (alarmLevel != null && alarmLevel != 2 && alarmLevel != 4) {
            throw new IllegalArgumentException("数据告警级别只能为2（一般）或4（严重）");
        }
        if (alarmStatus != null && alarmStatus != 0 && alarmStatus != 1) {
            throw new IllegalArgumentException("告警状态只能为0或1");
        }
        if (startTime != null && endTime != null
                && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    /** 将空白查询文本统一转换为null，避免生成无意义的SQL条件。 */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
