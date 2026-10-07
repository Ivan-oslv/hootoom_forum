# HOOTOOM Forum 后端

## 环境要求

- JDK 21
- MySQL 8.0
- Windows 使用仓库自带的 `mvnw.cmd`，不要求全局安装 Maven

## 本地数据库配置

后端默认连接 `127.0.0.1:3306/hootoom_forum`，默认用户名为 `root`。密码没有默认值，必须通过 `DB_PASSWORD` 环境变量注入，不得写入 `application.yml`、日志或 Git。

PowerShell 示例：

```powershell
cd backend
$env:DB_PASSWORD = '<你的本地数据库密码>'
$env:MAIL_OUTBOX_ENCRYPTION_KEY = '<Base64编码的32字节本地密钥>'
.\mvnw.cmd spring-boot:run
```

首次本地开发可用 PowerShell 生成独立密钥：

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

生成结果只放入 IDE 环境变量或本机密钥管理工具，不要提交到仓库。更换该密钥后，旧的待发送邮件任务将无法解密；正式环境必须通过受控的密钥轮换流程处理。

如用户名、端口或库名不同，可同时设置 `DB_USERNAME`、`DB_PORT`、`DB_NAME`。程序启动后可访问：

- 健康接口：`http://localhost:8080/api/v1/health`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- Actuator 健康检查：`http://localhost:8080/actuator/health`

## 已手工创建表时的 Flyway 基线

当前本地 `hootoom_forum` 已手工执行首版建表脚本，因此 `local` 配置默认开启 `baseline-on-migrate`。第一次成功连接时，Flyway 会在非空数据库中建立 `flyway_schema_history`，记录版本 `1` 基线，并跳过重复执行 `V1__init_schema.sql`。

使用前必须确认现有表结构确实与 `V1__init_schema.sql` 一致。Flyway 基线只登记版本，不会逐表反推验证手工建表结果。若数据库是全新空库，Flyway 会直接执行 V1。

共享、测试及生产环境不得开启 `FLYWAY_BASELINE_ON_MIGRATE`；这些环境必须由 Flyway 从空库创建，或通过经过评审的一次性基线流程接管。

## 验证命令

```powershell
cd backend
.\mvnw.cmd test
```

后续数据库变更只能新增迁移文件，例如 `V2__add_xxx.sql`，不能修改已经执行过的 V1。

## 当前认证接口

- `POST /api/v1/auth/register`：创建待验证账号，并在同一事务中生成验证令牌和加密邮件任务。
- `POST /api/v1/auth/email-verifications/confirm`：消费一次性令牌并激活账号。
- `POST /api/v1/auth/email-verifications`：受限流控制地重新发送验证邮件，响应不泄露邮箱是否存在。
- `POST /api/v1/auth/login`：使用用户名或邮箱登录，返回短期 Access Token，并设置 Refresh/CSRF Cookie。
- `POST /api/v1/auth/refresh`：校验可信 Origin 与 CSRF 后单次轮换 Refresh Token。
- `POST /api/v1/auth/logout`：幂等撤销当前 Refresh Token 并清除 Cookie。
- `POST /api/v1/auth/password-resets`：请求密码重置邮件，对任意邮箱返回相同响应。
- `POST /api/v1/auth/password-resets/confirm`：消费15分钟有效的一次性令牌并修改密码。
- `GET /api/v1/auth/sessions`：使用 Bearer Access Token 查询当前用户有效设备会话。
- `DELETE /api/v1/auth/sessions/{id}`：撤销当前用户指定会话，重复调用仍返回成功。

注册请求示例：

```json
{
  "username": "demo_user",
  "email": "demo@example.com",
  "password": "password123",
  "policyVersion": "2026-10-07",
  "policyAccepted": true
}
```

邮件 Outbox Worker 会定时领取任务并通过 SMTP 投递。数据库使用 `FOR UPDATE SKIP LOCKED` 和任务锁避免多个实例重复领取；失败任务按指数退避重试，达到上限后进入 `DEAD` 并输出不包含敏感数据的错误日志。数据库中不会保存明文验证令牌、明文邮箱或明文验证链接。

本地可以连接 Mailpit、MailHog 等测试 SMTP 服务，典型配置为：

```text
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS=false
MAIL_FROM=no-reply@localhost
```

连接真实邮件服务时，通过环境变量提供 `SMTP_USERNAME` 和 `SMTP_PASSWORD`，并按服务商要求启用认证和 STARTTLS。不得把邮件服务密码写入 YAML。

## 登录与刷新测试

登录请求同时支持用户名和邮箱：

```json
{
  "identifier": "demo_user",
  "password": "password123",
  "deviceName": "Local PowerShell"
}
```

PowerShell 可使用同一个 `WebRequestSession` 保留服务端设置的 Cookie：

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -WebSession $session -ContentType 'application/json' `
  -Body '{"identifier":"demo_user","password":"password123","deviceName":"Local PowerShell"}'

$csrf = $session.Cookies.GetCookies('http://localhost:8080/api/v1/auth')['HOOTOOM_XSRF'].Value
$refresh = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/refresh `
  -WebSession $session -Headers @{'Origin'='http://localhost:3000'; 'X-CSRF-Token'=$csrf}
```

本地和测试环境未配置 RSA 密钥时会生成进程级临时密钥，重启后旧 Access Token 自动失效。共享、预发布和生产环境必须通过 `AUTH_TOKEN_PRIVATE_KEY`（PKCS#8 DER Base64）、`AUTH_TOKEN_PUBLIC_KEY`（X.509 DER Base64）与 `AUTH_TOKEN_KEY_ID` 注入可轮换密钥；非 local/test 环境缺失密钥会拒绝启动。

生产 HTTPS 环境必须设置 `AUTH_COOKIE_SECURE=true`，并通过 `AUTH_TRUSTED_ORIGINS` 配置准确的前端 Origin，不允许使用通配符。

密码重置请求：

```json
{"email":"demo@example.com"}
```

无论邮箱是否注册都返回同一提示。重置邮件通过 Outbox 的 `RESET_PASSWORD` 模板投递；确认请求格式为：

```json
{"token":"邮件链接中的一次性令牌","newPassword":"newPassword123"}
```

重置成功后，该用户全部 Refresh Token 被撤销，旧 Access Token 也会因为 `tokenVersion` 递增而验证失败。

## 当前用户资料接口

- `GET /api/v1/users/me`：返回当前登录用户资料。
- `PUT /api/v1/users/me`：修改昵称、简介和系统内置头像。

更新请求必须携带上次查询得到的 `version`：

```json
{
  "nickname": "新的昵称",
  "bio": "个人简介",
  "avatarKey": "default-2",
  "version": 0
}
```

默认允许 `default-1` 至 `default-8`，可以通过 `PROFILE_ALLOWED_AVATAR_KEYS` 调整。接口不接受头像 URL 或上传文件。版本冲突返回 `RESOURCE_CONFLICT`；用户名、昵称和简介均经过敏感词规范化检查，响应不会暴露命中的具体词语。

## 后端代码边界

- `controller`：参数校验、鉴权入口和响应组装，不写业务规则。
- `service`：只放接口；`service/impl` 实现业务规则和事务边界。
- `mapper`：只声明数据访问方法；所有 SQL 写在 `resources/mapper/**/*.xml`。
- `entity`、`dto`、`vo`：分别用于持久化对象、入参和出参，禁止混用。
- `security`、`config`：分别承载认证授权与框架配置。
- 禁止使用注解 SQL、`BaseMapper` CRUD、`IService`、`QueryWrapper` 绕开 XML 规范。
- 异常只在责任边界记录一次；日志不得包含密码、令牌、验证码或完整敏感个人信息。
