package com.example.common.appstore;

import com.example.common.tool.HttpRequestTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** 异步发送日志，并在独立线程上定时向应用商店上报状态。 */
public class AppStoreReporter implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppStoreReporter.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STATUS_KEY_PREFIX = "sjcl:app-store:status:";
    private static final int LOG_QUEUE_CAPACITY = 256;
    private static final long STATUS_CHECK_MILLIS = 5_000L;
    private static final long WARNING_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final ThreadPoolExecutor logExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(LOG_QUEUE_CAPACITY),
            task -> new Thread(task, "应用商店日志上报"));
    private final ScheduledExecutorService statusExecutor = Executors.newSingleThreadScheduledExecutor(
            task -> new Thread(task, "应用商店状态上报"));
    private final AtomicLong lastQueueWarning = new AtomicLong();
    private final AtomicLong lastLogFailure = new AtomicLong();
    private final AtomicLong lastStatusFailure = new AtomicLong();

    private final AppStoreReportProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final String appName;
    private final URI baseUri;
    private final HttpRequestTool httpRequestTool;

    AppStoreReporter(AppStoreReportProperties properties, ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.appName = StringUtils.trimWhitespace(properties.getAppName());

        // 配置无效时停用本次启动的上报组件，正常启动业务服务。
        URI configuredUri = null;
        HttpRequestTool configuredTool = null;
        if (properties.isLogEnabled() || properties.isStatusEnabled()) {
            try {
                configuredUri = buildBaseUri(properties, appName);
                configuredTool = new HttpRequestTool(
                        properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis());
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("应用商店上报已停用，原因：{}", exception.getMessage());
            }
        }
        this.baseUri = configuredUri;
        this.httpRequestTool = configuredTool;
    }

    /** 服务就绪后启动状态上报，日志积压时状态仍可独立发送。 */
    @EventListener(ApplicationReadyEvent.class)
    public void startStatusReporting() {
        if (httpRequestTool == null || !properties.isStatusEnabled()) {
            return;
        }

        // 多实例定期检查共享上报资格，单实例直接按配置周期发送。
        long interval = properties.getStatusIntervalMillis();
        long checkDelay = properties.isStatusDeduplicate() ? Math.min(interval, STATUS_CHECK_MILLIS) : interval;
        statusExecutor.scheduleAtFixedRate(this::reportStatus, 0L, checkDelay, TimeUnit.MILLISECONDS);
    }

    /** 日志摘要在事务提交后进入有界队列，入队操作不会等待网络。 */
    public void submitLogAfterCommit(AppStoreLogLevel level, String message, LocalDateTime eventTime) {
        if (httpRequestTool == null || !properties.isLogEnabled() || level == null || !StringUtils.hasText(message)) {
            return;
        }

        // 固定本次上报参数，避免异步线程访问业务对象或事务上下文。
        Runnable enqueue = () -> enqueueLog(level, message, eventTime);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.run();
                }
            });
        } else {
            enqueue.run();
        }
    }

    /** 队列满时保留本地日志或告警，避免阻塞业务线程。 */
    private void enqueueLog(AppStoreLogLevel level, String message, LocalDateTime eventTime) {
        try {
            logExecutor.execute(() -> {
                LocalDateTime time = eventTime == null ? LocalDateTime.now() : eventTime;
                send("日志", () -> reportUri("saveAppLog").queryParam("level", "{level}")
                        .queryParam("msg", "{message}").encode()
                        .buildAndExpand(appName, DATE_FORMAT.format(time), level.name(), message)
                        .toUriString(), lastLogFailure);
            });
        } catch (RejectedExecutionException exception) {
            if (!logExecutor.isShutdown() && allowWarning(lastQueueWarning)) {
                LOGGER.warn("应用商店日志上报队列已满，部分日志仅保留在本地");
            }
        }
    }

    /** 同一应用的多个副本使用原子操作获取周期内唯一的发送资格。 */
    private void reportStatus() {
        if (properties.isStatusDeduplicate()) {
            try {
                long interval = properties.getStatusIntervalMillis();
                String statusKey = STATUS_KEY_PREFIX + baseUri + ":" + appName;
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                        statusKey, "1", interval, TimeUnit.MILLISECONDS);
                if (!Boolean.TRUE.equals(acquired)) {
                    return;
                }
            } catch (RuntimeException exception) {
                if (allowWarning(lastStatusFailure)) {
                    LOGGER.warn("应用商店状态上报未能取得发送资格，将在下次检查时重试");
                }
                return;
            }
        }

        // 使用当前时间上报在线状态，两个接口共用应用名称和地址前缀。
        send("状态", () -> reportUri("saveAppStatus").encode()
                .buildAndExpand(appName, DATE_FORMAT.format(LocalDateTime.now())).toUriString(), lastStatusFailure);
    }

    /** 使用查询参数模板严格编码中文、空格和具有特殊含义的字符。 */
    private UriComponentsBuilder reportUri(String endpoint) {
        return UriComponentsBuilder.newInstance().scheme(baseUri.getScheme()).host(baseUri.getHost())
                .port(baseUri.getPort()).path(baseUri.getPath()).pathSegment(endpoint)
                .queryParam("appName", "{appName}").queryParam("saveDate", "{saveDate}");
    }

    /** 分别识别网络、响应格式和应用商店业务失败，错误日志不带请求正文。 */
    private void send(String reportName, Supplier<String> url, AtomicLong lastFailure) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            ResponseEntity<byte[]> response = httpRequestTool.request(
                    url.get(), HttpMethod.POST, headers, null, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                warnFailure(reportName, "响应状态码为" + response.getStatusCodeValue(), lastFailure);
                return;
            }

            // 接口约定以响应中的整数业务状态码判断结果。
            byte[] body = response.getBody();
            JsonNode payload = body == null ? null : objectMapper.readTree(body);
            JsonNode code = payload == null ? null : payload.get("code");
            if (code == null || !code.isIntegralNumber() || !code.canConvertToInt()) {
                warnFailure(reportName, "响应缺少有效的业务状态码", lastFailure);
            } else if (code.intValue() != 200) {
                warnFailure(reportName, "业务状态码为" + code.intValue(), lastFailure);
            }
        } catch (IOException exception) {
            warnFailure(reportName, "响应无法解析为有效的结果数据", lastFailure);
        } catch (RuntimeException exception) {
            // 公共工具的异常包含查询参数，仅提取可公开的响应状态码。
            Throwable cause = exception.getCause();
            String reason = cause instanceof HttpStatusCodeException
                    ? "响应状态码为" + ((HttpStatusCodeException) cause).getRawStatusCode()
                    : "请求未完成，请检查地址、网络和超时配置";
            warnFailure(reportName, reason, lastFailure);
        }
    }

    /** 校验配置并统一接口前缀两端的斜线。 */
    private static URI buildBaseUri(AppStoreReportProperties properties, String appName) {
        String scheme = StringUtils.trimWhitespace(properties.getScheme());
        scheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        int port = properties.getPort();
        if (!StringUtils.hasText(appName) || !StringUtils.hasText(properties.getHost())) {
            throw new IllegalArgumentException("应用商店主机和应用登记名称必须填写");
        }
        if ((!scheme.equals("http") && !scheme.equals("https")) || port < 1 || port > 65_535) {
            throw new IllegalArgumentException("应用商店协议或端口配置无效");
        }
        if (properties.getConnectTimeoutMillis() <= 0 || properties.getReadTimeoutMillis() <= 0
                || (properties.isStatusEnabled() && properties.getStatusIntervalMillis() <= 0)) {
            throw new IllegalArgumentException("上报间隔和超时必须为正数");
        }

        // 主机只配置地址，部署路径统一从接口前缀读取。
        String prefix = StringUtils.trimWhitespace(properties.getPathPrefix());
        prefix = prefix == null ? "" : prefix.replaceAll("^/+|/+$", "");
        URI uri = UriComponentsBuilder.newInstance().scheme(scheme).host(properties.getHost().trim()).port(port)
                .path(prefix.isEmpty() ? "" : "/" + prefix).build().toUri();
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("应用商店主机格式无效");
        }
        return uri;
    }

    /** 持续失败时每分钟最多记录一次同类提示。 */
    private void warnFailure(String reportName, String reason, AtomicLong lastFailure) {
        if (allowWarning(lastFailure)) {
            LOGGER.warn("应用商店{}上报失败，原因：{}", reportName, reason);
        }
    }

    private static boolean allowWarning(AtomicLong lastWarning) {
        long now = System.nanoTime();
        long previous = lastWarning.get();
        return (previous == 0L || now - previous >= WARNING_INTERVAL_NANOS)
                && lastWarning.compareAndSet(previous, now);
    }

    /** 停止接收新任务并释放上报线程，未发送的日志仍可从本地查询。 */
    @Override
    public void destroy() {
        statusExecutor.shutdownNow();
        logExecutor.shutdownNow();
    }
}
