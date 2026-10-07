# HOOTOOM Forum

HOOTOOM Forum 是面向跨境电商、品牌出海和海外广告投放客户的官方社区，用于公告发布、经验沉淀、问题反馈和运营互动。

## 第一阶段目标

- 游客可以浏览和搜索公开内容。
- 注册用户可以发帖、评论、点赞和收藏，并管理自己的内容。
- 运营人员可以发布官方内容并审核、管理社区内容。
- 管理员可以管理用户、板块、后台账号、权限和系统配置。
- 核心流程具有自动化测试，项目能从空数据库启动。

## 技术基线

| 层级 | 方案 |
|---|---|
| 后端 | Java 21、Spring Boot 3.x、Maven、MyBatis-Plus |
| 前端 | Vue 3、TypeScript、Nuxt 3 SSR、Pinia |
| 数据库 | MySQL 8.0 |
| 数据迁移 | Flyway |
| 接口文档 | OpenAPI 3 / Swagger UI |
| 身份认证 | Access Token + Refresh Token |
| 内容格式 | Markdown，服务端净化后渲染 |
| 测试 | JUnit 5、Spring Boot Test、Vitest、Playwright |
| 邮件 | SMTP/API 邮件服务，用于验证、找回密码和审核通知 |

具体小版本在首次创建工程时锁定并记录在构建文件中。MVP 搜索使用 MySQL FULLTEXT ngram。单实例演示不强制 Redis；公开生产环境采用多实例时，Redis 或统一网关限流属于必需依赖。Elasticsearch、短信和对象存储不属于 MVP 强制依赖。

## 文档导航

| 文档 | 用途 |
|---|---|
| [产品需求](docs/product-requirements.md) | 产品范围、页面、业务规则和状态流转 |
| [角色权限](docs/permissions.md) | 前后台角色及操作边界 |
| [数据模型](docs/data-model.md) | 表结构、约束、索引和数据生命周期 |
| [数据库规格](docs/schema-specification.md) | 核心表字段类型、空值、默认值和外键约束 |
| [接口规范](docs/api-conventions.md) | REST、认证、分页、错误码和接口清单 |
| [安全与隐私](docs/security.md) | 认证、XSS、限流、上传、审计和隐私要求 |
| [测试与验收](docs/acceptance.md) | 自动化测试要求和可判定验收场景 |
| [环境与部署](docs/deployment.md) | 环境划分、配置、启动、迁移和上线要求 |
| [开发规范](docs/development-guidelines.md) | 前后端分层、编码边界、Mapper XML 与 SEO 规范 |
| [日志与可观测性](docs/logging-observability.md) | 日志级别、链路标识、脱敏、异常记录与告警规范 |

## 文档规则

- 产品范围变化先修改产品需求，再同步数据、接口和验收文档。
- 新增接口必须进入 OpenAPI，并具有权限和错误场景说明。
- 新增状态必须说明允许的流转和操作者。
- 实现与文档冲突时，以已评审并合入主分支的最新文档为准。

## 当前状态

首版 MySQL 结构、后端认证与用户资料模块及 Nuxt 前端基础工程已经建立。后端具备邮箱注册验证、登录、Token 刷新、退出、密码重置、会话管理、个人资料、邮件发件箱、统一响应与异常处理、Spring Security、请求链路标识和结构化日志。前端具备分层 API 客户端、内存态认证、自动刷新、登录注册与找回密码页面、用户资料页及 SEO 基础设施。

- 后端本地启动、数据库接入及现有数据库基线处理见 [后端说明](backend/README.md)。
- 前端目录职责、启动方式和安全约束见 [前端说明](frontend/README.md)。
- 首版数据库迁移为 [V1__init_schema.sql](backend/src/main/resources/db/migration/V1__init_schema.sql)。
- 后续表结构调整必须新增 `V2__*.sql`、`V3__*.sql` 等迁移，不得修改已在任何共享环境执行过的迁移。
- 真实密码、令牌和生产密钥不得提交到仓库；本地配置模板见 [.env.example](.env.example)。
