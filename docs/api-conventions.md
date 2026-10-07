# API 规范

## 1. 基本约定

- 根路径 `/api/v1`；后台路径 `/api/v1/admin`。
- 请求和响应使用 UTF-8 JSON；字段为 `camelCase`。
- ID 在 JSON 中使用字符串；时间使用 ISO 8601 UTC，例如 `2026-10-07T08:30:00.000Z`。
- OpenAPI 是接口契约，代码变更必须同步契约和示例。
- `GET`、`PUT`、`DELETE` 应具备幂等性。创建帖子和评论的请求体必须包含客户端生成的 `clientRequestId`；同一用户重复提交相同 ID 时返回已创建资源，不再创建新记录。

成功响应：

```json
{"code":"OK","message":"success","data":{},"requestId":"01K..."}
```

错误响应：

```json
{
  "code":"VALIDATION_ERROR",
  "message":"请求参数不正确",
  "details":[{"field":"title","reason":"长度必须为 5 到 120"}],
  "requestId":"01K..."
}
```

## 2. 状态码和错误码

| HTTP | 典型业务码 |
|---:|---|
| 400 | VALIDATION_ERROR、INVALID_REQUEST |
| 401 | UNAUTHENTICATED、TOKEN_EXPIRED、TOKEN_REPLAYED |
| 403 | FORBIDDEN、ACCOUNT_DISABLED、EMAIL_NOT_VERIFIED |
| 404 | RESOURCE_NOT_FOUND |
| 409 | USERNAME_EXISTS、EMAIL_EXISTS、POLICY_VERSION_OUTDATED、RESOURCE_CONFLICT |
| 413 | PAYLOAD_TOO_LARGE |
| 422 | CONTENT_REJECTED |
| 429 | RATE_LIMITED |
| 500 | INTERNAL_ERROR |

响应不得泄露堆栈、SQL、内部类名或账号是否存在等可被滥用的信息。

## 3. 分页和排序

请求：`page=1&pageSize=20&sort=latest`。page 从 1 开始，pageSize 最大 100；非法排序字段返回 400，不得直接拼接到 SQL。

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0,
  "totalPages": 0
}
```

MVP 使用页码分页；数据量增长后可为帖子流新增游标分页，但不得静默改变现有语义。

## 4. 认证

- 前台：`POST /auth/register`、`/auth/login`、`/auth/refresh`、`/auth/logout`。
- 后台：`POST /admin/auth/login`、`/admin/auth/refresh`、`/admin/auth/logout`。
- Web 与 API 采用同站部署。Access Token 有效期 15 分钟，只保存在前端运行时内存并通过 `Authorization: Bearer <token>` 发送，禁止写入 localStorage、sessionStorage 或非 HttpOnly Cookie。
- Refresh Token 有效期 7 天、单次轮换，只通过 `Secure; HttpOnly; SameSite=Lax; Path=/api/v1/auth` Cookie 传输；后台使用独立 Cookie 路径 `/api/v1/admin/auth`。
- Refresh 与 Logout 请求必须携带服务端生成的 CSRF Token，并校验可信 Origin/Referer。
- 前台 Refresh Cookie 名为 `HOOTOOM_REFRESH`、路径为 `/api/v1/auth`；CSRF Cookie 名为 `HOOTOOM_XSRF`、路径为 `/`，以便前端页面读取后通过 `X-CSRF-Token` 请求头回传。两个 Cookie 均使用 `SameSite=Lax`，Refresh Cookie 额外使用 `HttpOnly`，生产环境两者均启用 `Secure`。
- JWT 使用非对称签名并携带 `kid`、`iss`、`aud`、`sub`、`jti`、`iat`、`exp` 和 `tokenVersion`；签名密钥支持轮换。
- 登出撤销当前 Refresh Token；密码或账号状态改变时撤销该主体全部会话。
- 密码重置申请对已注册和未注册邮箱返回相同的成功文案与状态码；重置成功后递增 `tokenVersion`，撤销该用户全部 Refresh Token。
- `GET /auth/sessions` 仅返回当前用户未撤销且未过期的会话摘要；`DELETE /auth/sessions/{id}` 必须同时按用户 ID 和会话 ID 限定，避免横向越权，并保持幂等。
- 前后台 issuer/audience 分离。
- 支持查询和撤销当前用户的设备会话；SSR 只根据安全 Cookie 获取最小会话状态，不把 Token 注入页面 HTML。

## 5. 接口清单

### 前台

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/email-verifications
POST   /api/v1/auth/email-verifications/confirm
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/password-resets
POST   /api/v1/auth/password-resets/confirm
GET    /api/v1/auth/sessions
DELETE /api/v1/auth/sessions/{id}
GET    /api/v1/users/me
PUT    /api/v1/users/me

GET    /api/v1/categories
GET    /api/v1/categories/{idOrSlug}

GET    /api/v1/posts
GET    /api/v1/posts/{id}
POST   /api/v1/posts
PUT    /api/v1/posts/{id}
DELETE /api/v1/posts/{id}
PUT    /api/v1/posts/{id}/like
DELETE /api/v1/posts/{id}/like
PUT    /api/v1/posts/{id}/favorite
DELETE /api/v1/posts/{id}/favorite
POST   /api/v1/posts/{id}/reports

GET    /api/v1/posts/{postId}/comments
POST   /api/v1/posts/{postId}/comments
PUT    /api/v1/comments/{id}
DELETE /api/v1/comments/{id}

GET    /api/v1/search/posts
GET    /api/v1/me/posts
GET    /api/v1/me/comments
GET    /api/v1/me/favorites
```

### 后台

```text
GET    /api/v1/admin/dashboard
GET    /api/v1/admin/users
GET    /api/v1/admin/users/{id}
PUT    /api/v1/admin/users/{id}/status

POST   /api/v1/admin/categories
GET    /api/v1/admin/categories
PUT    /api/v1/admin/categories/{id}
PUT    /api/v1/admin/categories/{id}/status
PUT    /api/v1/admin/categories/order

GET    /api/v1/admin/posts
POST   /api/v1/admin/official-posts
PUT    /api/v1/admin/official-posts/{id}
PUT    /api/v1/admin/posts/{id}/review
PUT    /api/v1/admin/posts/{id}/lock
PUT    /api/v1/admin/posts/{id}/pin
PUT    /api/v1/admin/posts/{id}/featured
DELETE /api/v1/admin/posts/{id}
POST   /api/v1/admin/posts/{id}/restore

GET    /api/v1/admin/comments
PUT    /api/v1/admin/comments/{id}/review
DELETE /api/v1/admin/comments/{id}
POST   /api/v1/admin/comments/{id}/restore

GET    /api/v1/admin/reports
PUT    /api/v1/admin/reports/{id}/resolution
GET    /api/v1/admin/sensitive-words
POST   /api/v1/admin/sensitive-words
PUT    /api/v1/admin/sensitive-words/{id}
DELETE /api/v1/admin/sensitive-words/{id}

GET    /api/v1/admin/accounts
POST   /api/v1/admin/accounts
PUT    /api/v1/admin/accounts/{id}/status
PUT    /api/v1/admin/accounts/{id}/role
GET    /api/v1/admin/audit-logs
```

## 6. 并发和校验

- 编辑帖子、评论和板块时请求携带 `version`；版本冲突返回 409。
- `PUT /comments/{id}` 仅允许修改尚未公开的待审或驳回评论；已公开评论返回 409，用户应删除后重新发布。
- 点赞、收藏、退出登录和删除操作可重复调用并返回目标最终状态。
- `clientRequestId` 使用 UUID/ULID 等高熵值，长度不超过 64；同一用户使用相同 ID 但内容不同返回 409。
- 所有路径 ID、筛选、正文和枚举在 Controller 边界校验；业务层再次校验权限和状态。
- `PUT /users/me` 请求包含 `nickname`、`bio`、`avatarKey` 和 `version`；头像标识必须来自服务端配置的内置头像白名单，版本冲突返回 409。
- 每个响应携带或回显 `X-Request-Id`，用于日志关联。

## 7. HTTP 缓存与资源状态

- 匿名板块列表和公开帖子详情可返回 ETag/Last-Modified，并允许短时公共缓存；带个性化状态的响应使用 `private, no-store`。
- 登录、用户中心、管理后台、Token 和包含邮箱等个人信息的响应必须 `Cache-Control: no-store`。
- 内容发布、编辑、锁定、删除、恢复或迁移板块时，必须失效 SSR 与 CDN 对应缓存。
- 永久删除且无恢复可能的公开资源返回 410；未发布、待审、暂时隐藏或调用者无权判断其存在时返回 404。
- sitemap 与 robots 可缓存，但内容状态变化后必须在约定时间内刷新。
