# HOOTOOM Forum 前端

前端使用 Nuxt 3、Vue 3、TypeScript、Pinia 和 pnpm。公开页面默认服务端渲染；登录、注册、密码处理和用户中心页面按开发规范禁用 SSR，并统一设置 `noindex`。

## 本地启动

要求 Node.js 24，建议启用 Corepack。后端默认运行在 `http://localhost:8080`。

```powershell
cd frontend
corepack enable
pnpm install
pnpm dev
```

浏览器访问 `http://localhost:3000`。可通过以下环境变量覆盖默认配置：

- `NUXT_PUBLIC_API_BASE`：后端 API 根地址，默认 `http://localhost:8080/api/v1`。
- `NUXT_PUBLIC_SITE_URL`：站点规范地址，用于 canonical、robots 和 sitemap。
- `NUXT_PUBLIC_POLICY_VERSION`：注册时提交的当前协议版本，必须与后端一致。

## 目录职责

```text
src/
├─ api/          # 按领域封装的唯一 HTTP 访问入口
├─ components/   # 通用组件和领域组件
├─ composables/  # 认证、请求和 SEO 等可复用逻辑
├─ layouts/      # 默认布局与认证布局
├─ middleware/   # 路由访问控制
├─ pages/        # 路由页面，只负责编排与页面局部状态
├─ server/       # robots、sitemap 等 Nuxt 服务端路由
├─ stores/       # Pinia 跨页面状态
├─ styles/       # 设计令牌和全局样式
├─ types/        # API 与领域类型
└─ utils/        # 无状态工具函数
```

## 认证与安全约束

- Access Token 仅保存在 Pinia 内存中，不写入 localStorage、sessionStorage 或可持久化 Cookie。
- Refresh Token 由后端写入 HttpOnly Cookie；前端请求统一使用 `credentials: include`。
- 刷新与退出请求从 `HOOTOOM_XSRF` Cookie 读取值并发送 `X-CSRF-Token` 请求头。
- API 客户端对受保护接口的 401 只执行一次并发合并刷新，刷新成功后重试原请求。
- 页面和组件不得自行调用 `$fetch`；新增请求应先放入 `src/api`。

## 质量检查

```powershell
pnpm typecheck
pnpm lint
pnpm test
pnpm build
```

提交前四项均须通过。公开板块和帖子页面后续实现时必须保留 SSR，并补充唯一标题、描述、canonical、Open Graph、结构化数据和 sitemap 数据源。
