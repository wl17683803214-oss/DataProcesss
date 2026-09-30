package com.example.simulator.runtime;

import com.example.simulator.model.RunRecord;
import com.example.simulator.enums.RunStatus;
import com.example.simulator.mapper.SimulatorMapper;
import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;

/** 单次运行独立持有资源和统计，停止与写库通过同一实例协调。 */
public class ExecutionContext {
    public final RunRecord record;
    private volatile boolean cancelled;
    private volatile Thread worker;
    private Closeable resource;
    private boolean finished;

    public ExecutionContext(RunRecord record) {
        this.record = record;
    }

    /** 登记执行线程，处理启动后立即停止的竞争。 */
    public synchronized void begin() throws InterruptedException {
        worker = Thread.currentThread();
        check();
        record.statusCode = RunStatus.RUNNING.code;
    }

    /** 先登记未连接套接字，再连接，停止请求可及时解除阻塞。 */
    public synchronized void bind(Closeable resource) throws IOException, InterruptedException {
        if (cancelled) {
            resource.close();
            throw new InterruptedException("模拟源已停止");
        }
        this.resource = resource;
    }

    /** 运行线程频繁检查停止信号。 */
    public void check() throws InterruptedException {
        if (cancelled || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("模拟源已停止");
        }
    }

    /** 接受停止请求，先改变状态，再关闭网络资源和唤醒休眠。 */
    public synchronized void stop() {
        if (finished) {
            return;
        }
        cancelled = true;
        record.statusCode = RunStatus.STOPPING.code;
        closeResource();
        if (worker != null) {
            worker.interrupt();
        }
    }

    /** 供发送线程区分主动停止和实际发送异常。 */
    public boolean isStopRequested() {
        return cancelled;
    }

    /** 每次成功发送后更新内存统计，字节数包含协议头。 */
    public synchronized void sent(int bytes, boolean dataUnit) {
        record.sentByteCount += bytes;
        if (dataUnit) {
            record.sentUnitCount++;
        }
        record.lastSendTime = LocalDateTime.now();
    }

    /** 完整一轮发送完成后累计轮数。 */
    public synchronized void completeRound() {
        record.completedLoopCount++;
    }

    /** 每轮之间至少等待一毫秒，避免FEP已存在文件造成空转。 */
    public void pause(long millis) throws InterruptedException {
        check();
        if (millis > 0) {
            Thread.sleep(millis);
        }
    }

    /** 退出时先清理资源，再设置最终状态；异常原因保持中文。 */
    public synchronized void finish(Throwable failure) {
        closeResource();
        finished = true;
        record.statusCode = cancelled ? RunStatus.STOPPED.code
                : failure == null ? RunStatus.COMPLETED.code : RunStatus.FAILED.code;
        record.errorMessage = failure != null && !cancelled
                ? "发送失败，请检查源文件、目标连接及协议应答，详见服务日志" : null;
        record.endTime = LocalDateTime.now();
        worker = null;
    }

    /** 串行落库，避免旧统计覆盖停止或结束状态。 */
    public synchronized boolean flush(SimulatorMapper mapper) {
        mapper.updateRun(record);
        return finished;
    }

    /** 只关闭本次执行资源，连接异常时关闭失败不阻断最终状态保存。 */
    private void closeResource() {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignored) {
                // 套接字可能已由远端关闭，无需重复抛出。
            }
            resource = null;
        }
    }
}

