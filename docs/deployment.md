# 环境与部署

## 1. 环境基线

| 组件 | 基线 |
|---|---|
| JDK | 21 LTS |
| Spring Boot | 3.x，首次建项锁定具体版本 |
| 后端构建 | Maven + Maven Wrapper |
| Node.js | 建项时选定的 LTS，锁定 `.nvmrc` |
| 前端 | Vue 3 + TypeScript + Nuxt 3 SSR + Pinia |
| MySQL | 8.0 |
| 构建 | Maven Wrapper、pnpm + Corepack + `pnpm-lock.yaml` |

环境分为 local、test、staging、production。配置通过 Spring Profile 与前端环境文件模板区分；真实密钥只通过环境变量或密钥系统注入。

## 2. 建议目录

```text
/
├─ backend/
│  ├─ src/main/java/
│  ├─ src/main/resources/db/migration/
│  └─ src/test/
├─ frontend/
│  ├─ src/
│  └─ tests/
├─ docs/
├─ deploy/
├─ .env.example
└─ README.md
```

前后端目录、职责边界和 SEO 实现要求见 [development-guidelines.md](development-guidelines.md)。

## 3. 配置项

`.env.example` 只列变量名和无敏感示例，至少包括：

```text
APP_ENV
APP_PUBLIC_URL
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_ACCESS_PRIVATE_KEY
JWT_ACCESS_PUBLIC_KEY
REFRESH_TOKEN_SECRET
CORS_ALLOWED_ORIGINS
TRUSTED_PROXY_CIDRS
MAIL_PROVIDER
MAIL_FROM
MAIL_API_KEY
REDIS_URL
LOG_FORMAT
LOG_LEVEL_ROOT
OTEL_EXPORTER_OTLP_ENDPOINT
INITIAL_ADMIN_USERNAME
INITIAL_ADMIN_PASSWORD
```

生产环境启动时若仍使用示例密钥、空密码、宽泛 CORS 或调试模式，应直接失败。

## 4. 本地启动目标

工程建立后必须提供以下等价能力，并将真实命令更新到根 README：

1. 启动 MySQL 8。
2. 执行 Flyway 迁移和开发种子数据。
3. 使用 Maven Wrapper 启动后端。
4. 使用 Corepack 和锁定的 pnpm 安装依赖并启动 Nuxt SSR 开发服务器。
5. 一条命令运行后端、前端和端到端测试。

推荐提供 Docker Compose 仅用于本地依赖；应用本身是否容器化不影响 MVP 验收。开发配置不得自动连接生产服务。

## 5. 数据库迁移

- 迁移脚本命名、版本顺序和校验由 Flyway 管理。
- 已在共享环境运行的迁移不得修改；修复通过新迁移完成。
- 生产迁移前创建可验证备份，在 staging 使用生产规模副本演练。
- 破坏性结构变更采用扩展—迁移—收缩策略，不在单次上线直接删除仍被旧代码使用的列。
- 数据迁移失败时应用不得带着不完整结构启动。

## 6. 部署与运维

- 推荐拓扑为“浏览器 → CDN/Nginx → Nuxt SSR → Spring Boot API → MySQL”。Nuxt 是需要独立运行和健康检查的 Node 服务，不得按纯静态站点部署；静态资源可由 CDN/Nginx 缓存。
- Spring Boot API 保持无状态。单实例演示可不启用 Redis；多实例公开生产必须使用 Redis 或网关统一处理限流与必要的短期协调。
- 健康检查区分存活与就绪；就绪检查数据库连接和迁移状态。
- 日志使用结构化 JSON，包含时间、级别、服务、环境、发布版本、requestId/traceId、规范路由、状态和耗时；详细字段、脱敏、保留及告警规则见 [logging-observability.md](logging-observability.md)。
- 监控至少覆盖 QPS、P95/P99 延迟、4xx/5xx、JVM、线程、连接池、慢 SQL、磁盘和数据库容量。
- 数据库每日全量备份，并按风险增加增量或 binlog；至少每季度演练恢复。
- MVP 生产目标为 RPO 不超过 24 小时、RTO 不超过 4 小时；若业务上线前提出更高要求，必须先升级备份频率、binlog 和恢复方案。
- 发布使用滚动或蓝绿策略；数据库变更必须兼容新旧应用短期共存。

### 可观测性

- Spring Boot Actuator 仅对受控网络暴露 `/actuator/health/liveness`、`/actuator/health/readiness` 和经鉴权的指标端点，不暴露环境变量、Bean、堆转储等敏感端点。
- 使用 Micrometer 输出请求量、P50/P95/P99、错误率、JVM、线程池、连接池、邮件队列、审核积压、限流命中和 Token 重放指标。
- `X-Request-Id` 在 CDN/Nginx、Nuxt 和 Spring Boot 间透传；日志使用同一 requestId，必要时增加 traceId。
- 慢 SQL 初始阈值 500ms；发现全表扫描、连接池耗尽、磁盘不足、邮件持续失败或审核积压时触发告警。
- 健康与指标响应不得包含数据库地址、版本、账号、密钥或内部异常堆栈。

### 缓存

- 匿名公开页面可在 CDN/SSR 层短时缓存；登录态页面、预览和后台页面禁止共享缓存。
- 帖子或板块发生发布、编辑、删除、恢复、锁定、迁移时主动失效相关页面和 API 缓存。
- 缓存键必须包含规范路径、语言（未来启用时）和影响响应的公开筛选参数，不包含 Token 或个人信息。

### 定时任务

- 使用数据库锁或单实例调度锁保证集群中同一任务只有一个执行者。
- 每日清理已过期且超过安全保留期的邮件令牌、刷新令牌和已完成邮件任务。
- 邮件 Outbox 采用指数退避重试并设置最大次数；进入死信状态后告警，不无限重试。
- 每日校准帖子、板块统计计数；发现差异时记录指标和审计信息，不静默覆盖异常原因。
- sitemap 更新、软删除物理清理和历史内容回扫分别使用独立任务，均支持断点与幂等执行。

## 7. CI 质量门禁

每次合并至少执行：

- 后端编译、格式检查、单元与集成测试。
- 前端类型检查、Lint、单元测试和生产构建。
- 核心 Playwright 流程。
- SEO 自动检查：状态码、标题、描述、canonical、robots、sitemap、结构化数据和关键页面可抓取性。
- 依赖漏洞与密钥泄露扫描。
- 许可证扫描与 SBOM 生成。
- Flyway 迁移校验和 OpenAPI 兼容性检查。

任一必需步骤失败不得发布。生产发布应保留构建版本、Git 提交、迁移版本和操作者记录。
