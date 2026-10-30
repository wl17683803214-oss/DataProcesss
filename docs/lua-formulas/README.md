# Lua处理公式接入与导入说明

前端页面及请求响应对接见独立文档[前端对接：Lua脚本公式管理](../前端对接-Lua脚本公式管理.md)，涵盖分页、覆盖导入、批量删除、下载、试算和异步同步。

## 部署顺序

1. 新库使用更新后的 `sql/business_schema.sql`；尚未建公式表的现有库执行 `sql/20261029_add_lua_formula.sql`。已经部署旧历史版本结构的数据库执行 `sql/20261030_simplify_lua_formula.sql`，按编号保留原最高版本后移除版本列。数据库升级应与新版服务一起安排，脚本尚未自动执行。
2. 部署包含Lua公式功能的管理服务和处理服务，运行环境为Java 8，无需另装Lua。
3. 使用 `POST /processing/formulas/import` 上传本目录内的单个Lua文件，或上传 `处理公式示例.zip` 批量导入。
4. 遥测参数的 `formulaType` 填写脚本中的编号，`processParam` 继续使用斜线分隔，顺序不变。
5. 调用 `POST /processing/formulas/sync` 提交后台同步，也可保存采集配置触发现有同步。导入本身不切换正在执行的公式；同步后新帧使用当前脚本。接口响应表示排队成功，完成情况看运行日志和告警。

公式全系统共享，没有启用/停用字段，也不保存历史版本。一个编号只保存一份脚本；同编号重新导入直接覆盖源码并更新updateTime，createTime保留首次导入时间。相同源码重复导入不改变时间。需要备份时，覆盖前先导出。

## 脚本协议

每个文件必须返回包含 `code`、`name`、`calculate(input)` 的表。编号为1至20位数字，不能在同一ZIP中重复。名称为1至100个字符。文件内容是UTF-8文本，不接受预编译字节码。

输入字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| rawBytes | Lua数组 | 下标从1开始，每项为0至255的字节；顺序沿用现有取位器 |
| bitWidth | 整数 | 参数有效位宽；非整字节字段低位对齐，末字节未用高位补零 |
| params | Lua数组 | 斜线分隔的参数，按位置传入十进制数值文本；最多256项 |

没有 `input.x`，整数或浮点解码由每个公式自行选择。处理参数使用十进制文本，避免64位整数或高精度系数提前变成Lua双精度数。脚本可用 `tonumber` 进行普通浮点运算，也可使用精确线性工具。

输出字段：

| 字段 | 含义 |
| --- | --- |
| valueType | FLOAT、DOUBLE、TEXT、TIME、RAW，复用既有协议枚举 |
| value | 数值或十进制文本；文本类型不需要此字段 |
| valueText | TEXT和TIME必须提供，其他数值由Java按小数位配置生成 |

Java负责校验结果、四舍五入及组装消息。TIME必须是双精度消息可精确表示的整数计数，不应用小数位配置。数值状态范围按计算后的结果判断；TEXT不参与数值范围及野值检测。

## 可用工具

| 调用 | 含义 |
| --- | --- |
| tools.unsigned(rawBytes, "LE"或"BE") | 按指定字节序转无符号整数，返回精确十进制文本 |
| tools.float32(rawBytes, 字节序) | 4字节IEEE 754浮点解码，拒绝非有限值 |
| tools.float64(rawBytes, 字节序) | 8字节IEEE 754双精度解码 |
| tools.toFloat32(value) | 将数值转换为单精度并检查范围 |
| tools.linear(x, a, b) | 精确计算a*x+b，返回十进制文本 |
| tools.utf8(rawBytes) | 严格解码UTF-8，非法编码报错 |
| tools.timeText(ticks) | 以2000-01-01 12:00:00为起点，每单位0.1毫秒生成四位秒小数文本 |

LE是小端，BE是大端。公式109示例中 `3F 80 00 00` 按BE浮点解码得到1.0，等价于在小端机器上将这4字节反转后按本机float读取。这个约定以原先讨论的src构造方式为前提，上线前必须用实际设备报文核对。

## 随附公式

| 编号 | 行为 | 参数默认规则 |
| --- | --- | --- |
| 100 | 小端无符号整数，a*x+b，双精度结果 | 无参数为1、0；单参数只设置a |
| 109 | 大端浮点源码，k*x+b，最终单精度 | 至少两项才使用k、b；不足两项均默认1、0 |
| 300 | 小端无符号整数，a*x+b，单精度结果 | 无参数为1、0；单参数只设置a |
| 301 | 小端无符号整数转双精度 | 不使用参数 |
| 302 | UTF-8文本 | 不使用参数 |
| 303 | 小端整数作为时间计数 | 不使用参数 |

300至303在Java中不再有内置编号分支，必须先导入对应脚本。`formulaDesc` 仅作为历史描述保留，不再执行。

## 管理接口

| 方法与路径 | 用途 |
| --- | --- |
| GET /processing/formulas | 分页查询；pageNum默认1、pageSize默认20且最多200，可按code精确及name模糊筛选 |
| POST /processing/formulas/import | multipart字段file，接收.lua或.zip |
| GET /processing/formulas/export?code=100 | 下载当前脚本，文件名100.lua，不支持版本参数 |
| GET /processing/formulas/export-zip?codes=100,109 | 下载指定编号的当前脚本，不传codes则导出全部当前公式 |
| POST /processing/formulas/preview | 对已导入公式试算，不切换运行配置、不产生生产告警 |
| POST /processing/formulas/sync | 提交后台配置同步，运行接口在同步完成后使用当前脚本 |
| POST /processing/formulas/batch-delete | JSON传codes，单次1到100个；未引用公式才能整批删除，成功后提交后台同步 |

试算请求：

```json
{
  "code": "109",
  "rawHex": "3F 80 00 00",
  "bitWidth": 32,
  "processParam": "2/3"
}
```

预期data为 `{"code":"109","valueType":"单精度","value":5.0,"valueText":"5"}`。valueType查询展示为中文；脚本声明及消息存储仍使用英文枚举值。试算返回舍入前结果，实际遥测输出另应用参数配置的decimalPlaces。

## 异常与恢复

- formulaType为空：正常输出RAW，小端无符号整数文本保留精度，不因缺少公式产生告警。
- 编号已填写但无对应脚本：保留raw_data，类型标为BINARY，不生成计算值，记录“参数处理失败”告警，其他参数继续。
- 参数格式错误、脚本执行异常、结果不合法：同上处理，告警包含接口、遥测代号、名称、公式编号及原因。
- BINARY和TEXT的数值存储为NULL，不进入野值检测或曲线数值点。消息接收方也必须根据value_type判断，不能把Protobuf默认value=0当成计算结果。
- 导入缺失脚本或修正参数后同步配置，后续计算正常时自动恢复处理告警，不代替人工处理状态。失败历史数据不自动补算。

## 导入与执行边界

单文件64KB；上传包8MB；每批最多100个公式、200个ZIP条目；ZIP只允许Lua文件，不落地解压。任一文件语法或入口校验失败，整批不保存。参数配置允许公式编号为空。

模块初始化和calculate共享每次十万条指令及约100毫秒执行预算；字节数据最多64KB。每次执行环境独立，只缓存编译结果，不共享脚本可变全局变量。只开放基础函数、数学库和上述工具，不开放文件、系统、网络、协程、动态加载或Java反射。禁止字符串拼接操作，避免少量指令内指数扩张内存。它是受限的业务公式环境，不是完整Lua标准库环境。

导入只校验语法、元数据和入口，不对所有公式使用同一组输入强行试算。请使用preview及实际报文验证业务结果。

## 引擎选型

采用MIT许可证的LuaJ 3.0.1，纯Java、Lua 5.2语法，兼容Java 8。它的上游发布较旧，因此将引擎隔离在公共公式组件中，不直接依赖Lua 5.3函数。比较过LuaJava、JNLua和LuaJ其他分支：原生引擎方案需要额外处理平台库和部署；其他分支的版本和兼容性需要独立验证，本次不直接替换成熟API。

参考：
- https://github.com/luaj/luaj
- https://github.com/luaj/luaj/blob/master/examples/jse/SampleSandboxed.java
- https://github.com/gudzpoz/luajava
- https://github.com/MovingBlocks/JNLua

仅借鉴环境隔离和指令预算设计，业务代码自行实现；依赖按其原有许可证分发。

## 分页查询及批量删除示例

查询：GET /processing/formulas?pageNum=1&pageSize=20&code=109。响应data包含pageNum、pageSize、total、records，记录字段为code、name、createTime、updateTime，不返回源码或版本字段。

删除：POST /processing/formulas/batch-delete，请求体为{"codes":["100","109"]}，成功响应data为实际删除数量。重复编号只计一次。任一编号不存在或被任意任务的有效参数配置引用时，整批不删除，错误消息列出相关编号。检查与删除使用同一事务，期间参数写入会短暂等待。

删除成功后通过已有队列提交后台配置同步。响应不代表同步已经完成；同步前的内存快照及已开始的帧不会被强行中断，完成情况查看运行日志和告警。
