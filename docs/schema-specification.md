# 数据库规格

可执行结构位于 [V1__init_schema.sql](../backend/src/main/resources/db/migration/V1__init_schema.sql)。该脚本是字段类型、约束、索引和中文注释的唯一实现基准。

## 1. 基础约定

- MySQL 8.0、InnoDB、utf8mb4、`utf8mb4_0900_ai_ci`。
- 主键为 `BIGINT UNSIGNED AUTO_INCREMENT`。
- 时间使用 `DATETIME(3)`，应用按 UTC 写入。
- JSON 中的 ID 序列化为字符串。
- `created_at`、`updated_at` 为公共时间字段；软删除内容增加 `deleted_at`；并发更新表增加 `version`。
- 所有表和字段必须带中文 COMMENT。

## 2. 账号和认证

### `forum_user`

保存用户名、邮箱、验证时间、密码哈希、Token 版本、昵称、系统头像、简介、账号状态、协议版本和登录信息。用户名与邮箱分别唯一。

### `admin_user`

后台只提供 OPERATOR 和 ADMIN 两个固定角色。`role_code` 直接保存在后台账号表，并通过 CHECK 约束限定，不提供动态权限组合。

### `refresh_token`、`email_token`

Refresh Token 只保存 SHA-256 哈希，并通过互斥的 `user_id`、`admin_user_id` 外键关联主体。Email Token 用于验证邮箱和重置密码，只保存哈希、用途、有效期与使用状态。

## 3. 内容

### `category`

保存名称、唯一 slug、简介、图标、排序、推荐标记、状态和帖子计数。

### `forum_post`、`post_revision`

帖子主表保存作者、板块、业务状态、互动计数和发布/待审版本指针。版本表保存标题、Markdown、净化 HTML、目标板块、审核状态及创建和审核人员。版本表包含 ngram FULLTEXT 索引。

帖子和评论分别保存 `client_request_id`，通过用户与请求标识的组合唯一约束避免重复提交。

### `forum_comment`

保存一级评论、固定楼层、可选回复对象、审核状态、软删除状态和审核人员。复合外键保证回复目标属于同一帖子。

## 4. 互动和治理

- `post_like`：用户和帖子组成复合主键。
- `post_favorite`：用户和帖子组成复合主键，并按收藏时间支持用户列表查询。
- `report`：支持帖子和评论举报；生成列唯一约束防止重复待处理举报。
- `sensitive_word`：保存原词、规范化词、匹配方式、动作和状态。
- `audit_log`：保存管理操作和关键安全操作的脱敏快照与 requestId。

## 5. 邮件和统计

- `mail_outbox`：收件人和模板参数分别加密保存，并记录 `encryption_key_version`；同时保存模板编码、业务幂等键、投递状态和重试信息。
- `post_daily_stat`：按 Asia/Shanghai 日期保存浏览、新增点赞和新发布评论事件，用于过去 7 天热门排序。

## 6. 索引原则

- 唯一索引用于账号、slug、Token 哈希、重复提交、点赞收藏和待处理举报。
- 列表索引以等值筛选字段在前，排序字段和 ID 在后。
- 审核队列按审核状态、创建时间和 ID 建索引。
- 清理任务按过期/创建时间和 ID 建索引。
- 外键引用列均具有显式索引；复合外键列顺序与目标唯一索引一致。
- 不为低选择性布尔字段单独建索引，而与状态和排序字段组成业务索引。

## 7. 变更规则

- 当前 V1 尚未在共享环境执行，因此允许直接校正设计。
- V1 一旦进入共享环境即冻结，后续只能增加 V2、V3 等迁移。
- 不得通过手工 SQL 修改共享环境结构。
- 新增索引前必须给出目标查询及 EXPLAIN；删除索引前必须确认无代码和报表依赖。
