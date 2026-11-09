package com.example.dataadmin.service;

import com.example.common.auth.LoginUser;
import com.example.common.enums.TelemetryFrameStatus;
import com.example.common.greptime.TelemetryFrameRecord;
import com.example.common.telemetry.TelemetryRealtimeBatch;
import com.example.common.telemetry.TelemetryRealtimeCache;
import com.example.common.telemetry.TelemetryRealtimeProperties;
import com.example.dataadmin.config.WebSocketTokenInterceptor;
import com.example.dataadmin.enums.TelemetrySocketStream;
import com.example.dataadmin.service.support.TelemetryFrameAssembler;
import com.example.security.context.LoginUserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** 每条浏览器连接独立订阅一个页面数据范围，慢连接不会阻塞采集和入库。 */
public class TelemetryWebSocketHandler extends TextWebSocketHandler implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryWebSocketHandler.class);
    private static final int SESSION_LIMIT = 128;
    private static final int SESSION_QUEUE_LIMIT = 16;
    private static final int CLIENT_MESSAGE_LIMIT = 8192;
    private static final int SERVER_MESSAGE_LIMIT = 1048576;
    private static final int FRAME_ID_HISTORY = 512;
    private static final CloseStatus RESYNC = new CloseStatus(1013, "实时数据需要重新同步");

    private final ObjectMapper mapper;
    private final WebSocketTokenInterceptor authentication;
    private final ProcessedTelemetryQueryService query;
    private final TelemetryRealtimeCache cache;
    private final TelemetryRealtimeProperties properties;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Semaphore slots = new Semaphore(SESSION_LIMIT);
    private final AtomicBoolean redisGap = new AtomicBoolean();
    private final ThreadPoolExecutor senders = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(SESSION_LIMIT), task -> {
                Thread thread = new Thread(task, "遥测连接发送线程");
                thread.setDaemon(true);
                return thread;
            });
    /** 关闭慢连接也使用独立线程，避免影响其他连接的增量分发。 */
    private final ThreadPoolExecutor closers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(SESSION_LIMIT), task -> {
                Thread thread = new Thread(task, "遥测连接关闭线程");
                thread.setDaemon(true);
                return thread;
            });

    public TelemetryWebSocketHandler(ObjectMapper mapper, WebSocketTokenInterceptor authentication,
            ProcessedTelemetryQueryService query, TelemetryRealtimeCache cache, TelemetryRealtimeProperties properties) {
        this.mapper = mapper;
        this.authentication = authentication;
        this.query = query;
        this.cache = cache;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 限制连接总数与首条请求大小，订阅通过前不接收业务消息。
        if (!slots.tryAcquire()) {
            closeSocket(session, RESYNC);
            return;
        }
        session.setTextMessageSizeLimit(CLIENT_MESSAGE_LIMIT);
        connections.put(session.getId(), new Connection(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Connection connection = connections.get(session.getId());
        if (connection == null) {
            return;
        }
        try {
            // 第一步：每条连接只绑定一个范围，切换范围由前端重新建立订阅。
            if (connection.subscription != null || message.getPayloadLength() > CLIENT_MESSAGE_LIMIT) {
                connection.stop(CloseStatus.POLICY_VIOLATION);
                return;
            }
            JsonNode request = mapper.readTree(message.getPayload());
            if (request == null || !request.isObject()) {
                throw new IllegalArgumentException("订阅消息必须是JSON对象");
            }
            String token = request.path("accessToken").asText();
            LoginUser user = authentication.authenticate(token);
            // 第二步：在登录上下文中校验任务和组件的勾选范围。
            LoginUserContext.set(user);
            Subscription subscription = subscribe(request);
            // 第三步：快照任务排在增量任务前，同连接只由一个工作线程发送。
            connection.start(subscription, token);
        } catch (IllegalArgumentException | SecurityException exception) {
            connection.reject(exception.getMessage(), CloseStatus.POLICY_VIOLATION);
        } catch (Exception exception) {
            connection.reject("遥测订阅初始化失败", CloseStatus.SERVER_ERROR);
        } finally {
            LoginUserContext.clear();
        }
    }

    private Subscription subscribe(JsonNode request) {
        String taskId = request.path("taskId").asText().trim();
        if (taskId.isEmpty()) {
            throw new IllegalArgumentException("试验任务编号不能为空");
        }
        TelemetrySocketStream stream = TelemetrySocketStream.require(request.path("stream").asText());
        // 整帧连接只能订阅一个采集接口，正常与异常分别隔离。
        if (stream == TelemetrySocketStream.NORMAL || stream == TelemetrySocketStream.ABNORMAL) {
            long interfaceId = integer(request, "interfaceId", 0);
            if (interfaceId <= 0) {
                throw new IllegalArgumentException("采集接口主键不能为空且必须大于零");
            }
            return new Subscription(taskId, stream, interfaceId, null);
        }
        // 参数连接按固定页面或组件读取已保存的勾选范围。
        boolean curve = stream == TelemetrySocketStream.CURVE;
        long selectionType = integer(request, "selectionType", curve ? 2 : 1);
        if (selectionType < 1 || selectionType > 4) {
            throw new IllegalArgumentException("参数勾选场景不正确");
        }
        long targetId = integer(request, "targetId", 0);
        return new Subscription(taskId, stream, null, query.prepareLive(taskId, (int) selectionType, targetId,
                request.path("telemetryType").asText(null), curve));
    }

    private long integer(JsonNode request, String field, long fallback) {
        JsonNode value = request.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        // 主键可用十进制字符串传输，避免浏览器对大整数取整。
        if (value.isIntegralNumber() && value.canConvertToLong()) {
            return value.longValue();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.textValue());
            } catch (NumberFormatException ignored) {
                // 按统一的字段错误反馈。
            }
        }
        throw new IllegalArgumentException(field + "必须为有效整数");
    }

    /** Redis监听队列超限时登记中断，后续通知已订阅连接重新同步。 */
    public void signalRedisGap() {
        redisGap.set(true);
    }

    /** 监听线程只解析并提交发送任务，浏览器网络写操作由独立线程执行。 */
    public void receive(byte[] body) {
        if (connections.isEmpty()) {
            return;
        }
        try {
            if (body.length > TelemetryRealtimeBatch.MAX_BYTES) {
                signalRedisGap();
                return;
            }
            TelemetryRealtimeBatch batch = mapper.readValue(body, TelemetryRealtimeBatch.class);
            boolean gap = redisGap.getAndSet(false) || batch.isGap();
            for (Connection connection : connections.values()) {
                Subscription subscription = connection.subscription;
                if (subscription == null) {
                    continue;
                }
                if (gap) {
                    connection.stop(RESYNC);
                } else if (subscription.matches(batch)) {
                    connection.offer(() -> connection.sendUpdates(subscription, batch));
                }
            }
        } catch (IOException | RuntimeException exception) {
            signalRedisGap();
        }
    }

    /** 回收未订阅连接，并定期验证已登录连接仍持有有效会话。 */
    @Scheduled(fixedDelay = 5000)
    public void expireUnsubscribed() {
        long now = System.nanoTime();
        long deadline = now - TimeUnit.SECONDS.toNanos(15);
        for (Connection connection : connections.values()) {
            if (connection.subscription == null && connection.createdAt < deadline) {
                connection.stop(CloseStatus.POLICY_VIOLATION);
            } else if (connection.subscription != null && now >= connection.nextAuthCheck) {
                connection.nextAuthCheck = now + TimeUnit.MINUTES.toNanos(1);
                try {
                    authentication.authenticate(connection.token);
                } catch (SecurityException exception) {
                    connection.stop(CloseStatus.POLICY_VIOLATION);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Connection connection = connections.get(session.getId());
        if (connection != null) {
            connection.remove();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Connection connection = connections.get(session.getId());
        if (connection != null) {
            connection.stop(CloseStatus.SERVER_ERROR);
        }
    }

    private void closeSocket(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException exception) {
            LOGGER.debug("遥测连接关闭失败", exception);
        }
    }

    @Override
    public void close() {
        // 应用关闭时释放页面连接和专用发送线程。
        for (Connection connection : connections.values()) {
            connection.stop(CloseStatus.GOING_AWAY);
        }
        senders.shutdownNow();
        closers.shutdown();
    }

    /** 一次订阅只处理匹配任务的参数或匹配接口和状态的帧。 */
    private static final class Subscription {
        private final String taskId;
        private final TelemetrySocketStream stream;
        private final Long interfaceId;
        private final ProcessedTelemetryQueryService.LiveSelection selection;

        private Subscription(String taskId, TelemetrySocketStream stream, Long interfaceId,
                ProcessedTelemetryQueryService.LiveSelection selection) {
            this.taskId = taskId;
            this.stream = stream;
            this.interfaceId = interfaceId;
            this.selection = selection;
        }

        private TelemetryFrameStatus status() {
            return stream == TelemetrySocketStream.NORMAL ? TelemetryFrameStatus.NORMAL : TelemetryFrameStatus.ABNORMAL;
        }

        private boolean matches(TelemetryFrameRecord frame) {
            return taskId.equals(frame.getTaskId()) && interfaceId.equals(frame.getInterfaceId())
                    && status().getValue().equals(frame.getCheckStatus());
        }

        private boolean matches(TelemetryRealtimeBatch batch) {
            if (selection != null) {
                return batch.getParameters().stream().anyMatch(row -> taskId.equals(row.getTaskId()));
            }
            return batch.getFrames().stream().anyMatch(this::matches);
        }
    }

    /** 同连接按先进先出发送，快照与增量共享帧标识去重。 */
    private final class Connection {
        private final WebSocketSession session;
        private final long createdAt = System.nanoTime();
        private final Deque<Work> pending = new ArrayDeque<>();
        private final Set<String> seenFrames = new LinkedHashSet<>();
        private volatile Subscription subscription;
        private String token;
        private volatile long nextAuthCheck;
        private boolean scheduled;
        private boolean stopped;
        private long sequence;
        private long snapshotFrameOrder;

        private Connection(WebSocketSession session) {
            this.session = session;
        }

        private void start(Subscription value, String accessToken) {
            synchronized (this) {
                if (stopped || subscription != null) {
                    return;
                }
                // 先占住队首，再让Redis监听线程看到该订阅。
                pending.addLast(() -> sendSnapshot(value));
                scheduled = true;
                token = accessToken;
                nextAuthCheck = System.nanoTime() + TimeUnit.MINUTES.toNanos(1);
                subscription = value;
            }
            schedule();
        }

        private void offer(Work work) {
            boolean launch = false;
            boolean overflow;
            synchronized (this) {
                if (stopped) {
                    return;
                }
                overflow = pending.size() >= SESSION_QUEUE_LIMIT;
                if (!overflow) {
                    pending.addLast(work);
                    if (!scheduled) {
                        scheduled = true;
                        launch = true;
                    }
                }
            }
            if (overflow) {
                stop(RESYNC);
            } else if (launch) {
                schedule();
            }
        }

        private void schedule() {
            try {
                senders.execute(this::drainOne);
            } catch (RejectedExecutionException exception) {
                stop(RESYNC);
            }
        }

        private void drainOne() {
            Work work;
            synchronized (this) {
                work = stopped ? null : pending.pollFirst();
                if (work == null) {
                    scheduled = false;
                    return;
                }
            }
            try {
                work.run();
            } catch (Exception exception) {
                stop(CloseStatus.SERVER_ERROR);
                return;
            }
            // 一次执行一个发送任务，活跃连接轮流占用线程。
            synchronized (this) {
                if (stopped || pending.isEmpty()) {
                    scheduled = false;
                    return;
                }
            }
            schedule();
        }

        private void sendSnapshot(Subscription value) throws IOException {
            List<?> rows;
            if (value.selection == null) {
                List<TelemetryFrameRecord> snapshot = cache.frames(value.taskId, value.interfaceId, value.status());
                rows = frameRows(value, snapshot);
                // 快照之前的旧增量即使已被淘汰，也不能在初始化后重新进入列表。
                for (TelemetryFrameRecord frame : snapshot) {
                    snapshotFrameOrder = Math.max(snapshotFrameOrder, frame.getRealtimeOrder());
                }
            } else {
                rows = value.selection.snapshot();
            }
            send("snapshot", value, rows, true);
        }

        private void sendUpdates(Subscription value, TelemetryRealtimeBatch batch) throws IOException {
            List<?> rows = value.selection == null ? frameRows(value, batch.getFrames())
                    : value.selection.updates(batch.getParameters());
            if (!rows.isEmpty()) {
                send("delta", value, rows, true);
            }
        }

        private List<ObjectNode> frameRows(Subscription value, List<TelemetryFrameRecord> rows) {
            List<ObjectNode> result = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (TelemetryFrameRecord row : rows) {
                // 快照初始化期间同一条帧可能也在增量队列中，只发送一次。
                if (!value.matches(row) || row.getRealtimeOrder() <= snapshotFrameOrder
                        || row.getExpiresAt() <= now || !seenFrames.add(row.getRealtimeId())) {
                    continue;
                }
                if (seenFrames.size() > FRAME_ID_HISTORY) {
                    seenFrames.remove(seenFrames.iterator().next());
                }
                List<TelemetryFrameRecord> single = Collections.singletonList(row);
                Object view = value.stream == TelemetrySocketStream.NORMAL
                        ? TelemetryFrameAssembler.normal(single).get(0) : TelemetryFrameAssembler.abnormal(single).get(0);
                ObjectNode payload = mapper.valueToTree(view);
                payload.put("frameId", row.getRealtimeId());
                payload.put("expiresAt", row.getExpiresAt());
                payload.put("cachedBytes", row.getRealtimeBytes());
                payload.put("frameOrder", row.getRealtimeOrder());
                result.add(payload);
            }
            return result;
        }

        private void send(String type, Subscription value, List<?> rows, boolean complete) throws IOException {
            // 帧快照原码可能较大，拆分消息但保持序号和快照完成标记。
            ObjectNode response = mapper.createObjectNode();
            response.put("type", type);
            response.put("stream", value.stream.getValue());
            response.put("sequence", sequence + 1);
            response.put("complete", complete);
            response.put("retentionSeconds", properties.getRetentionSeconds());
            if (value.interfaceId != null) {
                response.put("frameCapacityBytes", properties.getFrameCapacityBytes());
            }
            response.set("data", mapper.valueToTree(rows));
            byte[] payload = mapper.writeValueAsBytes(response);
            if (payload.length > SERVER_MESSAGE_LIMIT && rows.size() > 1) {
                int middle = rows.size() / 2;
                send(type, value, rows.subList(0, middle), false);
                send(type, value, rows.subList(middle, rows.size()), complete);
                return;
            }
            if (payload.length > SERVER_MESSAGE_LIMIT) {
                stop(RESYNC);
                return;
            }
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
                sequence++;
            }
        }

        private void reject(String message, CloseStatus status) {
            try {
                ObjectNode error = mapper.createObjectNode();
                error.put("type", "error");
                error.put("message", message);
                session.sendMessage(new TextMessage(error.toString()));
            } catch (IOException exception) {
                LOGGER.debug("遥测订阅错误响应发送失败", exception);
            } finally {
                stop(status);
            }
        }

        private void remove() {
            synchronized (this) {
                stopped = true;
                pending.clear();
            }
            if (connections.remove(session.getId(), this)) {
                slots.release();
            }
        }

        private void stop(CloseStatus status) {
            synchronized (this) {
                if (stopped) {
                    return;
                }
                stopped = true;
                pending.clear();
            }
            try {
                // 关闭完成前保留连接名额，关闭队列与连接数共用同一个上限。
                closers.execute(() -> {
                    try {
                        closeSocket(session, status);
                    } finally {
                        remove();
                    }
                });
            } catch (RejectedExecutionException exception) {
                remove();
            }
        }
    }

    /** 同连接的快照和增量都由发送线程执行。 */
    @FunctionalInterface
    private interface Work {
        void run() throws IOException;
    }
}
