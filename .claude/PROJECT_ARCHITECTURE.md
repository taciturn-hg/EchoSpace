# EchoSpace 项目架构与历史

> 本文档记录项目完整架构设计、Sprint 开发历史、模块详情和关键文件索引。
> 日常开发用 `CLAUDE.md`（概要+约定），本文档作深度参考。

---

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                      echo-web (Vue 3)                   │
│  LoginPage  RegisterPage  HomePage  PostDetail  ...     │
│  ─────────────────────────────────────────────────────  │
│  Pinia Store │ Vue Router │ Axios (result.ts)           │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP / REST
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  echo-server (Spring Boot)               │
│                                                         │
│  Controller Layer                                       │
│    AuthController  (register / login / refresh / me)    │
│    UserController (profile GET/PUT, settings GET/PUT, password PUT) │
│    UserPublicController (GET /users/{id} + /users/{id}/posts + follow + followers + following) │
│    FileController (avatar/image upload + delete)         │
│    PostController (posts CRUD + cursor pagination + like/favorite/search) │
│    CommentController (comments CRUD + like + replies)    │
│                                                         │
│  Service Layer                                          │
│    AuthServiceImpl                                      │
│    UserServiceImpl (profile / settings / password / follow / followers / following) │
│    FileService → MinioFileServiceImpl (storage.type=minio) │
│    PostServiceImpl (CRUD + 游标分页 + Jsoup + ES同步)    │
│    SearchServiceImpl (全文检索 + ik 分词 + 高亮 + 分页)   │
│    CommentServiceImpl (CRUD + 游标分页 + 级联软删除 + 点赞 toggle) │
│                                                         │
│  Security Layer                                         │
│    JwtAuthFilter → JwtUtil → SecurityUtil               │
│    SecurityConfig（白名单 + 无状态 Session）             │
│                                                         │
│  Data Layer                                             │
│    MyBatis-Plus Mapper → MySQL                          │
│    Elasticsearch (帖子索引 posts，ik 分词，同步写入)      │
│    [Redis — 待接入]                                     │
│    MinIO (头像/图片上传，storage.type=minio)             │
│    [OSS — 待接入]                                       │
└─────────────────────────────────────────────────────────┘
```

### 认证流程

```
登录 → 返回 accessToken（30min）+ refreshToken（7d）
       ↓
前端存入 Pinia（userStore）
       ↓
每次请求 Header: Authorization: Bearer <accessToken>
       ↓
accessToken 过期 → result.ts 拦截 401 → 自动调用 /auth/refresh
       ↓
refreshToken 过期 → 清除 store → 跳转 /login
```

---

## 数据库实体

| 实体         | 说明 |
| ------------ | ---- |
| User         | 用户（id, username, nickname, email, phone, password, avatar, bio, status） |
| Post         | 帖子（id, userId, title, contentHtml, contentText, cover, likeCount, collectCount, commentCount, viewCount, status, version） |
| Comment      | 评论（id, postId, userId, parentId, replyToUid, content, likeCount, status）— parentId=0 一级，非0 二级 |
| UserFavorite | 收藏关系（userId + postId） |
| UserFollow   | 关注关系（followerId + followedId） |
| UserLike     | 点赞关系（targetType: 1=帖子 2=评论 3=回复） |

---

## 页面清单

| 页面                    | 路由                        | 状态      |
| ----------------------- | --------------------------- | --------- |
| HomePage.vue            | `/`                         | 已完成    |
| PostDetail.vue          | `/post/:id`                 | 已完成    |
| PostCreate.vue          | `/post/create`              | 已完成    |
| SearchPage.vue          | `/search`                   | 已完成    |
| UserProfile.vue         | `/user/:id`                 | 已完成    |
| ProfileSettingsPage.vue | `/settings/profile`         | 已完成    |
| SettingsPage.vue        | `/settings`                 | 已完成    |
| ChangePasswordPage.vue  | `/settings/change-password` | 已完成    |
| ForgotPassword.vue      | `/forgot-password`          | 占位      |
| ResetPassword.vue       | `/reset-password`           | 占位      |

---

## Sprint 开发历史

### Sprint 1 — 认证基础（已完成，PR #3 已合并）

- [x] 用户注册/登录（BCrypt 加密，三合一登录）
- [x] JWT 双 Token（accessToken/refreshToken）
- [x] 登录页/注册页 + 路由守卫
- [x] Spring Security 白名单 + JwtAuthFilter
- [x] Token 自动刷新（result.ts 拦截 401 + 请求队列锁）

### Sprint 2 — 用户认证与账号管理（已完成，已合并）

- [x] LayoutPage 布局（顶栏 + 可折叠侧边栏 + 搜索框 + 用户卡片 + 下拉菜单）
- [x] ProfileSettingsPage（头像上传/昵称/简介编辑）
- [x] SettingsPage（手机号/邮箱编辑 + el-form rules 校验）
- [x] ChangePasswordPage（旧密码/新密码/确认密码 + BCrypt）
- [x] 文件上传（MinioFileServiceImpl + 扩展名白名单 + 大小校验 + 内容类型防 XSS）
- [x] GlobalExceptionHandler（DuplicateKeyException→409, MissingServletRequestPartException→400）
- [x] MaskUtil 敏感数据脱敏 + @Slf4j 日志规范
- [x] 后端接口 2.2~2.6（资料 GET/PUT、账号 GET/PUT、改密 PUT）

### Sprint 3 — 帖子核心（已完成，已合并）

- [x] PostCard 组件（用户区/标题摘要/图片网格/交互区/图片预览 + 点赞/收藏自闭环）
- [x] HomePage 帖子列表（v-infinite-scroll 无限滚动 + useInfiniteList 泛型双模式）
- [x] PostController 7→8 端点（CRUD + 点赞 toggle + 收藏 toggle + ES 搜索）
- [x] PostService/PostServiceImpl（Jsoup HTML 清洗 + 游标编解码 + 乐观锁 + ES 同步）
- [x] PostDetail（淡入动画 + DOMPurify 消毒 + 评论区无限滚动）
- [x] PostCreate（TipTap 富文本 + Toolbar + 图片上传 + Link 协议白名单）
- [x] 点赞/收藏 toggle（乐观更新 + 回滚 + isLiked/isCollected LEFT JOIN）
- [x] Jsoup.clean Safelist（img src 仅 http/https, a href 仅 http/https/mailto/tel）

### Sprint 4 — 评论 CRUD + 评论点赞（已完成）

- [x] CommentController 5 端点（发表/列表游标分页/二级回复/点赞 toggle/软删除）
- [x] 评论发表校验 + 级联软删除 + CommentMapper 6 自定义 SQL
- [x] 前端评论组件（CommentCard/CommentThread/CommentCreate）
- [x] 评论点赞 toggle + LikeCommentVO（仅返回 liked，前端本地 ±1）

### Sprint 4 补充 — 关注/粉丝 + isLiked/isCollected 补全

- [x] 关注/取消关注接口：POST /api/users/{id}/follow（toggle，禁止自关注）
- [x] 粉丝列表/关注列表（MyBatis-Plus 页码分页，未登录可访问）
- [x] 帖子列表 isLiked/isCollected 4 条 SQL 全部 LEFT JOIN 补全
- [x] 测试数据 data.sql（38 评论 + 99 点赞 + 25 收藏 + 18 关注）

### Sprint 5 — 搜索（已完成，已合并）

- [x] Elasticsearch 8.x 全文检索 + ik 分词 + 高亮 `<em>`
- [x] SearchService/SearchServiceImpl + SearchPage.vue
- [x] PostServiceImpl ES 同步写入（try-catch 兜底，不影响主流程）
- [x] AdminController POST /api/admin/sync-es 全量同步

### Sprint 6 — 关注 + 个人主页（已完成，当前分支 feature/user-social）

- [x] UserPublicController（GET /users/{id}, /users/{id}/posts, /follow, /followers, /following）
- [x] 前端 UserProfile.vue + 关注/取消关注 + 粉丝/关注弹窗
- [x] 评论点赞 CommentController 端点
- [x] PR #12 代码审查修复（构造器注入全线改造、N+1 批量优化、竞态条件防护、XSS 修复）

---

## TODO

| 优先级 | 任务 |
|--------|------|
| 近期 | 引入 Redis，实现忘记密码/重置密码 |
| 中期 | 关注时间线、消息通知（RabbitMQ） |
| 远期 | OSS 存储切换、404 页面、短信验证码、Docker 部署 |

---

## 开发规范

- **日志**：@Slf4j + `log.info("描述 key={}", val)`，敏感数据用 MaskUtil 脱敏
- **依赖注入**：统一构造器注入（final 字段），禁止 @Autowired 字段注入
- **Toggle 操作**：selectOne → 存在删+count-1 / 不存在插+count+1，加 @Transactional + catch DuplicateKeyException
- **计数扣减**：SQL 用 `GREATEST(col - N, 0)` 防负值
- **N+1 禁止**：批量用 `selectBatchIds` + Map
- **XSS 防护**：后端 Jsoup.clean Safelist 白名单，前端 v-html 仅用于已消毒内容，标题用文本插值

---

## 环境变量

| 变量名                  | 用途               |
| ----------------------- | ------------------ |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 凭证     |
| `JWT_SECRET`            | JWT 签名密钥       |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO 凭证 |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS |

---

## 关键文件索引

| 文件 | 说明 |
|------|------|
| `echo-server/.../security/SecurityConfig.java` | Security 配置 |
| `echo-server/.../security/JwtAuthFilter.java` | JWT 过滤器 |
| `echo-server/.../security/JwtUtil.java` | Token 工具 |
| `echo-server/.../controller/AuthController.java` | 认证 |
| `echo-server/.../controller/UserController.java` | 用户自我管理 |
| `echo-server/.../controller/UserPublicController.java` | 用户公开信息 |
| `echo-server/.../controller/PostController.java` | 帖子（8 端点） |
| `echo-server/.../controller/CommentController.java` | 评论（5 端点） |
| `echo-server/.../controller/FileController.java` | 文件上传 |
| `echo-server/.../controller/AdminController.java` | 管理工具 |
| `echo-server/.../service/impl/PostServiceImpl.java` | 帖子核心逻辑 |
| `echo-server/.../service/impl/UserServiceImpl.java` | 用户核心逻辑 |
| `echo-server/.../service/impl/CommentServiceImpl.java` | 评论核心逻辑 |
| `echo-server/.../service/impl/AuthServiceImpl.java` | 认证逻辑 |
| `echo-server/.../service/impl/SearchServiceImpl.java` | ES 搜索 |
| `echo-server/.../service/impl/MinioFileServiceImpl.java` | MinIO 存储 |
| `echo-server/.../mapper/PostMapper.java` + `.xml` | 帖子 SQL |
| `echo-server/.../mapper/CommentMapper.java` + `.xml` | 评论 SQL |
| `echo-server/.../mapper/UserMapper.java` | 用户 Mapper |
| `echo-server/.../mapper/UserLikeMapper.java` | 点赞 Mapper |
| `echo-server/.../mapper/UserFavoriteMapper.java` | 收藏 Mapper |
| `echo-server/.../mapper/UserFollowMapper.java` | 关注 Mapper |
| `echo-server/.../common/Result.java` | 统一响应 |
| `echo-server/.../common/BusinessException.java` | 业务异常 |
| `echo-server/.../common/GlobalExceptionHandler.java` | 异常处理 |
| `echo-server/.../util/MaskUtil.java` | 脱敏工具 |
| `echo-server/.../document/PostDocument.java` | ES 文档 |
| `echo-server/src/main/resources/db/init.sql` / `data.sql` | 数据库 |
| `echo-web/src/api/posts.ts` / `users.ts` / `comments.ts` | 前端 API |
| `echo-web/src/api/modules/index.ts` | 前端类型 |
| `echo-web/src/composables/useInfiniteList.ts` | 无限列表 |
| `echo-web/src/utils/result.ts` | Axios 封装 |
| `echo-web/src/utils/time.ts` / `number.ts` | 工具函数 |
| `echo-web/src/stores/userStore.ts` | 用户状态 |
| `echo-web/src/router/index.ts` | 路由守卫 |
| `echo-web/src/components/LayoutPage.vue` | 主布局 |
| `echo-web/src/components/PostCard.vue` | 帖子卡片 |
| `echo-web/src/components/CommentThread.vue` | 评论线程 |
| `echo-web/src/components/CommentCard.vue` / `CommentCreate.vue` | 评论子组件 |
| `echo-web/src/components/UserCard.vue` | 用户卡片 |
| `echo-web/src/views/*.vue` | 页面组件（7 已完成 + 2 占位） |
| `docs/02-api-documentation.md` | API 文档 |
| `docs/04-development-plan.md` | 开发计划 |
| `docs/Question.md` | 问题记录 |
| `docs/roadmap.md` | 待开发方案 |
