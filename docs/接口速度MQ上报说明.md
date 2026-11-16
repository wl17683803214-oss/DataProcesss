# 接口速度MQ上报说明

管理服务通过 `scheduler/InterfaceSpeedReportScheduler.java` 一个定时类实现此功能。独立单线程按一秒节拍读取现有采集速度缓存并向指定Topic发送一条全量JSON消息，不新增HTTP接口，也不重新统计字节数。MQ连接、生产者组和认证沿用管理服务的 `rocketmq` 配置。

## 配置

在管理服务实际读取的Nacos配置中增加以下内容，已有连接参数不需要替换。兼容未添加配置的环境，代码默认关闭上报。仓库示例开启，修改后重启管理服务生效；脚本不会代为发布Nacos配置。

```yaml
# 接口采集速度JSON上报配置。
interface-speed-report:
  # 每秒发送一条缓存全量快照。
  enabled: true
  # 独立的速度主题，只填写主题名。
  topic: topic_interface_speed_out_sjcl
  # 消息中的系统标识。
  system-id: sjcl
```

若管理服务目前只有 `rocketmq.name-server`、没有 `rocketmq.producer`，还需要在原有 `rocketmq` 节点下补充生产者配置；已经配置生产者的环境直接复用原值，不覆盖已有组名、地址或认证信息，不重复新增另一个 `rocketmq` 根节点。

```yaml
rocketmq:
  # 沿用原有名称服务地址；此处为本机联调示例。
  name-server: 127.0.0.1:9876
  producer:
    # 仅当原配置没有生产者时补充，已有生产者组继续复用。
    group: data-admin-interface-speed-report
    # 消息发送超时毫秒数；已有配置继续保留。
    send-message-timeout: 5000
```

## 消息

消息体为UTF-8 JSON文本，不添加包装字段、标签或额外消息头。

```json
{
  "systemId": "sjcl",
  "sampleTime": "2026-11-13T10:30:01+08:00",
  "total": {
    "interfaceCount": 2,
    "receiveRate": "5.73 MB/s"
  },
  "interfaces": [
    {
      "taskId": "TASK001",
      "interfaceName": "遥测采集接口一",
      "receiveRate": "3.82 MB/s"
    },
    {
      "taskId": "TASK001",
      "interfaceName": "遥测采集接口二",
      "receiveRate": "1.91 MB/s"
    }
  ]
}
```

`sampleTime` 是本次读取缓存的开始时间，包含时区偏移。`interfaces` 包含全部未删除且归属任务的采集接口，既包含启用接口，也包含停用接口；未归属任务的配置不参与。接口名称和任务归属通过已有Mapper查询，每三十秒刷新一次内存快照；刷新失败时已有快照继续使用，首次查询失败则跳过本轮上报。

每个任务每轮只读取一次 `dashboard:collection:current:任务编号`，从 `interface:接口编号` 字段读取已有 `interfaceRateKbps` 数值及 `sampleTime`。速度缺失、格式不合法、非有限数值或缓存字段超过五秒时按零处理；各服务应使用相同系统时区。Redis读取整体失败时跳过本轮，不发送伪造的全零快照，也不删除原缓存。

单位与页面复用 `CollectionRateUnit`，小于1024 KB/s显示KB/s，达到后显示MB/s；总速率先累加原始数值再格式化，避免不同单位直接相加或提前修约。此处表示采集字节速度，不包含参数处理帧速率、MQ消费速度或Greptime入库速度。消息不包含 `interfaceId`、`processedFramesPerSecond`、`sampleDurationMillis`。

MQ慢请求或失败只影响本独立线程；后续周期读取最新缓存，同一秒最多发送一条，不补发过时快照。首次成功输出中文定位日志，连续失败最多每三十秒记录一次。若没有符合条件的接口，正常发送空数组、接口数0及总速度0 KB/s。容器关闭时取消后续任务并等待线程结束，共用消息模板继续由原组件管理。

Apifox导入文件已增加 `InterfaceSpeedMqMessage`、`InterfaceSpeedMqTotal`、`InterfaceSpeedMqInterface` 消息模型及样例，供消费端对接。
