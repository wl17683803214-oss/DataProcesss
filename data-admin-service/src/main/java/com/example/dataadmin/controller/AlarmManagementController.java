package com.example.dataadmin.controller;

import com.example.common.response.ApiResponse;
import com.example.common.response.PageResult;
import com.example.dataadmin.dto.IdRequest;
import com.example.dataadmin.dto.IdStatusRequest;
import com.example.dataadmin.dto.alarm.DataAlarmSaveRequest;
import com.example.dataadmin.dto.alarm.SystemAlarmSaveRequest;
import com.example.dataadmin.entity.DmAlarmEvent;
import com.example.dataadmin.entity.SysAlarmEvent;
import com.example.dataadmin.entity.SysHealthAlarmEvent;
import com.example.dataadmin.entity.SysMonitorSnapshot;
import com.example.dataadmin.enums.ControllerEnumData;
import com.example.dataadmin.service.AlarmManagementService;
import com.example.dataadmin.vo.EnumOptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据监测告警和系统告警管理接口。
 */
@RestController
@RequestMapping("/alarms")
public class AlarmManagementController {

    private final AlarmManagementService alarmService;

    public AlarmManagementController(AlarmManagementService alarmService) {
        this.alarmService = alarmService;
    }

    /** 查询告警管理页面涉及的全部枚举选项。 */
    @GetMapping("/findData")
    public ApiResponse<Map<String, List<EnumOptionVO>>> findData() {
        return ApiResponse.success(ControllerEnumData.alarm());
    }

    /** 查询系统健康监测当前状态，供“系统告警”页顶部指标卡展示。 */
    @GetMapping("/system-monitor/current")
    public ApiResponse<SysMonitorSnapshot> getSystemMonitorCurrent() {
        return ApiResponse.success(alarmService.getSystemMonitorCurrent());
    }

    /** 查询一周内系统健康状态快照，用于趋势曲线。 */
    @GetMapping("/system-monitor/history")
    public ApiResponse<List<SysMonitorSnapshot>> getSystemMonitorHistory(
            @RequestParam(defaultValue = "60") int minutes) {
        return ApiResponse.success(
                alarmService.getSystemMonitorHistory(minutes));
    }

    /** 查询系统资源与链路健康告警事件，不读取数据监测告警表。 */
    @GetMapping("/system-monitor/events")
    public ApiResponse<List<SysHealthAlarmEvent>> listSystemHealthAlarms(
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(
                alarmService.getSystemHealthAlarms(status));
    }

    /** 查询数据监测告警。 */
    @GetMapping("/data")
    public ApiResponse<PageResult<DmAlarmEvent>> listDataAlarms(
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String sourceName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer alarmLevel,
            @RequestParam(required = false) Integer alarmStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(
                alarmService.pageDataAlarms(
                        taskId,
                        sourceName,
                        keyword,
                        alarmLevel,
                        alarmStatus,
                        startTime,
                        endTime,
                        pageNum,
                        pageSize));
    }

    /** 查询数据监测告警来源选项，供页面来源下拉框使用。 */
    @GetMapping("/data/sources")
    public ApiResponse<List<String>> listDataAlarmSources(
            @RequestParam(required = false) String taskId) {
        return ApiResponse.success(
                alarmService.listDataAlarmSources(taskId));
    }

    /** 查询数据监测告警详情。 */
    @GetMapping("/data/{id}")
    public ApiResponse<DmAlarmEvent> getDataAlarm(@PathVariable Long id) {
        return ApiResponse.success(alarmService.getDataAlarm(id));
    }

    /** 新增数据监测告警。 */
    @PostMapping("/data/create")
    public ApiResponse<Long> createDataAlarm(
            @RequestBody DataAlarmSaveRequest request) {
        DmAlarmEvent alarm = toDataAlarm(request);
        return ApiResponse.success(alarmService.createDataAlarm(alarm));
    }

    /** 修改数据监测告警。 */
    @PostMapping("/data/update")
    public ApiResponse<Void> updateDataAlarm(
            @RequestBody DataAlarmSaveRequest request) {
        requireId(request.getId());
        DmAlarmEvent alarm = toDataAlarm(request);
        requireSuccess(
                alarmService.updateDataAlarm(alarm),
                "数据监测告警修改失败");
        return ApiResponse.success();
    }

    /** 删除数据监测告警。 */
    @PostMapping("/data/delete")
    public ApiResponse<Void> deleteDataAlarm(
            @Valid @RequestBody IdRequest request) {
        requireSuccess(
                alarmService.deleteDataAlarm(request.getId()),
                "数据监测告警删除失败");
        return ApiResponse.success();
    }

    /** 处理或恢复数据监测告警。 */
    // 处理/恢复告警属于对告警状态的修改，统一使用编辑权限。
    @PostMapping("/data/status")
    public ApiResponse<Void> updateDataAlarmStatus(
            @Valid @RequestBody IdStatusRequest request) {
        validateBinaryStatus(request.getStatus());
        requireSuccess(
                alarmService.updateDataAlarmStatus(
                        request.getId(),
                        request.getStatus()),
                "数据监测告警状态修改失败");
        return ApiResponse.success();
    }

    /** 查询系统告警。 */
    @GetMapping("/system")
    public ApiResponse<List<SysAlarmEvent>> listSystemAlarms() {
        return ApiResponse.success(alarmService.listSystemAlarms());
    }

    /** 查询系统告警详情。 */
    @GetMapping("/system/{id}")
    public ApiResponse<SysAlarmEvent> getSystemAlarm(@PathVariable Long id) {
        return ApiResponse.success(alarmService.getSystemAlarm(id));
    }

    /** 新增系统告警。 */
    @PostMapping("/system/create")
    public ApiResponse<Long> createSystemAlarm(
            @RequestBody SystemAlarmSaveRequest request) {
        SysAlarmEvent alarm = toSystemAlarm(request);
        return ApiResponse.success(alarmService.createSystemAlarm(alarm));
    }

    /** 修改系统告警。 */
    @PostMapping("/system/update")
    public ApiResponse<Void> updateSystemAlarm(
            @RequestBody SystemAlarmSaveRequest request) {
        requireId(request.getId());
        SysAlarmEvent alarm = toSystemAlarm(request);
        requireSuccess(
                alarmService.updateSystemAlarm(alarm),
                "系统告警修改失败");
        return ApiResponse.success();
    }

    /** 删除系统告警。 */
    @PostMapping("/system/delete")
    public ApiResponse<Void> deleteSystemAlarm(
            @Valid @RequestBody IdRequest request) {
        requireSuccess(
                alarmService.deleteSystemAlarm(request.getId()),
                "系统告警删除失败");
        return ApiResponse.success();
    }

    /** 恢复或重新打开系统告警。 */
    // 处理/恢复告警属于对告警状态的修改，统一使用编辑权限。
    @PostMapping("/system/status")
    public ApiResponse<Void> updateSystemAlarmStatus(
            @Valid @RequestBody IdStatusRequest request) {
        validateBinaryStatus(request.getStatus());
        requireSuccess(
                alarmService.updateSystemAlarmStatus(
                        request.getId(),
                        request.getStatus()),
                "系统告警状态修改失败");
        return ApiResponse.success();
    }

    /** 将数据告警请求 DTO 转换为数据库实体。 */
    private DmAlarmEvent toDataAlarm(DataAlarmSaveRequest request) {
        DmAlarmEvent alarm = new DmAlarmEvent();
        BeanUtils.copyProperties(request, alarm);
        return alarm;
    }

    /** 将系统告警请求 DTO 转换为数据库实体。 */
    private SysAlarmEvent toSystemAlarm(SystemAlarmSaveRequest request) {
        SysAlarmEvent alarm = new SysAlarmEvent();
        BeanUtils.copyProperties(request, alarm);
        return alarm;
    }

    /** 校验写操作请求中的业务主键。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("业务主键不能为空");
        }
    }

    /** 校验二值状态字段。 */
    private void validateBinaryStatus(Integer status) {
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("状态只能为0或1");
        }
    }

    /** 将实现层执行结果转换为统一业务异常。 */
    private void requireSuccess(boolean success, String message) {
        if (!success) {
            throw new IllegalArgumentException(message);
        }
    }
}
