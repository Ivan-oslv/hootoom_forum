# 数据模型

## 1. 设计原则

- MVP 共 15 张表，只保留当前业务闭环必需的数据。
- 主键使用 MySQL `BIGINT UNSIGNED AUTO_INCREMENT`，API 中以字符串返回。
- 数据库使用 InnoDB、`utf8mb4` 和统一 collation；`DATETIME(3)` 按 UTC 写入。
- 核心关系使用 `ON DELETE RESTRICT ON UPDATE RESTRICT` 外键，不使用级联删除。
- 内容采用软删除；并发更新使用 `version` 乐观锁。
- 枚举保存稳定英文字符串，并使用 CHECK 约束。
- 实际字段、索引和中文注释以 [V1__init_schema.sql](../backend/src/main/resources/db/migration/V1__init_schema.sql) 为准。

## 2. 表清单

| 模块 | 表 | 作用 |
|---|---|---|
| 用户 | forum_user | 前台账号、邮箱验证状态和资料 |
| 后台 | admin_user | 后台账号及固定 OPERATOR、ADMIN 角色编码 |
| 认证 | refresh_token | 前后台刷新令牌与设备会话 |
| 认证 | email_token | 邮箱验证和密码重置令牌 |
| 内容 | category | 论坛板块 |
| 内容 | forum_post | 帖子状态、统计和当前版本指针 |
| 内容 | post_revision | 帖子版本、正文和审核结果 |
| 内容 | forum_comment | 一级评论和审核状态 |
| 互动 | post_like | 点赞关系 |
| 互动 | post_favorite | 收藏关系 |
| 治理 | report | 帖子和评论举报 |
| 治理 | sensitive_word | 敏感词规则 |
| 审计 | audit_log | 管理和安全关键操作审计 |
| 邮件 | mail_outbox | 邮件可靠投递任务 |
| 统计 | post_daily_stat | 每日热门度指标 |

## 3. 简化决策

- 后台只有两个固定角色，不建立角色、权限和关联表。`admin_user.role_code` 使用 CHECK 限定为 OPERATOR 或 ADMIN，具体权限由后端代码维护。
- 标签属于后续功能，MVP 不创建 tag、post_tag。
- 浏览量接受近似统计，不保存逐用户浏览明细；匿名 Cookie 与应用逻辑负责当天防重复，`post_daily_stat` 只保存聚合结果。
- 创建帖子和评论使用 `client_request_id`，分别通过“用户 + 请求ID”唯一约束防止网络重试产生重复数据，不建立通用幂等表。
- 登录风险事件写结构化日志，关键管理动作写 `audit_log`，不单独建立安全事件表。
- 系统配置使用环境变量和 YAML，不在数据库中维护配置表。

## 4. 核心关系

- `forum_post.category_id → category.id`
- `forum_post.author_user_id → forum_user.id`
- `forum_post.published_revision_id/pending_revision_id → post_revision.id`
- 帖子版本指针使用复合外键，保证版本确实属于当前帖子。
- `forum_comment.reply_to_comment_id + post_id` 使用复合外键，保证回复对象属于同一帖子。
- 点赞和收藏使用复合主键，天然防止重复操作。
- 举报使用生成列 `active_key` 唯一索引，保证一个用户对同一目标最多一条待处理举报。

## 5. 帖子版本

`forum_post` 保存业务状态、统计值以及发布/待审版本指针；标题和正文只存在 `post_revision`：

- 首次发帖创建 revision 1。
- 自动通过时设置 AUTO 和发布时间。
- 需要审核时仅设置 pending_revision_id。
- 编辑已公开帖子时创建新版本，旧 published_revision_id 保持不变。
- 审核通过后在同一事务中切换 published_revision_id、category_id，并清空 pending_revision_id。
- FULLTEXT ngram 只搜索 published_revision_id 指向的版本。

## 6. 计数与热门排序

- `forum_post` 保存累计浏览、点赞、收藏和公开评论数快照。
- 点赞、收藏关系变更和帖子计数更新位于同一事务，使用原子增减。
- `post_daily_stat` 保存 Asia/Shanghai 日期下的浏览、新增点赞和新发布评论事件数。
- 过去 7 天热门分数由每日统计汇总，不因后续取消点赞或删除评论回改历史事件数。
- 定时任务每天校准累计统计；发现差异时记录日志和指标。

## 7. 数据生命周期

- 软删除帖子保留版本、评论、点赞、收藏和举报关系，恢复时重新校准统计。
- Refresh Token、Email Token 和 Mail Outbox 按部署文档定期清理。Mail Outbox 的收件人和模板参数均加密保存，并记录密钥版本以支持轮换。
- Audit Log 仅追加，默认保留 180 天；导出和清理必须受控并留痕。
- 已在共享环境执行的 Flyway 迁移不可修改，后续变化必须新增版本。
