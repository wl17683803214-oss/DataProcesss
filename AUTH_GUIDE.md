# 登录与权限模块

## 依赖服务

- JDK 8、Maven
- Nacos：`127.0.0.1:8848`
- Redis：`127.0.0.1:6379`
- KingbaseES：`localhost:54321/test`

## 初始化

1. 在 KingbaseES 执行 `sql/system.sql`。
2. 将以下配置导入 Nacos，Group 使用 `DEFAULT_GROUP`，格式使用 YAML：
   - `nacos-config/data-admin-service.yaml` -> `data-admin-service.yaml`
   - `nacos-config/data-process-service.yaml` -> `data-process-service.yaml`
3. 生产环境必须同时修改两个业务服务的 `auth.jwt-secret`，两处值必须一致。

初始化管理员账号：`admin`，密码：`admin123`。系统服务首次启动时会立即将初始化密码转换为 BCrypt 密文。

## 启动顺序

```text
mvn -pl data-admin-service spring-boot:run
mvn -pl data-process-service spring-boot:run
```

## 接口

后台管理接口使用 `http://localhost:8082`，数据处理接口使用 `http://localhost:8083`。

```http
POST http://localhost:8082/auth/login
Content-Type: application/json

{"username":"admin","password":"admin123"}
```

登录后携带响应中的 Token：

```http
Authorization: Bearer <token>
```

- `POST http://localhost:8082/auth/logout`
- `GET http://localhost:8082/auth/user-info`
- `GET http://localhost:8082/auth/routes`
- `GET http://localhost:8082/system/users`
- `POST http://localhost:8082/system/users/create`
- `POST http://localhost:8082/system/users/update`
- `POST http://localhost:8082/system/users/delete`
- `POST http://localhost:8082/system/users/reset-password`

完整接口清单参见 `docs/API.md`；Apifox、Postman 可导入 `docs/apifox-openapi.yaml`。

两个服务都会独立校验 `Authorization: Bearer <token>` 和 Redis 登录会话，不再依赖 Gateway。
