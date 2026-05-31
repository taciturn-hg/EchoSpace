# EchoSpace

## 技术栈

| 层 | 后端 (echo-server) | 前端 (echo-web) |
|---|---|---|
| 核心 | Spring Boot 3.5 + Java 17 | Vue 3.5 + TypeScript 6 |
| 构建 | Maven | Vite 8 |
| ORM | MyBatis-Plus 3.5.7 | — |
| 数据库 | MySQL 8 | — |
| 搜索 | Elasticsearch 8 (ik 分词) | — |
| 存储 | MinIO (@ConditionalOnProperty storage.type=minio) | — |
| 安全 | Spring Security + JWT (jjwt 0.12.6) | Axios 拦截器 (result.ts) |
| UI | — | Element Plus 2 + @lucide/vue |
| 富文本 | — | TipTap 3 + DOMPurify 3 |
| 状态 | — | Pinia 3 + Vue Router 5 |

## 项目结构

```
echo-server/                          echo-web/
├── controller/                       ├── api/
│   ├── AuthController.java           │   ├── posts.ts / users.ts / comments.ts
│   ├── UserController.java (/me)     │   └── modules/index.ts (类型定义)
│   ├── UserPublicController.java     ├── components/
│   ├── PostController.java           │   ├── PostCard.vue (自闭环交互)
│   ├── CommentController.java        │   ├── CommentThread.vue
│   ├── FileController.java           │   ├── CommentCard.vue / CommentCreate.vue
│   └── AdminController.java          │   ├── UserCard.vue / LayoutPage.vue
├── service/                          ├── composables/
│   └── impl/                         │   └── useInfiniteList.ts (双模式)
│       ├── PostServiceImpl.java      ├── views/
│       ├── UserServiceImpl.java      │   ├── HomePage.vue / PostDetail.vue
│       ├── CommentServiceImpl.java   │   ├── PostCreate.vue / UserProfile.vue
│       ├── AuthServiceImpl.java      │   ├── SearchPage.vue
│       ├── SearchServiceImpl.java    │   └── *Settings*.vue / ChangePassword*.vue
│       └── MinioFileServiceImpl.java ├── stores/userStore.ts
├── security/                         ├── utils/
│   ├── JwtAuthFilter.java            │   ├── result.ts (Axios + 401 刷新)
│   ├── SecurityConfig.java           │   ├── time.ts / number.ts
│   └── JwtUtil.java                  └── router/
├── mapper/ (*Mapper.java + *.xml)        └── index.ts (路由守卫)
└── common/
    ├── Result.java (统一响应包装)
    ├── BusinessException.java
    └── GlobalExceptionHandler.java
```

## 关键约定

### 依赖注入：必须用构造器注入

所有 Service / Controller / Security 类统一使用构造器注入，`final` 字段 + 显式构造函数。Spring 对单构造器自动注入，无需 `@Autowired`。

### 后端模式

- **Toggle 操作** (like/favorite/follow)：查已有 → 存在则删+count-1，不存在则插+count+1。必须加 `@Transactional`，insert 时 catch `DuplicateKeyException` 防并发。count+1 后 re-read 数据库拿真实值（不依赖 pre-mutation 对象计算 ±1）。
- **计数扣减**：SQL 用 `GREATEST(col - N, 0)` 防负值。
- **游标分页**：`buildCursorPage(records, size, sort)` 通用方法——查 size+1 条，hasMore=超过 size，裁剪后从 last record 构造 nextCursor。游标编码 `{timestamp}_{id}` (最新) 或 `{likeCount}_{id}` (热门)。
- **isLiked/isCollected**：列表 SQL 全部 LEFT JOIN user_like + user_favorite + `IF(NULL→FALSE)`，未登录传 userId=0。
- **HTML 清洗**：服务端 `Jsoup.clean(html, POST_SAFELIST)` — img src 仅 http/https，a href 仅 http/https/mailto/tel。
- **日志**：`@Slf4j` + `log.info("描述 key={}", val)`。敏感字段用 `MaskUtil.maskPhone/Email/Password/Token` 脱敏。
- **N+1 禁止**：批量操作用 `selectBatchIds` + Map，参考 `syncAllPostsToEs()`。
- **ES 同步**：发布/编辑时 try-catch 写 ES，失败不影响 MySQL 主流程。

### 前端模式

- **PostCard 自闭环**：点赞/收藏在组件内部调 API + 乐观更新 + 失败回滚。不要在父组件维护 likedPosts/collectedPosts Set（已清理）。PostCard emit 只有 `edit` / `delete`。
- **useInfiniteList**：泛型 `useInfiniteList<T>`，双模式 `cursor` / `page`，`fetchFn` 注入。
- **XSS 防护**：`v-html` 仅用于经 DOMPurify 消毒的内容。标题用文本插值 `{{ }}`。搜索高亮 `<em>` 通过 `:deep(em)` CSS 样式。
- **乐观更新**：操作前 snapshot prev → 本地直接改 → API 成功则同步服务端值 → 失败则回滚 prev。
- **replyLikes 本地覆盖**：`replyLikes: Record<number, {liked, likeCount}>`，首次 toggle 前必须从 reply prop 初始化基线，避免从 false/0 起步导致计数错乱。

## 常用命令

```bash
# 后端编译
cd echo-server && mvn compile -q

# 前端类型检查
cd echo-web && npx tsc --noEmit

# 运行（Spring Boot + Vite dev server）
# 后端：IDE 运行 EchoServerApplication
# 前端：cd echo-web && pnpm dev
```

## 文档索引

| 文档 | 用途 |
|------|------|
| `.claude/PROJECT_ARCHITECTURE.md` | 项目架构、Sprint 历史、文件索引 |
| `docs/02-api-documentation.md` | API 接口文档 |
| `docs/04-development-plan.md` | 开发计划与 Sprint 规划 |
| `docs/Question.md` | 开发问题记录与解决方案 |
| `.claude/PRPs/reviews/` | 代码审查报告 |

## 当前状态

- Sprint 1~6 已完成：认证 → 用户管理 → 帖子 → 评论 → 搜索 → 关注/个人主页
- 待完成：关注时间线、Redis 接入（忘记密码/重置密码）、消息通知（RabbitMQ）、Docker 部署、OSS 存储切换
