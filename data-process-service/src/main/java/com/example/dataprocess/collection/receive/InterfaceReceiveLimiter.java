package com.example.dataprocess.collection.receive;

import com.example.common.enums.CollectionRateUnit;
import com.example.dataprocess.entity.CollectInterfaceRuntimeConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.time.Duration;

/** 单个接口共用的字节令牌桶，TCP等待额度，UDP整包尝试消费。 */
public final class InterfaceReceiveLimiter {
    /** UDP合法报文的最大载荷额度，避免低速配置永远无法接收大包。 */
    private static final long MAX_DATAGRAM_BYTES = 65535L;
    /** 未配置上限时不创建令牌桶。 */
    private final Bucket bucket;
    /** 停止标记与等待唤醒共用当前对象监视器。 */
    private boolean stopped;

    /** 接口启动时确定容量，最多积累一秒额度或一个完整UDP包。 */
    public InterfaceReceiveLimiter(CollectInterfaceRuntimeConfig config) {
        if (config.getReceiveRateLimit() == null) {
            bucket = null;
            return;
        }
        long rate = CollectionRateUnit.require(config.getReceiveRateLimitUnit())
                .bytesPerSecond(config.getReceiveRateLimit());
        long capacity = Integer.valueOf(1).equals(config.getTransferType())
                ? Math.max(rate, MAX_DATAGRAM_BYTES) : rate;
        bucket = Bucket.builder().addLimit(Bandwidth.classic(capacity,
                Refill.greedy(rate, Duration.ofSeconds(1)))).build();
    }

    /** UDP不等待，有足够额度才允许整个报文进入处理。 */
    public synchronized boolean acceptDatagram(int length) {
        return !stopped && (bucket == null || length == 0 || bucket.tryConsume(length));
    }

    /** TCP只申请当前可用额度，等待期间可由接口停止立即唤醒。 */
    private synchronized int acquire(int requested) throws IOException {
        while (!stopped) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("接收限速等待被中断");
            }
            long available = bucket == null ? requested : bucket.tryConsumeAsMuchAsPossible(requested);
            if (available > 0) {
                return (int) available;
            }
            try {
                // 短间隔等待不占用处理线程，并在关闭接口时即时解除。
                wait(10L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("接收限速等待被中断");
            }
        }
        throw new IOException("采集接口已停止接收");
    }

    /** 实际读取不足或读取失败时归还预先申请的额度。 */
    private void refund(int reserved, int actual) {
        if (bucket != null && reserved > Math.max(0, actual)) {
            bucket.addTokens(reserved - Math.max(0, actual));
        }
    }

    /** 包装原始输入流，所有协议共用限速读取，组帧逻辑保持不变。 */
    public InputStream wrap(InputStream input) {
        // 未配置限速时保持原始读取路径，避免逐字节产生额外开销。
        if (bucket == null) {
            return input;
        }
        return new FilterInputStream(input) {
            /** 单字节读取同样先取得额度。 */
            @Override
            public int read() throws IOException {
                int reserved = acquire(1);
                int result = -1;
                try {
                    result = in.read();
                    return result;
                } finally {
                    refund(reserved, result < 0 ? 0 : 1);
                }
            }

            /** 批量读取不能超过当前额度，按实际字节扣减。 */
            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                if (bytes == null) { throw new NullPointerException("接收缓冲区不能为空"); }
                if (offset < 0 || length < 0 || offset > bytes.length - length) {
                    throw new IndexOutOfBoundsException("接收缓冲区范围不正确");
                }
                if (length == 0) { return 0; }
                int reserved = acquire(length);
                int actual = -1;
                try {
                    actual = in.read(bytes, offset, reserved);
                    return actual;
                } finally {
                    refund(reserved, actual);
                }
            }
        };
    }

    /** 停止时唤醒全部限速等待，不依赖额度自然恢复。 */
    public synchronized void stop() {
        stopped = true;
        notifyAll();
    }
}
