package com.example.dataprocess.entity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import com.example.dataprocess.protocol.rpc.PdxpProtocolField;

/** 数据处理服务运行采集接口所需的配置快照。 */
public class CollectInterfaceRuntimeConfig {
    /** 公式源码在同步时编译，每帧处理只读取当前版本快照。 */
    private volatile Map<String, com.example.common.formula.LuaFormulaEngine.Compiled>
            formulaScripts = Collections.emptyMap();

    /** 获取当前公式快照。 */
    public Map<String, com.example.common.formula.LuaFormulaEngine.Compiled> getFormulaScripts() {
        return formulaScripts;
    }

    /** 原子替换只读公式快照。 */
    public void setFormulaScripts(Map<String, com.example.common.formula.LuaFormulaEngine.Compiled> scripts) {
        formulaScripts = scripts == null ? Collections.emptyMap() : scripts;
    }
    /** 接收速率上限，空值表示不限速。 */
    private java.math.BigDecimal receiveRateLimit;
    /** 接收速率单位，使用英文单位编码保存。 */
    private String receiveRateLimitUnit;

    /** 获取接收速率上限。 */
    public java.math.BigDecimal getReceiveRateLimit() { return receiveRateLimit; }
    /** 设置接收速率上限。 */
    public void setReceiveRateLimit(java.math.BigDecimal value) { receiveRateLimit = value; }
    /** 获取接收速率单位。 */
    public String getReceiveRateLimitUnit() { return receiveRateLimitUnit; }
    /** 设置接收速率单位。 */
    public void setReceiveRateLimitUnit(String value) { receiveRateLimitUnit = value; }


    /** 同一次接口启动只记录第一条处理完成日志，并发处理时原子判断。 */
    private final AtomicBoolean pdxpProcessingLogged = new AtomicBoolean();
    /** 同一次接口启动只记录第一条消息发送成功日志。 */
    private final AtomicBoolean pdxpMessageLogged = new AtomicBoolean();

    /** 已运行接口复用当前对象，重新启动创建新对象并重新允许首帧日志。 */
    public boolean markFirstPdxpProcessingLog() {
        return pdxpProcessingLogged.compareAndSet(false, true);
    }

    /** 消息发送完成后再标记，失败日志仍由结果调度组件逐次记录。 */
    public boolean markFirstPdxpMessageLog() {
        return pdxpMessageLogged.compareAndSet(false, true);
    }

    /** 采集接口主键。 */
    private Long interfaceId;
    /** 采集接口名称。 */
    private String interfaceName;
    /** 采集接口所属试验任务主键。 */
    private String taskId;
    /** 采集接口本机监听地址。 */
    private String host;
    /** 采集接口本机监听端口。 */
    private Integer port;
    /** UDP组播地址，为空时按单播接收。 */
    private String multicastIp;
    /** 传输方式：1UDP、2TCP、3HTTP。 */
    private Integer transferType;
    /** 传输协议：1JSON、2PDXP、3FEP。 */
    private Integer transferProtocol;
    /** 是否启用RPC处理。 */
    private Integer rpcEnabled;
    /** 关联协议配置主键。 */
    private Long protocolConfigId;
    /** 参数值范围检查是否开启：0关闭、1开启。 */
    private volatile Integer parameterRangeCheckEnabled;

    /** 协议动态配置参数。 */
    private String protocolConfigParams;
    /** PDXP本地处理关联的设备卫星主键；RPC运行时不设置此值。 */
    private volatile Long deviceSatelliteId;
    /** 导入工作表对应的设备卫星名称。 */
    private volatile String deviceSatelliteName;
    /** 接口启动时加载的参数解析配置快照。 */
    private volatile List<PdxpProtocolField> pdxpFields = Collections.emptyList();

    public Long getDeviceSatelliteId() { return deviceSatelliteId; }
    public void setDeviceSatelliteId(Long deviceSatelliteId) { this.deviceSatelliteId = deviceSatelliteId; }
    public String getDeviceSatelliteName() { return deviceSatelliteName; }
    public void setDeviceSatelliteName(String name) { this.deviceSatelliteName = name; }
    public List<PdxpProtocolField> getPdxpFields() { return pdxpFields; }
    public void setPdxpFields(List<PdxpProtocolField> fields) {
        this.pdxpFields = fields == null ? Collections.emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<>(fields));
    }
    /** 提交采集任务前加载的卫星名称快照，按卫星编码索引。 */
    private volatile Map<String, BaselineSatelliteInfo> baselineSatellites = Collections.emptyMap();

    /** 获取采集和处理线程共用的只读目录。 */
    public Map<String, BaselineSatelliteInfo> getBaselineSatellites() {
        return baselineSatellites;
    }

    /** 原子替换快照，避免跨线程读取正在修改的目录。 */
    public void setBaselineSatellites(Map<String, BaselineSatelliteInfo> satellites) {
        baselineSatellites = satellites == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(satellites));
    }
    /** 当前任务按校准公式索引的有效校准通道配置。 */
    private volatile Map<String, CalibrationChannelRuntimeConfig>
            calibrationConfigMap = Collections.emptyMap();

    public Long getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getMulticastIp() {
        return multicastIp;
    }

    public void setMulticastIp(String multicastIp) {
        this.multicastIp = multicastIp;
    }

    public Integer getTransferType() {
        return transferType;
    }

    public void setTransferType(Integer transferType) {
        this.transferType = transferType;
    }

    public Integer getTransferProtocol() {
        return transferProtocol;
    }

    public void setTransferProtocol(Integer transferProtocol) {
        this.transferProtocol = transferProtocol;
    }

    public Integer getRpcEnabled() {
        return rpcEnabled;
    }

    public void setRpcEnabled(Integer rpcEnabled) {
        this.rpcEnabled = rpcEnabled;
    }

    public Long getProtocolConfigId() {
        return protocolConfigId;
    }

    public void setProtocolConfigId(Long protocolConfigId) {
        this.protocolConfigId = protocolConfigId;
    }

    public Integer getParameterRangeCheckEnabled() {
        return parameterRangeCheckEnabled;
    }

    public void setParameterRangeCheckEnabled(
            Integer parameterRangeCheckEnabled) {
        this.parameterRangeCheckEnabled = parameterRangeCheckEnabled;
    }

    public String getProtocolConfigParams() {
        return protocolConfigParams;
    }

    public void setProtocolConfigParams(String protocolConfigParams) {
        this.protocolConfigParams = protocolConfigParams;
    }

    public Map<String, CalibrationChannelRuntimeConfig> getCalibrationConfigMap() {
        return calibrationConfigMap;
    }

    /** 原子替换当前接口使用的只读校准配置快照。 */
    public void setCalibrationConfigMap(
            Map<String, CalibrationChannelRuntimeConfig> calibrationConfigMap) {
        this.calibrationConfigMap = calibrationConfigMap == null
                ? Collections.emptyMap() : calibrationConfigMap;
    }

    /** 判断影响监听或处理分支的配置是否相同。 */
    public boolean hasSameRuntimeSettings(CollectInterfaceRuntimeConfig other) {
        // 逐项比较运行关键字段，任一变化都需要重启对应接口。
        return other != null
                && same(host, other.host)
                && same(taskId, other.taskId)
                && same(port, other.port)
                && same(multicastIp, other.multicastIp)
                && same(transferType, other.transferType)
                && same(transferProtocol, other.transferProtocol)
                && same(rpcEnabled, other.rpcEnabled)
                && same(receiveRateLimit, other.receiveRateLimit)
                && same(receiveRateLimitUnit, other.receiveRateLimitUnit)
                && same(protocolConfigId, other.protocolConfigId)
                && same(protocolConfigParams, other.protocolConfigParams);
    }

    /** 对可空配置值执行一致性比较。 */
    private boolean same(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }
}
