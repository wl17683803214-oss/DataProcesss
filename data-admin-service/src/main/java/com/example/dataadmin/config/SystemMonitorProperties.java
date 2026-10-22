package com.example.dataadmin.config;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 健康采样和消息上报配置，修改后重启管理服务生效。 */
@Component("systemMonitorProperties")
@ConfigurationProperties(prefix = "system-monitor")
@Validated
public class SystemMonitorProperties {
    /** 健康采样及缓存更新周期，单位毫秒。 */
    @Min(1)
    private long sampleIntervalMillis = 1000;
    /** 快照批量入库周期，单位毫秒。 */
    @Min(1)
    private long snapshotFlushIntervalMillis = 30000;
    /** 连续超限达到该时长才触发告警，单位毫秒。 */
    @Min(1)
    private long alarmDurationMillis = 15000;
    /** 独立的消息上报设置。 */
    @Valid
    private final MqReport mqReport = new MqReport();

    /** 返回采样周期。 */
    public long getSampleIntervalMillis() { return sampleIntervalMillis; }
    /** 保存采样周期。 */
    public void setSampleIntervalMillis(long value) { sampleIntervalMillis = value; }
    /** 返回入库周期。 */
    public long getSnapshotFlushIntervalMillis() { return snapshotFlushIntervalMillis; }
    /** 保存入库周期。 */
    public void setSnapshotFlushIntervalMillis(long value) { snapshotFlushIntervalMillis = value; }
    /** 返回持续超限时长。 */
    public long getAlarmDurationMillis() { return alarmDurationMillis; }
    /** 保存持续超限时长。 */
    public void setAlarmDurationMillis(long value) { alarmDurationMillis = value; }
    /** 返回消息上报设置。 */
    public MqReport getMqReport() { return mqReport; }

    /** 消息发送周期与采样周期分开设置。 */
    public static class MqReport {
        /** 是否启用上报。 */
        private boolean enabled;
        /** 上报间隔，单位毫秒。 */
        @Min(1)
        private long intervalMillis = 1000;
        /** 快照有效年龄，单位毫秒。 */
        @Min(1)
        private long maxSnapshotAgeMillis = 5000;
        /** 目标主题。 */
        private String topic = "topic_yc_out_sjcl";
        /** 目标标签，允许为空。 */
        private String tag = "";
        /** 接收方约定的业务枚举名称，未约定时不发送。 */
        private String business = "";
        /** 接收方约定的参数表业务标识，未约定时不发送。 */
        private String businessId = "";

        /** 返回上报开关。 */
        public boolean isEnabled() { return enabled; }
        /** 保存上报开关。 */
        public void setEnabled(boolean value) { enabled = value; }
        /** 返回上报间隔。 */
        public long getIntervalMillis() { return intervalMillis; }
        /** 保存上报间隔。 */
        public void setIntervalMillis(long value) { intervalMillis = value; }
        /** 返回快照有效年龄。 */
        public long getMaxSnapshotAgeMillis() { return maxSnapshotAgeMillis; }
        /** 保存快照有效年龄。 */
        public void setMaxSnapshotAgeMillis(long value) { maxSnapshotAgeMillis = value; }
        /** 返回目标主题。 */
        public String getTopic() { return topic; }
        /** 保存目标主题。 */
        public void setTopic(String value) { topic = value; }
        /** 返回消息标签。 */
        public String getTag() { return tag; }
        /** 保存消息标签。 */
        public void setTag(String value) { tag = value; }
        /** 返回业务枚举名称。 */
        public String getBusiness() { return business; }
        /** 保存业务枚举名称。 */
        public void setBusiness(String value) { business = value; }
        /** 返回参数表业务标识。 */
        public String getBusinessId() { return businessId; }
        /** 保存参数表业务标识。 */
        public void setBusinessId(String value) { businessId = value; }
    }
}
