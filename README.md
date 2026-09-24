# 数据处理软件

基于 JDK 8、Spring Boot 2.6、Spring Cloud Alibaba、MyBatis 和人大金仓构建。
项目不再使用 Spring Cloud Gateway，两个业务服务直接对外提供接口并独立鉴权。

## 模块说明

| 模块 | 说明 |
| --- | --- |
| `common` | 通用响应、JWT 工具和登录用户模型 |
| `common-security` | 公共 Token 过滤器、登录上下文、权限注解和 AOP |
| `data-admin-service` | 登录、动态路由和后台管理业务，端口 `8082` |
| `data-process-service` | 具体数据处理业务，端口 `8083` |

## 依赖环境

- Nacos：`127.0.0.1:8848`
- Redis：`127.0.0.1:6379`
- KingbaseES：`localhost:54321/data_processing`

## Nacos 配置

将以下文件导入 Nacos，Group 使用 `DEFAULT_GROUP`，格式选择 YAML：

- `nacos-config/data-admin-service.yaml` -> `data-admin-service.yaml`
- `nacos-config/data-process-service.yaml` -> `data-process-service.yaml`

`data-admin-service` 和 `data-process-service` 必须使用相同的 JWT 密钥和 Redis 数据库。

## 数据库初始化

依次执行：

```text
sql/system.sql
sql/business_schema.sql
```

初始化管理员账号为 `admin`，密码为 `admin123`。

## 启动

```text
mvn -pl data-admin-service spring-boot:run
mvn -pl data-process-service spring-boot:run
```

## 接口地址

登录：

```http
POST http://localhost:8082/auth/login
Content-Type: application/json

{"username":"admin","password":"admin123"}
```

其他接口必须携带：

```http
Authorization: Bearer <token>
```

示例：

- `GET http://localhost:8082/auth/user-info`
- `GET http://localhost:8082/auth/routes`
- `GET http://localhost:8082/system/users`

Swagger 调试页面：

- `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8082/v3/api-docs`

在 Swagger 页面先调用登录接口获取 Token，再点击右上角 `Authorize`，仅填写 Token 本身即可调试其他接口，无需重复输入 `Bearer` 前缀。

完整接口说明参见 `docs/API.md`，逐接口入参与出参示例参见 `docs/API-EXAMPLES.md`，本系统接口可导入 Apifox 文件 `docs/apifox-openapi.yaml`，交互系统心跳上报接口可单独导入 `docs/apifox-interaction-heartbeat.yaml`。

系统左侧导航页面由 `sql/system.sql` 预置到 `dp_menu`，角色通过后台接口绑定可见页面；暂未提供页面自身的增删改查接口。
