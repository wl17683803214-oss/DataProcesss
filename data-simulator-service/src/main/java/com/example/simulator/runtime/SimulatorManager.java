package com.example.simulator.runtime;

import com.example.simulator.config.SimulatorProperties;
import com.example.simulator.enums.ProtocolProfile;
import com.example.simulator.enums.RunStatus;
import com.example.simulator.model.RunRecord;
import com.example.simulator.model.PreparedRun;
import com.example.simulator.mapper.SimulatorMapper;
import com.example.simulator.protocol.SimulatorSender;
import com.example.simulator.service.RunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/** 单进程多任务运行管理器，每个源同一时间只占用一个槽位。 */
@Component
public class SimulatorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorManager.class);
    private final RunService runs;
    private final SimulatorMapper mapper;
    private final SimulatorProperties properties;
    private final Map<Long, ExecutionContext> active = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final SimulatorSender sender;
    private volatile boolean closing;
    private FileChannel lockChannel;
    private FileLock processLock;
    private final SqlSessionFactory sessions;
    private SqlSession lockSession;
    private boolean databaseLocked;

    public SimulatorManager(RunService runs, SimulatorMapper mapper, SimulatorProperties properties,
                            SqlSessionFactory sessions) {
        this.runs = runs;
        this.mapper = mapper;
        this.properties = properties;
        this.sessions = sessions;
        // 容量由管理器先检查，直接移交线程，禁止隐式排队。
        this.executor = new ThreadPoolExecutor(0, properties.getMaxConcurrent(), 60,
                TimeUnit.SECONDS, new SynchronousQueue<>(), runnable -> {
                    Thread thread = new Thread(runnable, "模拟源发送线程");
                    thread.setDaemon(false);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        this.sender = new SimulatorSender(properties);
    }

    /** 获取本机进程锁后恢复上次遗留状态，同库只允许部署一个服务进程。 */
    @PostConstruct
    public void initialize() throws Exception {
        Path path = Paths.get(properties.getLockFile()).toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        lockChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            processLock = lockChannel.tryLock();
            if (processLock == null) {
                throw new IllegalStateException("模拟源服务已经运行，不能重复启动");
            }
            // 单独持有数据库会话，只有取得锁的进程才允许恢复历史状态。
            lockSession = sessions.openSession(true);
            databaseLocked = lockSession.getMapper(SimulatorMapper.class).acquireServiceLock();
            if (!databaseLocked) {
                throw new IllegalStateException("该数据库已有模拟源服务运行，请勿重复部署");
            }
            mapper.recoverRuns();
        } catch (Exception exception) {
            releaseLock();
            throw exception;
        }
    }

    /** 提交前占用槽位，数据库提交失败不会产生发送任务。 */
    public synchronized long start(long sourceId) throws Exception {
        checkOwnership();
        if (closing) {
            throw new IllegalStateException("服务正在停止");
        }
        if (active.containsKey(sourceId)) {
            throw new IllegalStateException("该模拟源已有执行任务或待保存的结束记录");
        }
        if (active.size() >= properties.getMaxConcurrent()) {
            throw new IllegalStateException("同时运行的模拟源已达到服务上限");
        }
        PreparedRun prepared = runs.prepare(sourceId);
        ExecutionContext context = new ExecutionContext(prepared.record);
        active.put(sourceId, context);
        try {
            executor.execute(() -> execute(prepared, context));
        } catch (RejectedExecutionException exception) {
            // 提交失败仍保留可追溯的异常记录，定时任务负责失败重试。
            context.finish(exception);
            flushOne(sourceId, context);
            throw new IllegalStateException("发送线程暂不可用，请稍后重试");
        }
        return prepared.record.id;
    }

    /** 携带运行编号停止，旧请求不能影响下一轮执行。 */
    public synchronized void stop(long sourceId, long runId) {
        RunRecord record = mapper.findRun(runId);
        if (record == null || record.sourceId != sourceId) {
            throw new IllegalArgumentException("运行记录不属于该模拟源");
        }
        ExecutionContext context = active.get(sourceId);
        if (context != null && context.record.id == runId) {
            context.stop();
            flushOne(sourceId, context);
        } else if (record.statusCode <= 3) {
            throw new IllegalStateException("运行实例无法确认，请检查服务状态");
        }
        // 已结束的相同运行重复停止属于幂等操作。
    }

    /** 执行完成后无论成功与否都释放资源，并尝试保存最终统计。 */
    private void execute(PreparedRun prepared, ExecutionContext context) {
        Throwable failure = null;
        try {
            context.begin();
            // 发送任务实际开始时记录模拟源及本次运行信息。
            LOGGER.info("模拟源开始发送，名称：{}，模拟源编号：{}，运行编号：{}，源文件：{}，目标地址：{}:{}",
                    prepared.config.sourceName, prepared.config.id, context.record.id,
                    prepared.config.filePath, prepared.config.targetHost, prepared.config.targetPort);
            flushOne(prepared.config.id, context);
            sender.send(prepared.config, context);
        } catch (Exception exception) {
            failure = exception;
            // 主动停止由结束日志说明，仅真正发送失败才打印异常堆栈。
            if (!context.isStopRequested()) {
                LOGGER.warn("模拟源发送失败，名称：{}，模拟源编号：{}，运行编号：{}",
                        prepared.config.sourceName, prepared.config.id, context.record.id, exception);
            }
        } finally {
            context.finish(failure);
            // 按协议说明本次累计发送数量，停止和异常结束也记录实际已发送量。
            String countUnit = ProtocolProfile.of(prepared.config.transferProtocol) == ProtocolProfile.PDXP
                    ? "帧" : "个数据单元";
            LOGGER.info("模拟源发送结束，名称：{}，模拟源编号：{}，运行编号：{}，状态：{}，发送数量：{}{}",
                    prepared.config.sourceName, prepared.config.id, context.record.id,
                    RunStatus.of(context.record.statusCode).label,
                    context.record.sentUnitCount, countUnit);
            flushOne(prepared.config.id, context);
        }
    }

    /** 周期保存统计，也重试结束记录的数据库写入。 */
    @Scheduled(fixedDelay = 2000)
    public void flush() {
        checkOwnership();
        for (Map.Entry<Long, ExecutionContext> entry : active.entrySet()) {
            flushOne(entry.getKey(), entry.getValue());
        }
    }

    /** 只有最终记录保存成功才释放活动槽位。 */
    private void flushOne(long sourceId, ExecutionContext context) {
        try {
            if (context.flush(mapper)) {
                active.remove(sourceId, context);
            }
        } catch (Exception exception) {
            LOGGER.error("保存模拟源运行记录失败，将继续重试，模拟源编号：{}", sourceId, exception);
        }
    }

    /** 服务退出时逐一停止实例，等待发送线程清理完成。 */
    @PreDestroy
    public synchronized void shutdown() {
        closing = true;
        for (ExecutionContext context : active.values()) {
            context.stop();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        } finally {
            flush();
            releaseLock();
        }
    }

    /** 释放文件锁但保留文件，避免删除锁文件导致并发锁失效。 */
    private void releaseLock() {
        try {
            if (lockSession != null) {
                try {
                    if (databaseLocked) {
                        lockSession.getMapper(SimulatorMapper.class).releaseServiceLock();
                    }
                } finally {
                    databaseLocked = false;
                    lockSession.close();
                    lockSession = null;
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("释放模拟源数据库会话锁失败", exception);
        }
        try {
            if (processLock != null) {
                processLock.release();
            }
            if (lockChannel != null) {
                lockChannel.close();
            }
        } catch (Exception exception) {
            LOGGER.warn("释放模拟源进程锁失败", exception);
        }
    }

    /** 持锁会话断开时停止发送并拒绝新启动，避免失去所有权后继续发送。 */
    private synchronized void checkOwnership() {
        if (closing) {
            return;
        }
        try {
            if (lockSession != null && lockSession.getConnection().isValid(2)) {
                return;
            }
        } catch (Exception exception) {
            LOGGER.error("模拟源数据库会话检查失败", exception);
        }
        closing = true;
        for (ExecutionContext context : active.values()) {
            context.stop();
        }
        LOGGER.error("模拟源服务失去数据库会话，已停止发送，请恢复数据库后重启服务");
    }
}

