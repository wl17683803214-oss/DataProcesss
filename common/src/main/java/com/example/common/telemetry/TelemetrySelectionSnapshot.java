package com.example.common.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** 实时缓存使用所有页面及组件勾选的并集，按来源对象合并，不再使用任务维度。 */
public class TelemetrySelectionSnapshot {
    private Map<String, Set<String>> objects = Collections.emptyMap();

    /** 生成共享缓存身份，各部分独立编码避免分隔符冲突。 */
    @Deprecated
    public static String objectKey(String ignoredTaskId, String sourceType, String identity) {
        // 保留旧调用入口，任务不再参与筛选快照的身份。
        return objectKey(sourceType, identity);
    }

    /** 参数勾选仅按来源和对象身份合并。 */
    public static String objectKey(String sourceType, String identity) {
        return encode(sourceType) + "." + encode(identity);
    }

    /** 统一构造筛选快照的 Redis 键。 */
    public static String redisKey(TelemetryRealtimeProperties properties) {
        return properties.getKeyPrefix() + "selection:union:v2";
    }

    /** 对业务身份使用无填充编码。 */
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 返回当前完整对象筛选集合，反序列化后由使用方转为不可变快照。 */
    public Map<String, Set<String>> getObjects() {
        return objects;
    }

    /** 保存数据库勾选并集的完整版本。 */
    public void setObjects(Map<String, Set<String>> objects) {
        this.objects = objects;
    }
}
