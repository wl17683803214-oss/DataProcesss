package com.example.dataprocess.collection;

import com.example.dataprocess.collection.receive.CollectInterfaceReceiver;
import com.example.dataprocess.collection.receive.TcpInterfaceReceiver;
import com.example.dataprocess.collection.receive.UdpInterfaceReceiver;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import com.example.dataprocess.entity.CalibrationChannelRuntimeConfig;
import com.example.dataprocess.mapper.CollectInterfaceRuntimeMapper;
import com.example.dataprocess.processing.handler.ProtocolHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/** 根据数据库配置动态启动和停止采集接口。 */
@Component
public class CollectInterfaceManager implements DisposableBean {

    /** 接口管理日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CollectInterfaceManager.class);
    /** UDP传输方式的数据库枚举值。 */
    private static final int TRANSFER_TYPE_UDP = 1;
    /** TCP传输方式的数据库枚举值。 */
    private static final int TRANSFER_TYPE_TCP = 2;
    /** 接口运行配置数据访问组件。 */
    private final CollectInterfaceRuntimeMapper runtimeMapper;
    /** 采集接口内存统计组件。 */
    private final CollectInterfaceStatistics statistics;
    /** 网络数据采集线程池。 */
    private final ThreadPoolExecutor collectExecutor;
    /** 协议处理器注册表。 */
    private final ProtocolHandlerRegistry protocolHandlerRegistry;
    /** 当前任务校准通道配置加载组件。 */
    private final CalibrationRuntimeConfigManager calibrationConfigManager;
    /** 当前已经启动的接口。 */
    private final Map<Long, RunningInterface> runningInterfaces =
            new HashMap<Long, RunningInterface>();
    /** 数据处理服务是否正在停止。 */
    private volatile boolean stopping;

    public CollectInterfaceManager(
            CollectInterfaceRuntimeMapper runtimeMapper,
            CollectInterfaceStatistics statistics,
            @Qualifier("dataCollectExecutor")
            ThreadPoolExecutor collectExecutor,
            ProtocolHandlerRegistry protocolHandlerRegistry,
            CalibrationRuntimeConfigManager calibrationConfigManager) {
        this.runtimeMapper = runtimeMapper;
        this.statistics = statistics;
        this.collectExecutor = collectExecutor;
        this.protocolHandlerRegistry = protocolHandlerRegistry;
        this.calibrationConfigManager = calibrationConfigManager;
    }

    /** 数据处理服务启动完成后自动恢复全部启用采集接口。 */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void startEnabledInterfacesAfterStartup() {
        if (stopping) {
            return;
        }
        try {
            // 第一步：从数据库读取全部启用且未删除的接口运行配置。
            List<CollectInterfaceRuntimeConfig> enabledConfigs =
                    runtimeMapper.findAllEnabledInterfaces();
            // 第二步：接口启动前按其共同任务加载一次有效校准通道配置。
            attachCalibrationConfigs(enabledConfigs);
            // 第三步：逐个提交接收任务，单个接口失败不会阻断其他接口启动。
            synchronizeRunningInterfaces(enabledConfigs);
            LOGGER.info("数据处理服务启动后采集接口恢复完成，启用接口数量：{}",
                    enabledConfigs.size());
        } catch (Exception exception) {
            // 数据库整体查询失败时保留服务进程，等待后续管理服务再次同步。
            LOGGER.error("数据处理服务启动后恢复采集接口失败，原因：{}",
                    exception.getMessage(), exception);
        }
    }

    /** 按Admin通知的完整接口主键列表同步当前运行接口。 */
    public synchronized void syncInterfaces(List<Long> interfaceIds) {
        if (stopping) {
            throw new IllegalStateException("数据处理服务正在停止，不能同步采集接口");
        }
        // 第一步：去除空主键和重复主键，得到Admin要求运行的完整列表。
        List<Long> normalizedIds = normalizeInterfaceIds(interfaceIds);
        // 第二步：空列表表示停止全部接口，不执行无意义的数据库IN查询。
        List<CollectInterfaceRuntimeConfig> configs = normalizedIds.isEmpty()
                ? new ArrayList<CollectInterfaceRuntimeConfig>()
                : runtimeMapper.findEnabledInterfacesByIds(normalizedIds);
        // 第三步：接口同步前按其共同任务刷新一次有效校准通道配置。
        attachCalibrationConfigs(configs);
        // 第四步：按数据库实际配置启动、更新和停止接口。
        synchronizeRunningInterfaces(configs);
        LOGGER.info("采集接口同步完成，通知数量：{}，实际启用数量：{}",
                normalizedIds.size(), configs.size());
    }

    /** 查询一次共同任务的有效校准通道，并让本批接口共用同一个只读Map。 */
    private void attachCalibrationConfigs(
            List<CollectInterfaceRuntimeConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        String taskId = configs.get(0).getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("启用采集接口未配置试验任务编号");
        }
        // 接口ID列表按业务约定只允许属于同一个试验任务。
        for (CollectInterfaceRuntimeConfig config : configs) {
            if (!taskId.equals(config.getTaskId())) {
                throw new IllegalArgumentException("启用采集接口不属于同一个试验任务");
            }
        }
        Map<String, CalibrationChannelRuntimeConfig> calibrationConfigs =
                calibrationConfigManager.load(taskId);
        for (CollectInterfaceRuntimeConfig config : configs) {
            config.setCalibrationConfigMap(calibrationConfigs);
        }
    }

    /** 清理Admin通知列表中的空值和重复值。 */
    private List<Long> normalizeInterfaceIds(List<Long> interfaceIds) {
        if (interfaceIds == null || interfaceIds.isEmpty()) {
            return new ArrayList<Long>();
        }
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<Long>();
        for (Long interfaceId : interfaceIds) {
            if (interfaceId != null) {
                normalizedIds.add(interfaceId);
            }
        }
        return new ArrayList<Long>(normalizedIds);
    }

    /** 根据数据库结果同步当前运行接口集合。 */
    private synchronized void synchronizeRunningInterfaces(
            List<CollectInterfaceRuntimeConfig> enabledConfigs) {
        Set<Long> enabledIds = new HashSet<Long>();
        for (CollectInterfaceRuntimeConfig config : enabledConfigs) {
            enabledIds.add(config.getInterfaceId());
            startOrRestart(config);
        }

        // 数据库中已禁用、删除或未出现在通知列表中的接口需要停止。
        Set<Long> currentIds = new HashSet<Long>(runningInterfaces.keySet());
        for (Long interfaceId : currentIds) {
            if (!enabledIds.contains(interfaceId)) {
                stopInterface(interfaceId, "接口已禁用或配置已移除");
            }
        }
    }

    /** 启动新接口，关键运行配置变化时先停止再重新启动。 */
    private void startOrRestart(CollectInterfaceRuntimeConfig config) {
        RunningInterface current = runningInterfaces.get(config.getInterfaceId());
        if (current != null
                && current.getConfig().hasSameRuntimeSettings(config)) {
            // 监听参数未变化时直接替换校准Map和规则开关，避免重启网络监听。
            current.getConfig().setCalibrationConfigMap(
                    config.getCalibrationConfigMap());
            current.getConfig().setParameterRangeCheckEnabled(
                    config.getParameterRangeCheckEnabled());
            return;
        }
        if (current != null) {
            stopInterface(config.getInterfaceId(), "接口运行配置发生变化");
        }

        CollectInterfaceReceiver receiver = null;
        try {
            // 第一步：本管理器只根据传输方式创建UDP或TCP接收任务。
            Integer transferType = config.getTransferType();
            if (transferType == null) {
                throw new IllegalArgumentException("采集接口没有配置传输方式");
            }
            switch (transferType) {
                case TRANSFER_TYPE_UDP:
                    // UDP接收器内部完成端口绑定和协议会话组装。
                    receiver = UdpInterfaceReceiver.create(
                            config, protocolHandlerRegistry);
                    break;
                case TRANSFER_TYPE_TCP:
                    // TCP接收器内部完成端口绑定和协议会话组装。
                    receiver = TcpInterfaceReceiver.create(
                            config, protocolHandlerRegistry);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "暂不支持的采集传输方式：" + transferType);
            }

            // 第二步：这里是进入采集线程的切换点，由采集线程池调用receiver.run()。
            // submit立即返回Future任务句柄，Admin请求线程不会进入长期阻塞的网络接收循环。
            Future<?> future = collectExecutor.submit(receiver);
            // 第三步：保存接口配置、接收任务和Future，供配置变更和停止接口使用。
            runningInterfaces.put(
                    config.getInterfaceId(),
                    new RunningInterface(config, receiver, future));
            LOGGER.info("采集接口已启动，接口编号：{}，传输方式：{}，传输协议：{}，地址：{}，端口：{}",
                    config.getInterfaceId(), config.getTransferType(),
                    config.getTransferProtocol(), config.getHost(), config.getPort());
        } catch (RejectedExecutionException exception) {
            // 接收任务未进入线程池时立即释放已经绑定的网络端口。
            stopCreatedReceiver(receiver);
            LOGGER.error("采集线程池容量不足，接口编号：{}",
                    config.getInterfaceId(), exception);
        } catch (Exception exception) {
            // 启动中途失败时释放已经创建的接收器，避免端口残留占用。
            stopCreatedReceiver(receiver);
            LOGGER.error("采集接口启动失败，接口编号：{}",
                    config.getInterfaceId(), exception);
        }
    }

    /** 停止尚未登记到运行集合中的接收器。 */
    private void stopCreatedReceiver(CollectInterfaceReceiver receiver) {
        // 接收器尚未创建时无需执行清理。
        if (receiver != null) {
            receiver.stop();
        }
    }

    /** 停止指定接口并释放网络端口。 */
    private void stopInterface(Long interfaceId, String reason) {
        RunningInterface runningInterface = runningInterfaces.remove(interfaceId);
        if (runningInterface == null) {
            return;
        }
        // 先关闭接收器解除网络阻塞，再取消线程池中的接收任务。
        runningInterface.stop();
        LOGGER.info("采集接口已停止，接口编号：{}，原因：{}", interfaceId, reason);
    }

    /** 应用关闭时停止全部运行接口并最后回写统计。 */
    @Override
    public synchronized void destroy() {
        stopping = true;
        Set<Long> interfaceIds = new HashSet<Long>(runningInterfaces.keySet());
        for (Long interfaceId : interfaceIds) {
            stopInterface(interfaceId, "数据处理服务正在关闭");
        }
        statistics.flush();
    }

    /** 一个已经启动的采集接口运行句柄。 */
    private static final class RunningInterface {

        /** 启动时的接口配置快照。 */
        private final CollectInterfaceRuntimeConfig config;
        /** 负责该接口收包的任务。 */
        private final CollectInterfaceReceiver receiver;
        /** 采集线程池任务句柄。 */
        private final Future<?> future;

        private RunningInterface(
                CollectInterfaceRuntimeConfig config,
                CollectInterfaceReceiver receiver,
                Future<?> future) {
            this.config = config;
            this.receiver = receiver;
            this.future = future;
        }

        private CollectInterfaceRuntimeConfig getConfig() {
            return config;
        }

        /** 停止接收任务。 */
        private void stop() {
            receiver.stop();
            future.cancel(true);
        }
    }
}
