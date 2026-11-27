package com.example.dataprocess.service.impl;

import com.example.common.alarm.DataAlarmRecord;
import com.example.common.appstore.AppStoreLogLevel;
import com.example.common.appstore.AppStoreReporter;
import com.example.common.enums.DataAlarmType;
import com.example.dataprocess.mapper.DataAlarmRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/** 告警批次使用独立事务，失败后整批保留重试。 */
@Service
public class DataAlarmPersistenceWriter {
    /** 只选择影响采集和处理链路的告警类型。 */
    private static final Set<String> REPORT_TYPES = new HashSet<String>(Arrays.asList(
            DataAlarmType.INTERFACE_START.name(), DataAlarmType.INTERFACE_STOP.name(),
            DataAlarmType.COLLECTION_RECEIVE.name(), DataAlarmType.PACKET_PARSE.name(),
            DataAlarmType.FILE_RECEIVE.name(), DataAlarmType.PROCESS_QUEUE.name(),
            DataAlarmType.PROCESS_CONFIG.name(), DataAlarmType.RPC_FAILURE.name(),
            DataAlarmType.STORAGE_FAILURE.name(), DataAlarmType.MESSAGE_FAILURE.name(),
            DataAlarmType.FILE_RESULT.name(), DataAlarmType.CALIBRATION_PERSIST.name()));

    /** 现有数据告警中严重级别的编码。 */
    private static final int SERIOUS_ALARM_LEVEL = 4;
    private final DataAlarmRecordMapper mapper;
    private final AppStoreReporter appStoreReporter;

    public DataAlarmPersistenceWriter(DataAlarmRecordMapper mapper, AppStoreReporter appStoreReporter) {
        this.mapper = mapper;
        this.appStoreReporter = appStoreReporter;
    }

    @Transactional(rollbackFor = Exception.class)
    public void write(List<DataAlarmRecord> records) {
        for (DataAlarmRecord record : records) {
            // 同一合并键的查询和写入需要处于同一事务锁内。
            mapper.lockKey(record.getDedupKey());
            if (record.getTriggerCount() == 0) {
                mapper.recover(record);
                continue;
            }
            // 仅合并一分钟内仍活动且未人工处理的同对象同参数告警。
            DataAlarmRecord existing = mapper.findRecent(record.getDedupKey(),
                    record.getAlarmTime().minusSeconds(60));
            if (existing == null) {
                mapper.insert(record);
                // 同一条告警后续只累计次数，首次落库成功时才上报。
                reportNewAlarm(record);
            } else {
                record.setId(existing.getId());
                mapper.merge(record);
            }
        }
    }

    /** 发送可定位故障对象的中文摘要，完整原因、报文及网络信息留在本地。 */
    private void reportNewAlarm(DataAlarmRecord record) {
        if (!REPORT_TYPES.contains(record.getEventType())) {
            return;
        }

        // 一般告警上报警告，严重告警上报错误。
        AppStoreLogLevel level = Integer.valueOf(SERIOUS_ALARM_LEVEL).equals(record.getAlarmLevel())
                ? AppStoreLogLevel.ERROR : AppStoreLogLevel.WARN;
        String summary = DataAlarmType.labelOf(record.getEventType()) + "，接口编号：" + record.getInterfaceId();
        appStoreReporter.submitLogAfterCommit(level, summary, record.getAlarmTime());
    }
}
