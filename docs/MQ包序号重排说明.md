# MQ包序号重排说明

PDXP设备、卫星及指令响应处理完成后，在 `TelemetryResultDispatcher` 中提交消息到 `InterfaceMessageOrderBuffer`。采集、解析、校准、入库及实时缓存沿用现有处理路径，MQ消息内容保持原约定。

## 连续排序规则

只按采集接口主键分组。各接口独立维护入口队列和当前待发消息的序号、到期索引。同一接口只由自己的周期任务操作排序索引并同步发送，不同接口共用固定数量的调度线程。

每条消息使用单调时钟记录入队时间。只要当前队列有包等待满配置时间，就发送队列中的最小序号；每发一条都根据剩余待发消息重新检查。这样，到期包及其之前的较小序号可以一起按序发出，较小序号不必单独等满。所有到期包发完后，剩余消息继续等待，不沿用之前的发送边界，也不按固定的一秒批次清空。

例如等待时间为1000毫秒：

| 时间 | 事件 | 结果 |
| --- | --- | --- |
| 0毫秒 | 收到103 | 开始记录103的等待时间 |
| 200毫秒 | 收到105 | 保留105自己的入队时间 |
| 800毫秒 | 收到101 | 排在103之前 |
| 1000毫秒 | 103到期 | 发送101、103，保留105 |
| 1050毫秒 | 收到104 | 排在105之前 |
| 1200毫秒 | 105到期 | 发送104、105 |

实际发送时刻还受扫描间隔、线程调度和MQ响应时间影响。时间从处理结果进入发送缓冲开始计算，不从网络收包开始计算。排序只覆盖当前处理实例内同一接口的待发消息；已经发出的消息不参与后续排序。

## 实现及配置

多个处理线程通过标准库 `ConcurrentLinkedQueue` 投递消息，条数及字节额度通过原子比较更新预占。每个接口注册一个 `ScheduledThreadPoolExecutor.scheduleWithFixedDelay` 周期任务，前后执行不会重叠；该任务独占序号树和到期树，两种索引的插入和删除均为对数复杂度。业务代码不使用显式互斥锁，也不持锁等待或发送消息。

配置位于 `nacos-config/data-process-service.yaml` 的 `data-processing.result` 下，修改后重启处理服务生效：

| 配置 | 默认值 | 含义 |
| --- | --- | --- |
| message-thread-count | 4 | 共用的调度和发送线程数，部署文件沿用已有128线程 |
| message-queue-capacity | 2048 | 所有接口待发送消息总条数，包含投递中和重试中 |
| message-order.send-enabled | true | 是否实际发送遥测和指令响应MQ消息；关闭后仍入队、等待和排序，到期移除并释放额度，不补发 |
| message-order.wait-millis | 1000 | 到期包推动发送边界的等待毫秒数 |
| message-order.scan-interval-millis | 10 | 周期任务的检查间隔毫秒数 |
| message-order.max-pending-per-interface | 2048 | 单个接口的待发送条数上限 |
| message-order.max-pending-bytes | 268435456 | 部署配置设为1073741824（1 GiB）；所有接口共享，按消息序列化长度加记录开销估算 |
| message-order.retry-interval-millis | 1000 | 发送失败后的重试间隔毫秒数 |
| message-order.max-sends-per-run | 256 | 单接口每次调度的发送条数上限 |
| shutdown-wait-seconds | 30 | 消息发送与入库共用的正常关闭等待上限 |

RocketMQ使用现有客户端的 `syncSendOrderly`，以接口主键作为分组键，原始消息正文不添加序号或接口字段。消费端仍需配置顺序消费；主题队列拓扑变化和多个生产实例的并发行为不能只靠本地缓冲保证。

同一配置文件中的 `greptime.enabled` 独立控制遥测入库。关闭后不提交正常或异常整帧及遥测参数的入库任务，实时缓存、推送及MQ重排不受影响。`message-order.send-enabled` 关闭时不调用MQ客户端，也不会影响入库、实时缓存或推送；两个开关都在重启处理服务后生效。

条数和字节容量需覆盖配置等待时间内所有接口的峰值消息量，并为发送变慢及重试保留余量。单接口串行发送的吞吐量还取决于消息代理的响应时间。

## 异常及边界

- 同一接口缓冲中重复的包序号保留先登记的消息，重复项丢弃并记录现有消息失败告警。
- 发送成功后不保留包号。小序号后来到达，或同一包号在此前消息已发送后再次到达，均正常入队，按当前队列的到期规则发送。
- 正常三十二位无符号序号回绕按当前待发队列展开，待发序号跨度应小于半个序号空间；队列为空时以新包序号重新开始。
- 发送异常或未获得成功确认时保留消息，暂停该接口后续发送并按配置重试。若代理已经收到消息但确认丢失，重试可能产生重复消息，消费端仍需具备幂等能力。
- 超出接口条数、全局条数或全局字节额度时拒绝当前消息并记录中文告警；不会通过提前发送大序号来腾出空间。
- 正常关闭拒绝新消息，并在共用关闭期限内跳过剩余等待时间、按序刷新已有消息。超过期限记录未发送量；异常退出时内存消息不保证恢复。
- 处理成功统计表示消息缓冲和已启用的入库任务接受了当前帧，不代表外部入库或MQ发送最终成功。

## 实现参考

比较了采用Apache-2.0许可证、持续维护的 [Netty串行执行器](https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/concurrent/SingleThreadEventExecutor.java) 和 [JCTools多生产者队列](https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/MpscLinkedQueue.java)。前者使用单个任务执行者管理状态，后者提供专用的多生产者单消费者队列，但特定实现依赖底层内存操作并存在生产线程暂停时的可见性等待。本次采用串行拥有排序状态的设计，并使用JDK标准队列和定时器，不新增依赖，不复制项目源码。

[JDK调度器说明](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledThreadPoolExecutor.html)保证同一周期任务不会重叠执行，前一次执行的结果对下一次可见。[现有RocketMQ客户端源码](https://github.com/apache/rocketmq-spring/blob/rocketmq-spring-all-2.2.3/rocketmq-spring-boot/src/main/java/org/apache/rocketmq/spring/core/RocketMQTemplate.java)提供按分组键选择队列的顺序发送接口。

## 本次验证

2026年11月17日完成编译和本地运行验证：真实一秒跨秒排序结果为101、103、104、105；验证了较小序号提前发送、不同接口独立调度、同一接口不并发、失败保留重试、迟到去重、无符号回绕、条数与字节容量限制、关闭刷新，以及8个线程并发投递2000条消息后全部按序发送。使用已有依赖模拟消息代理验证了接口分组和消息字节，并解析了真实部署配置及Apifox导入文件。

移除历史包号后，重新验证了晚到的小序号和已发过的包号可以正常入队、不会沿用已发包的到期资格，同包号仅在队列内去重。公共模块3项测试和处理服务35项测试通过；`PdxpParserTest.shouldParseGeneratedSampleFile` 因缺少 `docs/pdxp-generated-samples.dat` 未运行。未新增或修改项目单元测试，也未连接真实消息代理进行端到端联调。
