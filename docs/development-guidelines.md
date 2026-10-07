# 开发规范

本规范是代码评审和验收依据。若确需偏离，必须在 Pull Request 中说明原因、影响和替代方案。

## 1. 后端技术与构建

- 后端使用 Java 21、Spring Boot 3.x、Maven 和 MyBatis-Plus；前端使用 Vue 3、TypeScript、Nuxt 3 SSR 和 Pinia。
- 仓库提交 Maven Wrapper（`mvnw`、`mvnw.cmd`、`.mvn/`），构建和 CI 优先使用 Wrapper。
- 依赖及插件版本统一在父 `pom.xml` 的 `dependencyManagement`、`pluginManagement` 或属性中管理。
- 禁止提交本地仓库、IDE 缓存、编译产物、密钥和真实环境配置。

## 2. 后端目录和职责

建议按业务模块组织，每个模块内部保持以下分层；公共基础能力放在 `common`：

```text
backend/src/main/java/com/hootoom/forum/
├─ auth/
│  ├─ controller/
│  ├─ service/
│  │  └─ impl/
│  ├─ mapper/
│  ├─ entity/
│  ├─ dto/
│  └─ vo/
├─ post/
│  ├─ controller/
│  ├─ service/
│  │  └─ impl/
│  ├─ mapper/
│  ├─ entity/
│  ├─ dto/
│  └─ vo/
├─ security/
├─ config/
└─ common/

backend/src/main/resources/
├─ mapper/
│  ├─ auth/
│  └─ post/
└─ db/migration/
```

各层职责：

- `controller`：接收请求、参数校验、调用 Service、转换响应。不得编写业务判断、事务、SQL 或直接调用 Mapper。
- `service`：只存放 Service 接口，定义业务能力，不放实现类。
- `service/impl`：存放接口实现、业务规则、权限后的业务校验、状态流转、事务编排和多个 Mapper 的协作。
- `mapper`：只存放 Mapper 接口；方法名和参数应表达数据访问意图，不承担业务判断。
- `entity`：数据库实体，与表字段对应，不直接作为 API 请求或响应对象。
- `dto`：请求参数和跨层命令对象，使用 Bean Validation 声明边界校验。
- `vo`：返回给前端的视图对象，不暴露密码哈希、内部状态或其他敏感字段。
- `security`：认证、授权、Token、密码编码、当前主体和安全过滤器。
- `config`：Spring、MyBatis、CORS、序列化、OpenAPI 等配置。

## 3. Service 与事务规范

- 每个 Service 必须先定义接口，例如 `PostService`；实现位于 `service.impl.PostServiceImpl`。
- Controller 依赖 Service 接口，不依赖实现类；优先构造器注入，禁止字段注入。
- 业务逻辑、权限后的资源校验、审核规则、计数维护和状态流转统一放在 `ServiceImpl`。
- 事务边界放在 `ServiceImpl` 的公开业务方法上。只读查询使用只读事务；不得在 Controller 开启事务。
- `ServiceImpl` 不返回 Entity 给 Controller，应返回 VO 或由明确的转换器转换。
- 单个方法只处理一个清晰用例；过长逻辑应拆为私有业务步骤或独立领域服务，不能转移到 Controller 或 Mapper XML。
- 禁止 Service 之间形成循环依赖。

示例结构：

```java
public interface PostService {
    PostDetailVO createPost(CreatePostDTO command, CurrentUser currentUser);
}

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;

    @Override
    @Transactional
    public PostDetailVO createPost(CreatePostDTO command, CurrentUser currentUser) {
        // 校验、状态决策、持久化和结果组装
    }
}
```

## 4. Mapper 与 SQL XML 规范

- 所有项目数据读写 SQL 都必须写在 `src/main/resources/mapper/**/*.xml` 中。
- Mapper Java 接口只声明方法，不使用 `@Select`、`@Insert`、`@Update`、`@Delete`、`@SelectProvider` 等注解 SQL。
- XML 的 `namespace` 必须与 Mapper 接口全限定名一致，语句 `id` 必须与接口方法名一致。
- SQL XML 只负责查询和持久化，不包含审核、权限、状态决策等业务逻辑。
- 动态 SQL 的排序字段、表名和列名必须经过 Java 侧白名单映射，禁止 `${userInput}`。
- 普通参数使用 `#{}` 绑定；复杂返回显式声明 `resultMap`，避免依赖易变的隐式映射。
- 公共列清单可以使用 `<sql>` 片段复用，但避免跨文件形成难以追踪的嵌套引用。
- 分页、联表和统计查询必须考虑索引；复杂 SQL 需在代码评审中附 `EXPLAIN` 结果。
- 禁止业务代码调用 `BaseMapper` 内置 CRUD、`IService`、`ServiceImpl` 基类或 `QueryWrapper`/`LambdaQueryWrapper` 构造查询。MyBatis-Plus 仅用于分页、乐观锁、字段填充和经过批准的基础插件；所有 Mapper 方法必须显式声明并由 XML 实现。
- XML 中不得拼接权限规则来替代 Service 授权；数据范围条件由 Service 明确传入 Mapper。

## 5. 后端通用约束

- 使用统一异常处理器和统一响应结构，不在每个 Controller 重复 `try/catch`。
- Entity、DTO、VO 相互分离；转换逻辑集中在 converter/assembler 或 ServiceImpl，不散落在 Controller。
- Controller 路径、HTTP 方法、错误码、分页和时间格式遵守 [api-conventions.md](api-conventions.md)。
- 包名、类名和方法名使用领域含义，禁止 `CommonService`、`Utils2` 等模糊命名。
- 核心 ServiceImpl、Mapper XML 和权限场景必须具有自动化测试。
- 后端统一使用 SLF4J 门面和 Logback 实现，禁止使用 `System.out`、`System.err` 或直接打印异常；具体规则见 [logging-observability.md](logging-observability.md)。

## 6. 前端目录和分层

前端不得把页面、路由、状态、请求和样式全部写进 `index.html`、`main.ts`、`App.vue` 或单个页面组件。建议结构：

```text
frontend/src/
├─ api/             # HTTP 客户端和按领域拆分的接口函数
├─ assets/          # 字体、图片和全局静态资源
├─ components/      # 可复用展示组件
│  ├─ common/
│  └─ forum/
├─ composables/     # 可复用组合式逻辑
├─ layouts/         # 前台、用户中心、后台布局
├─ pages/           # 路由页面，负责页面编排
│  ├─ home/
│  ├─ category/
│  ├─ post/
│  ├─ user/
│  └─ admin/
├─ router/          # 路由表、守卫和懒加载
├─ stores/          # Pinia 状态；按领域拆分
├─ types/           # API 与领域类型
├─ utils/           # 无状态工具函数
├─ styles/          # 设计令牌、重置和全局样式
├─ app.vue          # 根布局出口，不承载页面业务
├─ nuxt.config.ts   # SSR、运行时配置和模块配置
└─ server/          # Nuxt 服务端中间层；不得复制 Spring 业务逻辑
```

分层规则：

- `pages` 负责路由级数据获取和组件编排，不堆积可复用 UI 或复杂业务逻辑。
- `components` 通过 props、events 和 slots 通信，不直接依赖具体路由页面。
- `api` 是唯一 HTTP 访问入口；组件不得直接散写 `fetch` 或 Axios 请求。
- 跨页面状态放入按领域拆分的 Pinia store；局部状态保留在组件或 composable，避免全局化。
- 可复用逻辑放入 composables；纯函数放入 utils；类型集中管理，禁止到处复制接口结构。
- 路由页面和重量组件使用动态导入；前台与后台代码包分离。
- 样式使用设计令牌和组件作用域，禁止在入口文件堆放全部样式。
- `app.vue` 只提供根布局和页面出口，不堆放页面业务；Nuxt 自动生成 HTML 入口，不维护业务化 `index.html`。

## 7. SEO 规范

论坛首页、板块页和帖子详情页是可索引公开页面，使用 Nuxt 3 SSR；搜索结果页可以访问但统一 `noindex,follow`。登录、发帖、用户中心和后台可仅客户端渲染。

### 页面输出

- 首次 HTTP 响应必须包含页面主体文本和有效链接，不能依赖爬虫执行 JavaScript 后才出现正文。
- 每页生成唯一且贴合内容的 `<title>` 和 `meta description`。
- 输出绝对地址 `rel="canonical"`，统一协议、域名、尾斜杠和分页 URL 规则。
- 输出 Open Graph 基础字段；需要社交分享时补充 Twitter Card。
- HTML 只保留一个语义明确的 `h1`，标题层级连续，导航和正文使用语义化元素。
- 图片包含有意义的 `alt`、明确尺寸和懒加载策略，首屏主图避免延迟加载。
- 面包屑、帖子等适用页面输出符合 schema.org 的 JSON-LD，结构化数据必须与可见内容一致。

### 索引策略

- 首页、正常板块、已通过且未删除的帖子允许索引。
- 登录、注册、用户中心、发帖、后台、站内搜索结果、待审、驳回、删除及预览页面设置 `noindex`。
- 提供 `/robots.txt`，只控制抓取范围，不用于保护敏感内容；敏感页面仍必须鉴权。
- 提供 `/sitemap.xml` 或 sitemap 索引，包含公开板块和帖子；内容发布、更新、删除后及时更新 `lastmod` 和收录状态。
- 不允许索引的内容应返回正确的 401/403/404/410 或 `noindex`，不能只在页面中显示“无权限”。
- 分页页面应有稳定 URL；筛选和排序参数通过 canonical 或 noindex 避免重复内容。
- 帖子规范 URL 使用 `/t/{postId}/{slug}`；`postId` 是稳定身份，标题变化后旧 slug 以 301 跳转到最新规范 URL。

### 性能与可访问性

- 以 Core Web Vitals 为目标：LCP ≤ 2.5 秒、INP ≤ 200 毫秒、CLS ≤ 0.1（第 75 百分位）。
- 路由级拆包、压缩资源、缓存静态资产，避免阻塞渲染的大型依赖。
- 页面在禁用 JavaScript 时仍能读取公开核心内容和导航。
- SEO 不得通过隐藏关键词、重复标题或为爬虫返回不同内容来实现。

## 8. 代码评审清单

- Controller 是否只做协议适配和校验，并且仅依赖 Service 接口？
- Service 是否只有接口，业务实现是否位于 `service/impl`？
- 业务规则和事务是否集中在 ServiceImpl？
- Mapper 是否无注解 SQL，所有 SQL 是否位于对应 XML？
- SQL 是否参数化、可使用索引且不包含业务决策？
- 前端是否按页面、组件、API、状态和组合逻辑拆分？
- Nuxt `app.vue`、布局和插件是否保持轻量，是否避免把业务堆进入口？

## 9. 依赖与许可证

- Maven 依赖集中管理；前端统一使用 Corepack 管理的 pnpm 并提交 `pnpm-lock.yaml`，禁止混入 npm/yarn 锁文件。依赖升级通过自动化 PR 或每月维护窗口完成。
- CI 执行依赖漏洞、许可证和密钥泄露扫描，并生成 SBOM。
- 禁止引入与商业用途不兼容的代码、字体、图片或图标；素材来源和许可证记录在仓库中。
- Markdown 渲染、HTML 清洗、认证和加密库属于安全关键依赖，严重漏洞修复不得等待常规发布周期。
- 公开页面是否 SSR，并具有正确元数据、canonical、索引策略和语义化 HTML？
- 新增行为是否同步 OpenAPI、测试和相关文档？
- 关键流程、异常出口和外部调用是否具有可关联且已脱敏的日志？
