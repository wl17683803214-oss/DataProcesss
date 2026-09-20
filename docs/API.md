# 数据处理软件接口文档

本文档依据当前 `data-admin-service` 和 `data-process-service` 源码整理。后台管理服务默认地址为 `http://localhost:8082`，数据处理服务默认地址为 `http://localhost:8083`。

每个接口可直接使用的入参与出参示例见同目录的 `API-EXAMPLES.md`。

## 1. 通用约定

- 请求与响应编码：`UTF-8`
- POST 请求类型：`application/json`
- 除登录接口和子系统菜单配置接口外，所有接口都需要请求头：`Authorization: Bearer <token>`
- 时间格式：`yyyy-MM-dd'T'HH:mm:ss`，例如 `2026-07-30T15:20:00`
- 日期格式：`yyyy-MM-dd`
- 分页参数 `pageNum` 从 1 开始。

统一响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

分页数据位于 `data` 中：

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "total": 100,
  "records": []
}
```

子系统菜单配置接口按照嵌套集成约定直接返回配置对象，不使用统一响应结构。

## 2. 子系统集成

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/integration/menu-config` | 按基线角色查询子系统菜单配置（无需 Token） | 查询：`roleIds` 必填，多个编号使用英文逗号分隔 |

接口使用 `roleIds` 匹配 `dp_role.baseline_role_id`，再通过 `dp_role_menu` 查询角色关联的未删除菜单，自动补齐父菜单并按照 `parent_id` 和 `sort_order` 生成菜单树。`systemKey`、`label`、`icon`、`baseUrl`、`defaultPath` 读取 `subsystem.integration` 配置，`origin` 根据 `baseUrl` 自动生成。

## 3. 登录认证

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 用户登录（无需 Token） | JSON：`username`、`password` |
| POST | `/auth/sso/login` | 使用基线平台 Token 登录（无需本系统 Token） | JSON：`tokenId` 必填，`clientId`、`remoteIp` 可选 |
| POST | `/auth/logout` | 退出当前登录 | 无 |
| GET | `/auth/user-info` | 当前用户和角色 | 无 |
| GET | `/auth/routes` | 根据角色菜单关系查询左侧菜单树 | 无 |

登录示例：

```http
POST http://localhost:8082/auth/login
Content-Type: application/json

{"username":"admin","password":"admin123"}
```

基线 SSO 登录时，前端把 iframe 消息或统一登录回调中的平台 Token 提交给 `/auth/sso/login`。后台依次校验平台 Token、查询平台用户和角色、同步本地账号关系，随后返回本系统 JWT。角色只按用户返回的 `roleIds` 与角色目录匹配，不再使用 `systemPermissions` 筛选。普通平台用户首次登录时自动创建同名本地用户并生成八位随机密码；用户名为 `admin` 时固定复用本地管理员和超级管理员角色。

平台用户和角色同步后仍由本地角色配置菜单范围。平台角色没有配置本地菜单时可以完成登录，但 `/auth/routes` 返回空数组；超级管理员角色通过角色菜单关系绑定全部菜单。

### 3.1 总览

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/dashboard/collection` | 查询采集实时汇总、折线图和表格 | 查询：`taskId?`、`range=3h/24h/7d` |
| GET | `/dashboard/processing` | 查询处理汇总、折线图和表格 | 查询：`taskId?`、`range=3h/24h/7d` |

前端可按固定周期并行轮询两个查询接口。接口速率从 Redis 秒级指标读取，历史采集量和处理量从金仓五秒统计表读取；查询时再按时间范围分桶，并按 `collect_interface_config.send_from` 聚合为发送方系统数据。

## 4. 用户管理

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/system/users` | 用户列表 | 无 |
| GET | `/system/users/{id}` | 用户详情 | 路径：`id` |
| POST | `/system/users/create` | 新增用户 | `UserSaveRequest` |
| POST | `/system/users/update` | 修改用户 | `UserSaveRequest`，`id` 必填 |
| POST | `/system/users/delete` | 删除用户 | JSON：`id` |
| POST | `/system/users/status` | 启用或禁用用户 | JSON：`id`、`status`（0 禁用/1 启用） |
| POST | `/system/users/reset-password` | 重置密码 | JSON：`id`、`password` |

`UserSaveRequest`：`id`、`username`、`password`、`nickname`、`phone`、`email`、`status`（0 停用/1 启用）、`roleId`。

用户列表和详情返回 `phone`、`email`、`baselineUserId`、`roleId`、`roleName`、`roleKey`，不会返回密码。新增或修改用户传入 `roleId` 时，服务会校验角色存在且启用；删除用户会同步清理用户角色关系。用户名为 `admin` 的本地管理员账号不能删除、停用或解除超级管理员关系。

### 4.1 角色与菜单权限管理

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/system/roles` | 角色列表 | 无 |
| GET | `/system/roles/{id}` | 角色详情 | 路径：`id` |
| POST | `/system/roles/create` | 新增角色 | `roleName`、`roleKey`、`status` |
| POST | `/system/roles/update` | 修改角色 | `id`、`roleName`、`roleKey`、`status` |
| POST | `/system/roles/delete` | 删除角色 | JSON：`id` |
| POST | `/system/roles/status` | 启用或停用角色 | JSON：`id`、`status`（0/1） |
| GET | `/system/menus` | 查询可分配的菜单 | 无 |
| GET | `/system/roles/{id}/menus` | 查询角色已分配的菜单 ID | 路径：`id` |
| POST | `/system/roles/menus` | 重新分配角色可访问菜单 | JSON：`roleId`、`menuIds` |

菜单权限采用 `dp_user → dp_user_role → dp_role → dp_role_menu → dp_menu`。后端业务接口只验证登录状态，不校验按钮或接口权限。超级管理员角色不能删除、停用或修改 `admin` 标识；已经分配给用户的角色不能删除。

### 4.2 菜单来源与角色绑定

当前左侧菜单统一预置在 `sql/system.sql` 中，执行脚本后写入 `dp_menu` 表；系统暂未提供菜单新增、修改、删除接口。`GET /system/menus` 仅用于读取可分配的菜单节点。分组菜单通过 `parent_id` 组织子菜单，叶子菜单通过 `path` 指向页面，`hidden` 控制是否在左侧显示。

页面在初始化脚本中的层级为：

```text
总览（ID 10，C）
数据可视化（ID 30，C）
数据采集（ID 20，C）
卫星管理（ID 35，C）
数据处理（ID 40，C）
数据校准（ID 50，C）
告警管理（ID 60，C）
系统配置（ID 1，M）
├── 用户管理（ID 2，C）
├── 角色管理（ID 7，C）
└── 日志管理（ID 11，C）
```

给角色授权时，前端先调用 `GET /system/menus` 获取全部节点，再调用 `GET /system/roles/{id}/menus` 获取已勾选 ID，最后提交：

```http
POST /system/roles/menus
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "roleId": 2,
  "menuIds": [10, 20, 40, 50]
}
```

保存时会先删除该角色在 `dp_role_menu` 中的原关联，再写入本次提交的页面 ID；选择子页面时会自动补齐父目录。超级管理员 `admin` 始终拥有全部有效页面，因此不允许通过此接口重新分配页面。

## 5. 数据采集

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/collection/overview` | 采集总览 | 查询：`taskId`（必填） |
| GET | `/collection/interfaces` | 分页查询采集接口 | 查询：`taskId?`、`interfaceType?`、`transferType?`、`status?`、`enabled?`、`pageNum=1`、`pageSize=10` |
| GET | `/collection/interfaces/{id}` | 采集接口详情 | 路径：`id`；返回 `protocolConfigName` 用于展示，未关联或协议已删除时为 `null`，禁用协议仍返回名称 |
| POST | `/collection/interfaces/create` | 新增采集接口 | `InterfaceSaveRequest` |
| POST | `/collection/interfaces/update` | 修改采集接口 | `InterfaceSaveRequest`，`id` 必填 |
| POST | `/collection/interfaces/delete` | 删除采集接口 | JSON：`id` |
| POST | `/collection/interfaces/enabled` | 启用/禁用采集接口 | JSON：`id`、`enabled`（0/1） |
| GET | `/collection/protocols` | 协议配置列表 | 查询：`taskId?` |
| GET | `/collection/protocols/{id}` | 协议配置详情 | 路径：`id` |
| POST | `/collection/protocols/create` | 新增协议配置 | `ProtocolSaveRequest` |
| POST | `/collection/protocols/update` | 修改协议配置 | `ProtocolSaveRequest`，`id` 必填 |
| POST | `/collection/protocols/delete` | 删除协议配置 | JSON：`id` |
| POST | `/collection/protocols/enabled` | 启用/禁用协议配置 | JSON：`id`、`enabled`（0/1） |
| GET | `/collection/events/overview` | 采集事件统计 | 查询：`taskId?` |
| GET | `/collection/events` | 最近采集事件 | 查询：`taskId?` |

采集事件列表中的 `logLevel` 返回稳定枚举编码，`logLevelName` 返回对应中文名称（调试、信息、警告、错误）。

`InterfaceSaveRequest`：`id`、`taskId`、`interfaceName`、`interfaceType`、`sendFrom`、`messageContent`、`transferType`、`transferProtocol`、`protocolConfigId`、`host`、`port`、`status`、`enabled`、`rpcEnabled`、`remark`。其中 `transferProtocol` 为整数枚举：1 JSON、2 PDXP、3 Protobuf、4 FEP；`port` 范围为 1～65535；`rpcEnabled` 为 0 时使用本地处理，为 1 时进入 RPC 处理流程。两种处理方式都必须传 `protocolConfigId`，采集接口不再接收或返回 `sheetName`。

`ProtocolSaveRequest`：`id`、`taskId`、`configName`、`protocolType`（1 JSON/2 PDXP/3 Protobuf/4 RPC）、`dataSourceType`（1 遥测/2 遥感/3 设备/4 环境）、`configDesc`、`configParams`、`parserClass`、`enabled`。

`protocolType=2`时，PDXP字段配置保存在`configParams.fields`中：

```json
{
  "pdxphead": {
    "ver": null,
    "mid": null,
    "sid": null,
    "did": null,
    "bid": null,
    "no": null,
    "flag": null,
    "res": null
  },
  "fields": [
    {
      "id": 81,
      "tableIndex": "1",
      "bitWidth": "32",
      "telemetryName": "温度",
      "telemetryCode": "TMK1001",
      "formulaType": "109",
      "formulaDesc": "",
      "processParam": "",
      "decimalPlaces": "5",
      "alarmFlag": "1",
      "normalValue": "正常:[-1,30]",
      "warningValue": "",
      "stateChangeInfo": "",
      "commandCode": "",
      "systemName": "发控台",
      "controlChannel": "",
      "mergeChannelCount": "0",
      "delayChannel": "0",
      "storeFlag": "1",
      "calibrationFormula": ""
    }
  ]
}
```

FEP文件在本地接收完整后先返回结束确认包，再上传对象存储并发送文件完成消息。临时目录、数据单元长度、对象存储连接和消息主题统一通过配置中心管理。

文件完成消息示例：

```json
{
  "requirementId": "",
  "requirementName": "",
  "requirementDesc": "",
  "version": "",
  "fileUrl": "http://192.168.2.4:9002/ekbs/2026/09/02/b14cfe8d4d9d4f668f8f5bcaafb0aa4b.docx",
  "baseInfo": {
    "businessName": "",
    "senderId": "",
    "senderName": "",
    "receiverId": "",
    "receiverName": "",
    "time": "2026-09-02 16:35:28.123"
  }
}
```

PDXP保留`pdxphead`包头配置；`fields`中的每项与参数解析配置的十九个字符串业务字段保持一致，不再使用`protocolTableName`、`tmSymbol`、`fieldName`、`offset`、`length`、`dataType`和`byteOrder`。

`protocolType=4`时，RPC请求的协议配置字段保存在`configParams.fields`中：

```json
{
  "fields": [
    {
      "field": "sat_code",
      "fieldName": "卫星代号",
      "dataType": "string",
      "value": "SAT-001"
    },
    {
      "field": "channel",
      "fieldName": "通道代号",
      "dataType": "string",
      "value": "CH-01"
    },
    {
      "field": "data_source",
      "fieldName": "数据源类型",
      "dataType": "string",
      "value": "telemetry"
    }
  ]
}
```

RPC字段属性：`field`为`FrameRequest`字段名，`fieldName`为中文名称，`dataType`为数据类型，`value`为配置值。`data`、`sequence`、`send_time`和`length`由每帧PDXP数据动态组装，不放入`configParams`。

## 6. 数据可视化

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/visualization/widgets` | 当前用户组件列表 | 查询：`taskId`（必填） |
| GET | `/visualization/widgets/{id}` | 组件详情 | 路径：`id` |
| POST | `/visualization/widgets/create` | 新增组件 | `WidgetSaveRequest` |
| POST | `/visualization/widgets/update` | 修改组件 | `WidgetSaveRequest`，`id` 必填 |
| POST | `/visualization/widgets/delete` | 删除组件及数据项 | JSON：`id` |
| GET | `/visualization/widgets/{widgetId}/items` | 组件参数筛选项列表（含勾选状态） | 路径：`widgetId` |
| GET | `/visualization/items/{id}` | 数据项详情 | 路径：`id` |
| POST | `/visualization/items/create` | 新增数据项 | `WidgetItemSaveRequest` |
| POST | `/visualization/items/update` | 修改数据项 | `WidgetItemSaveRequest`，`id` 必填 |
| POST | `/visualization/items/delete` | 删除数据项 | JSON：`id` |
| POST | `/visualization/items/selected` | 修改勾选状态 | JSON：`id`、`status`（0/1） |

`WidgetSaveRequest`：`id`、`taskId`、`widgetKey`、`widgetType`（1 实时曲线/2 实时数据/3 实时告警/4 载荷图像）、`widgetTitle`、`gridX`、`gridY`、`gridWidth`、`gridHeight`、`enabled`。

`WidgetItemSaveRequest`：`id`、`widgetId`、`parseRuleId`、`interfaceId`、`protocolConfigId`、`itemCode`、`itemName`、`isSelected`。`parseRuleId` 对应参数解析配置主键；自动同步产生的筛选项不绑定单一采集接口或协议，`interfaceId`、`protocolConfigId` 可以为空。

## 7. 试验任务

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/experiment/tasks` | 查询调度系统下发的全部试验任务生命周期快照，使用字符串 `taskId` 作为主键 | 无 |

## 8. 数据处理

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/processing/overview` | 今日处理量和累计去重总览 | 查询：`taskId`（必填） |
| GET | `/processing/realtime` | 实时处理速率、CPU和内存监控 | 查询：`taskId`（必填） |
| GET | `/processing/rule-config` | 查询处理规则开关 | 查询：`taskId`（必填） |
| POST | `/processing/rule-config/update` | 保存处理规则开关 | `ProcessingRuleConfigRequest` |
| GET | `/processing/logs` | 分页查询处理日志 | 查询：`taskId?`、`processTaskId?`、`logLevel?`、`pageNum=1`、`pageSize=20` |
| GET | `/processing/device-satellites` | 设备卫星筛选列表 | `taskId` 必填，`type` 可选；类型为空时查询全部；返回 `id`、`code`、`name`、`type` |
| GET | `/processing/rules` | 遥测解析规则列表 | 必填：`taskId`、`deviceSatelliteId` |
| POST | `/processing/rules/import` | Excel或TXT按类型替换导入 | `multipart/form-data`：`taskId`、`type`、`file` |

处理日志中的 `logLevel` 返回稳定枚举编码（1 信息、2 警告、3 错误），`logLevelName` 返回对应中文名称。

`ProcessingRuleConfigRequest` 只包含 `taskId` 和 `parameterRangeCheckEnabled`（0关闭、1开启）。查询时如果该任务尚无配置，系统会先插入参数值范围检查关闭的默认配置，再返回数据库记录。数据库表保留其他规则字段，但查询和保存接口不再读取或修改这些字段。

上传.xls、.xlsx或.txt文件（最大20MB），前端选择type，整份文件使用相同类型。Excel按Sheet页签分别创建设备卫星；TXT使用制表符分隔，一次表示一个设备卫星，使用去掉扩展名的文件名填写code和name，并自动兼容UTF-8和GBK编码。第一行必须包含全部19个中文表头：序号、位宽、遥测名称、遥测代号、公式类型、处理公式、处理参数、小数位数、是否报警、正常值范围、预警值范围、状态跳变信息、相关命令、所属系统、控制波道、合并波道、延时波道、存储遥测、校准公式；旧TXT最后一列表头和内容为反斜线时按空校准公式处理。每个设备至少有一条有效数据，空行忽略，序号不能重复。完整校验后在同一事务中逻辑删除当前taskId下同类型设备卫星及关联参数，回填设备卫星主键到参数的deviceSatelliteId。其他任务和另一类型不受影响，任意失败整体回滚。业务单元格按文本保存，是否报警和存储遥测空白时为0。返回导入的参数总条数。

参数解析配置统一通过Excel或制表符TXT导入维护；导入完成后同步实时曲线和实时数据组件的参数筛选项。

## 9. 数据校准

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/calibration/overview` | 查询累计野值数量 | 查询：`taskId`（必填） |
| GET | `/calibration/channels` | 校准通道列表 | 查询：`taskId` |
| GET | `/calibration/channels/{id}` | 校准通道详情 | 路径：`id` |
| POST | `/calibration/channels/create` | 新增校准通道 | `ChannelSaveRequest` |
| POST | `/calibration/channels/update` | 修改校准通道 | `ChannelSaveRequest`，`id` 必填 |
| POST | `/calibration/channels/delete` | 删除校准通道 | JSON：`id` |
| GET | `/calibration/records` | 分页查询校准记录汇总 | 查询：`taskId`、`pageNum=1`、`pageSize=20` |
| GET | `/calibration/records/{id}` | 查询记录汇总和分页野值详情 | 路径：`id`；查询：`taskId`、`pageNum=1`、`pageSize=20` |

`ChannelSaveRequest`：`id`、`taskId`、`channelName`、`channelType`（1 温度/2 电压/3 电流/4 功率/5 姿态传感器）、`calibrationFormula`（校准公式，与每个遥测参数的 calibration_formula 对应）、`normalRange`、`detectMethod`（1 莱特准则/2 阈值法/3 肖维涅法）、`sigmaValue`（莱特准则σ倍数，1至5）、`fluctuationRate`（阈值百分比，大于0且不超过100）、`chauvenetCoef`（肖维涅判别常数，固定0.5）、`minSampleCount`（肖维涅法参与计算的最近样本数，不小于3）、`iterateCount`（肖维涅法迭代次数，1至3）、`sampleWindow`（莱特准则最近样本窗口，不小于3）、`dynamicUpdate`、`autoClean`（是否自动排除异常值）、`enabled`、`channelStatus`。只校验当前检测方法对应的专用参数。

新增校准通道时自动创建一条野值数量为 0 的校准记录。通道与记录一对一，野值详情与记录一对多；记录由数据处理流程维护，不提供手工新增、修改和删除接口。数据处理服务每 5 秒批量保存野值详情，并按全局唯一的校准通道 ID 累加汇总数量。

## 10. 告警管理

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/alarms/system-monitor/current` | 当前系统健康状态 | 无参数 |
| GET | `/alarms/system-monitor/history` | 系统健康趋势 | 查询：`minutes=60` |
| GET | `/alarms/system-monitor/events` | 系统健康告警事件 | 查询：`status?` |
| GET | `/alarms/data` | 组合筛选并分页查询数据告警 | 查询：`taskId?`、`sourceName?`、`keyword?`、`alarmLevel?`、`alarmStatus?`、`startTime?`、`endTime?`、`pageNum=1`、`pageSize=20` |
| GET | `/alarms/data/sources` | 数据告警来源下拉选项 | 查询：`taskId?` |
| GET | `/alarms/data/{id}` | 数据告警详情 | 路径：`id` |
| POST | `/alarms/data/create` | 新增数据告警 | `DataAlarmSaveRequest` |
| POST | `/alarms/data/update` | 修改数据告警 | `DataAlarmSaveRequest`，`id` 必填 |
| POST | `/alarms/data/delete` | 删除数据告警 | JSON：`id` |
| POST | `/alarms/data/status` | 处理/恢复数据告警 | JSON：`id`、`status`（0/1） |
| GET | `/alarms/system` | 系统告警列表 | 无参数 |
| GET | `/alarms/system/{id}` | 系统告警详情 | 路径：`id` |
| POST | `/alarms/system/create` | 新增系统告警 | `SystemAlarmSaveRequest` |
| POST | `/alarms/system/update` | 修改系统告警 | `SystemAlarmSaveRequest`，`id` 必填 |
| POST | `/alarms/system/delete` | 删除系统告警 | JSON：`id` |
| POST | `/alarms/system/status` | 恢复/重新打开系统告警 | JSON：`id`、`status`（0/1） |

`DataAlarmSaveRequest`：`id`、`taskId`、`alarmTime`、`sourceName`、`alarmMsg`、`alarmLevel`（1～5）、`alarmStatus`、`taskName`、`channelCode`、`triggerRule`。

数据告警列表的 `sourceName` 为来源精确匹配；`keyword` 会模糊匹配来源、告警信息、任务名称、通道编码和触发规则；时间范围使用 ISO 8601 格式并包含起止时间。

`SystemAlarmSaveRequest`：`id`、`alarmTime`、`objName`、`alarmType`、`alarmContent`、`alarmLevel`（1～3）、`alarmStatus`。

## 11. 系统日志

| 方法 | 地址 | 说明 | 参数 |
| --- | --- | --- | --- |
| GET | `/system/logs` | 组合筛选并分页查询系统日志 | 查询：`taskId?`、`logLevel?`、`logSource?`、`operatorId?`、`keyword?`、`startTime?`、`endTime?`、`pageNum=1`、`pageSize=20` |
| GET | `/system/logs/sources` | 系统日志来源下拉选项 | 查询：`taskId?` |
| GET | `/system/logs/{id}` | 系统日志详情 | 路径：`id` |

查询参数 `logLevel` 使用枚举编码：`DEBUG` 调试、`INFO` 信息、`WARN` 警告、`ERROR` 错误。日志列表和详情同时返回 `logLevel` 编码与 `logLevelName` 中文名称，前端可直接展示中文名称。`logSource` 精确匹配来源，`operatorId` 模糊匹配操作人，`keyword` 模糊匹配日志内容和详情；时间范围使用 ISO 8601 格式并包含起止时间。

系统日志页面只开放查询接口。日志新增由后台模块通过 `SystemLogService.createLog` 写入，不开放页面新增、修改或删除接口。

## 12. 调试建议

Apifox、Postman 或其他支持 OpenAPI 3.0 的工具可直接导入同目录下的 `apifox-openapi.yaml`。登录后，将返回的 Token 配置为 Bearer Token 即可调用其他接口。

OpenAPI 文件已为各模块请求 DTO 配置独立 Schema，包括字段类型、枚举、说明和示例值；导入 Apifox 后可直接生成请求参数表与 JSON 示例。

设备卫星表和参数解析配置表的建表定义统一维护在 `sql/business_schema.sql` 中。
