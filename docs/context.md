# EchoSpace 项目上下文

> 本文件记录项目当前状态，供开发过程中快速定位上下文使用。随开发进展持续更新。

---

## 技术栈

### 后端（echo-server）

| 层次     | 技术                                                   |
| -------- | ------------------------------------------------------ |
| 框架     | Spring Boot 3.5.x，Java 17                             |
| 安全     | Spring Security + JWT（jjwt 0.12.6）                   |
| ORM      | MyBatis-Plus 3.5.7                                     |
| 数据库   | MySQL 8.x                                              |
| 缓存     | Redis（已引入依赖，暂未接入）                          |
| 搜索     | Elasticsearch 8.x（已接入，全文检索 + ik 分词 + 高亮） |
| 存储     | MinIO / 阿里云 OSS（已引入依赖，暂未接入）             |
| 邮件     | Spring Mail + Gmail SMTP                               |
| 消息队列 | RabbitMQ（已引入依赖，暂未接入）                       |
| 文档     | SpringDoc OpenAPI 2.x（Swagger UI）                    |
| 构建     | Maven                                                  |

### 前端（echo-web）

| 层次         | 技术                                  |
| ------------ | ------------------------------------- |
| 框架         | Vue 3.5.x + TypeScript 6.x            |
| 构建         | Vite 8.x                              |
| UI 组件库    | Element Plus 2.x                      |
| 状态管理     | Pinia 3.x                             |
| 路由         | Vue Router 5.x                        |
| HTTP         | Axios 1.x（封装于 `utils/result.ts`） |
| 富文本编辑器 | TipTap 3.x                            |
| HTML 净化    | DOMPurify 3.x                         |
| 图标         | @lucide/vue（Lucide Icons）           |
| 样式         | SCSS（Sass）                          |
| 代码规范     | ESLint + Prettier + Oxlint            |

---

## 架构设计草稿

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
│    UserPublicController (GET /users/{id} + GET /users/{id}/posts + POST /users/{id}/follow + GET /users/{id}/followers + /following) |│
│    FileController (avatar/image upload)                  │
│    PostController (posts CRUD + cursor pagination + like/favorite/search)       │
│    CommentController (posts/{postId}/comments CRUD + /comments/{id}/like + /comments/{id}/replies + /comments/{id} DELETE) │
│                                                         │
│  Service Layer                                          │
│    AuthServiceImpl                                      │
│    UserServiceImpl (profile / settings / changePassword / follow / followers / following) │
│    FileService → MinioFileServiceImpl (storage.type=minio) │
│    PostServiceImpl (posts CRUD + 游标分页 + Jsoup + ES同步) │
│    SearchServiceImpl (全文检索 + ik 分词 + 高亮 + 分页)    │
│    CommentServiceImpl (评论 CRUD + 游标分页 + 级联软删除 + 点赞 toggle)   │
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

## 模块规划

### 已完成模块

#### 认证模块（auth）

- [x] 用户注册（用户名 + 手机号 + 邮箱 + 密码，BCrypt 加密）
- [x] 用户登录（支持用户名 / 邮箱 / 手机号三合一）
- [x] JWT 双 Token 机制（accessToken + refreshToken）
- [x] Token 自动刷新（result.ts 拦截 401 + 请求队列锁）
- [x] 路由守卫（登录保护 + guest 页面重定向）
- [x] 登录页（LoginPage.vue）
- [x] 注册页（RegisterPage.vue）

#### 安全配置

- [x] Spring Security 白名单（`/auth/login`、`/auth/register`、`/auth/refresh` 等）
- [x] JwtAuthFilter（`getServletPath()` 匹配白名单，无 `/api` 前缀）
- [x] UserPrincipal（存储 userId + username，从 SecurityContext 取用）

#### 文件上传模块（upload）

- [x] FileService 接口（uploadAvatar / uploadImage）
- [x] MinioFileServiceImpl（`@ConditionalOnProperty(name="storage.type", havingValue="minio")`）
- [x] FileController（POST /api/upload/avatar + POST /api/upload/image，需认证）
- [x] MinioProperties 配置绑定 + MinioConfig 条件注入 MinioClient Bean
- [x] `storage.type` 配置预留 MinIO/OSS 切换（当前值 minio）
- [x] 前端 uploadAvatar / uploadImage API 函数 + ProfileSettingsPage 头像上传逻辑已放开

#### 评论后端模块（comment）— Sprint 4

- [x] CommentController（POST/GET /posts/{postId}/comments + GET /comments/{commentId}/replies + POST /comments/{id}/like + DELETE /comments/{id}）
- [x] CommentService / CommentServiceImpl（发表校验/游标分页/预加载回复/级联软删除/评论点赞 toggle/评论数同步）
- [x] CommentMapper + CommentMapper.xml（6 自定义 SQL：游标分页一级评论 + 预加载回复 + 回复统计 + 二级回复页码分页 + 点赞数增减）
- [x] PostMapper 新增 incrementCommentCount/decrementCommentCount（GREATEST 防负值）
- [x] CreateCommentDTO / CommentVO / ReplyVO / CommentUserVO / ReplyToUserVO / CreateCommentVO

### 页面占位（已建文件，内容待开发）

| 页面                    | 路由                        | 状态                                                                                                                |
| ----------------------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| ForgotPassword.vue      | `/forgot-password`          | 占位，待 Redis 接入后开发                                                                                           |
| ResetPassword.vue       | `/reset-password`           | 占位，待 Redis 接入后开发                                                                                           |
| HomePage.vue            | `/`                         | 已完成（v-infinite-scroll 无限滚动 + useInfiniteList，游标分页已接入后端）                                          |
| PostCard.vue            | —                           | 已完成（卡片组件：用户区/帖子区/图片网格/交互区/图片预览）                                                          |
| PostDetail.vue          | `/post/:id`                 | 已完成（淡入动画，作者区+关注+DOMPurify 净化+交互，评论区无限滚动）                                                 |
| PostCreate.vue          | `/post/create`              | 已完成（TipTap 富文本 + Toolbar + 图片上传，详见 Sprint 3）                                                         |
| SearchPage.vue          | `/search`                   | 已完成（极简现代风/返回按钮渐变+平滑/骨架屏/空状态/无限滚动/v-html 高亮，复用 PostCard + useInfiniteList 页码模式） |
| UserProfile.vue         | `/user/:id`                 | 已完成（用户信息卡片+关注/取消关注+帖子 tabs 游标分页+粉丝/关注弹窗页码分页，PostCard 点赞/收藏状态由后端 isLiked/isCollected 驱动）|
| ProfileSettingsPage.vue | `/settings/profile`         | 已完成（头像上传/昵称/简介编辑+保存/取消）                                                                          |
| SettingsPage.vue        | `/settings`                 | 已完成（手机号/邮箱编辑+保存/取消，el-form rules 校验）                                                             |
| ChangePasswordPage.vue  | `/settings/change-password` | 已完成（el-form rules 校验，旧密码/新密码/确认密码，show-password 切换，修改密码/清空按钮，忘记密码链接）           |

### 数据库实体（已建 Entity）

| 实体         | 说明                                                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| User         | 用户基础信息（id, username, nickname, email, phone, password, avatar, bio, status）                                             |
| Post         | 帖子（id, authorId, title, contentHtml, contentText, cover, likeCount, collectCount, commentCount, viewCount, status, version） |
| Comment      | 评论（id, postId, userId, parentId, replyToUid, content, likeCount, status）——parentId=0 一级评论，非0 二级回复                 |
| UserFavorite | 收藏关系（userId + postId，toggle 模式已激活）                                                                                  |
| UserFollow   | 关注关系（followerId + followedId，toggle 模式已激活）                                                                                                              |
| UserLike     | 点赞关系（targetType 1=帖子 2=评论 3=回复）                                                                                     |

---

## 当前进度

**Sprint 1 — 认证基础（已完成，已合并至 dev）**

- 后端：注册、登录、刷新 Token、获取当前用户信息
- 前端：登录页、注册页、路由守卫、Token 自动刷新
- feature/login-register → dev（PR #3 已合并）

**Sprint 2 — 用户认证与账号管理（已完成，已合并至 dev）**

- 相关分支：feature/user-settings（已合并）、feature/file-upload（安全加固，已合并）
- 完成了 LayoutPage.vue 布局系统：固定顶栏、可折叠左侧导航、搜索框、用户卡片、发布帖子按钮、下拉菜单（资料设置/账号设置/修改密码/退出登录）
- 新增 ProfileSettingsPage.vue 完整实现：头像上传（前端校验，上传接口 TODO）、昵称编辑、个人简介编辑（Element Plus textarea，6行固定高度）、取消按钮（重新拉取服务器数据覆盖本地，失败时提示"还原失败"）、保存按钮（绿色渐变，仅脏数据可点击）
- 新增 ChangePasswordPage.vue 完整实现：旧密码/新密码/确认密码三输入（show-password 切换），el-form rules 校验（必填、长度 6~20、新旧密码不同、两次输入一致），修改密码按钮 PUT /api/users/me/password 后端返回消息提示，清空按钮重置表单与校验状态，按钮行左侧"忘记密码？"链接（当前禁用态，待 Redis 接入后启用），绿色渐变按钮仅脏数据可点击
- 新增 SettingsPage.vue 完整实现：手机号/邮箱编辑（el-form rules 必填 + 格式校验），取消回拉服务器数据（失败时提示"还原失败"），保存 PUT /api/users/me/settings 并同步 Pinia store
- 前端 API 层新增 `echo-web/src/api/users.ts`（getProfile / updateProfile / getSettings / updateSettings / changePassword / uploadAvatar / uploadImage），类型新增 UserProfileVO / UpdateProfileDTO / UserSettingsVO / UpdateSettingsDTO / ChangePasswordDTO / UploadAvatarVO / UploadImageVO
- 后端 FileService 接口 + MinioFileServiceImpl 实现文件上传（头像 + 通用图片），MinioConfig 按 `storage.type` 条件注入 MinioClient Bean，application.yaml 新增 `storage.type: minio` 预留 OSS 切换
- FileController（POST /api/upload/avatar + /api/upload/image）接入，前端 ProfileSettingsPage 头像上传 TODO 已放开
- 后端 UserSettingsVO 取消手机号/邮箱脱敏，直接返回原始值（前端展示用，与 /api/auth/me 保持一致）
- 用户表 nickname 字段改为可为空，前端 LayoutPage/ProfileSettingsPage 已适配：昵称为空时展示 username
- 接口文档 02-api-documentation.md 已更新：文件上传模块 2 个通用接口（/api/upload/image + /api/upload/avatar），底层由 FileService 按 storage.type 切换 MinIO/OSS 实现；nickname 响应字段标注"非必须（可为空）"；附录接口总数 31
- 开发计划 04-development-plan.md 已同步：文件上传接口在 Sprint 2（累计接口数 9→12），OSS 存储实现（OssFileServiceImpl）留在 Sprint 3，全景图接口数联级更新（最终 31）
- 功能流程图 05-feature-flows.md 已更新：新增 2.4 文件上传流程，修正 MinIO/OSS SDK 兼容性说明（OSS 官方 SDK 非 S3 协议，需 FileService 接口抽象切换）
- 需求文档 01-requirements-and-plan.md 已修正：图片存储阶段策略中 MinIO→OSS 切换方案由"改配置"改为"FileService 接口 + OssFileServiceImpl 实现"
- 系统设计文档已产出：`docs/《EchoSpace》系统设计.md` + `.docx`，含系统架构、六大功能模块设计、E-R 图、6 张数据库表定义、设计要点总结
- 后端已实现接口 2.2~2.6（资料设置 GET/PUT、账号设置 GET/PUT、修改密码 PUT），编译通过
- 后端 Controller / Service 已添加 @Slf4j 业务日志，logback.xml 日志配置已就绪（控制台 + 滚动文件输出到 logs/）
- 忘记密码 / 重置密码流程已规划，等待 Redis 接入后实现
- 新增 `MaskUtil` 工具类（`echo-server/.../util/MaskUtil.java`）：手机号/邮箱/密码/Token/登录账号统一脱敏，Controller/ServiceImpl 日志中 8 处敏感数据已接入
- GlobalExceptionHandler 新增 `DuplicateKeyException` → 409 Conflict 全局映射，数据库唯一键并发冲突不再返回 500
- UserServiceImpl.updateProfile 增加空更新保护（与 updateSettings 一致），避免全 null DTO 触发无 SET 列的 SQL 异常
- ProfileSettingsPage 头像交互重构："选择图片"按钮仅做本地预览（URL.createObjectURL），点击保存时统一上传头像 + 更新资料
- UserService/UserController 移除残余"脱敏"Javadoc 注释
- **文件上传安全加固（feature/file-upload）**：
  - MinioFileServiceImpl 新增扩展名白名单校验（.jpg/.jpeg/.png），防止路径遍历和恶意文件上传
  - MinioFileServiceImpl 新增文件大小上限校验（MAX_FILE_SIZE = 2MB），业务层 + Servlet 层双重防护
  - MinioFileServiceImpl 新增 `resolveContentType()` 由服务端根据已验证扩展名推导 MIME 类型，不再信任客户端声明的 Content-Type，防止伪造为 text/html 导致 XSS
  - MinioFileServiceImpl upload() 用 try-with-resources 包裹 InputStream，确保流显式关闭
  - MinioFileServiceImpl 新增 normalizeEndpoint() 去除 endpoint 尾部斜杠 + trim，避免双斜杠 URL
  - 前端 uploadAvatar/uploadImage 删除手动 Content-Type: multipart/form-data，让 Axios 自动设置 boundary
  - MinioProperties 关键字段（endpoint/accessKey/secretKey/bucketName）加 @NotBlank(message=...) 启动期强校验，fail-fast；配置绑定从类级别 @ConfigurationProperties 移到 MinioConfig @Bean 方法级别，仅在 storage.type=minio 时注册校验
  - MinioProperties 新增 publicBaseUrl 可选字段：SDK 直连用 endpoint，对外返回链接用 publicBaseUrl（为空时回退），分离内网与公网访问地址
  - GlobalExceptionHandler 新增 MissingServletRequestPartException → 400 + MultipartException → 400 映射
  - FileService 新增 deleteFile(String url) 接口 + MinioFileServiceImpl 实现（解析 URL → removeObject）+ FileController DELETE /api/upload/file 端点（需认证，URL 白名单校验 bucket 匹配）
  - ProfileSettingsPage handleSave 新增回滚清理：updateProfile 失败时自动调 deleteFile 清理已上传头像（best-effort，清理失败不影响错误提示）
  - 前端 users.ts 新增 deleteFile API 函数
  - 接口文档 02-api-documentation.md 新增 5.3 删除文件接口，附录接口总数更新为 31
  - 开发问题记录 Question.md 新增问题 12~20（Content-Type/boundary、扩展名白名单、文件大小校验、contentType NPE、InputStream 关闭、Content-Type 伪造/XSS、MinIO 配置校验/条件注册、MissingServletRequestPartException、手工拼接 URL、删除回滚）

**Sprint 3 — 帖子核心（已完成，已合并至 dev）**

- PostCard 帖子卡片组件已完成（用户区/帖子标题摘要/图片网格/交互区/图片预览，@lucide/vue 图标，单图原生比例+多图 4:3 网格，点赞/收藏交互自闭环：内部调 likePost/favoritePost API + 乐观更新+失败回滚，内部维护 likeCount/collectCount ref，formatCount 数字格式化，document 级 Esc 预览关闭）
- HomePage 帖子列表展示已完成（v-infinite-scroll 无限滚动，游标分页，880px 固定宽度）
- 交互区按钮：评论（跳转详情页 #comments）、点赞（红色填充/灰色轮廓切换，乐观更新+回滚）、收藏（黄色填充/灰色轮廓切换，乐观更新+回滚）、分享（ElMessage 占位提示）
- **前端 composable `useInfiniteList`**：由 `usePostList` 重构重命名，泛型化 `useInfiniteList<T>`，支持双模式——游标分页（`mode: 'cursor'`，帖子/评论列表）和传统页码分页（`mode: 'page'`，粉丝/关注列表等），`fetchFn` 注入 + `baseParams` 复用（已移除 likedPosts/collectedPosts 本地 Set，点赞/收藏状态由 PostCard 内部自管理）
- 前端 API `api/posts.ts` 已完成：`fetchPosts` / `fetchPostDetail` / `likePost` / `favoritePost`
- 前端 API `api/users.ts` 新增：`followUser`（POST `/api/users/{id}/follow`）
- 前端类型新增：`PostAuthor`、`PostVO`、`PostListDTO`、`CursorPageResult<T>`、`PostDetailVO`（含 isLiked/isCollected/isFollowed/contentHtml/viewCount/updatedAt）、`LikePostVO`、`FavoritePostVO`、`FollowVO`
- 后端测试数据 `db/data.sql` 已完成：5 个测试用户 + 23 条帖子（覆盖 3 页游标分页）
- **评论组件**：
  - `CommentCard.vue` 已完成：无边框卡片，用户区（头像+昵称+相对时间 + 二级回复时显示"@回复人昵称"，`hideReplyTarget` prop 控制显隐）+ 评论内容 + 底部操作栏（绝对时间占 25% + 点赞/回复/删除按钮占 37.5%），Heart 图标点赞（红色/暗灰切换），MessageCircle 图标回复，Trash2 图标删除（仅本人评论可见，通过 `useUserStore` 判断，hover 红色高亮）；emit `toggle-like` / `reply` / `delete`
  - `CommentThread.vue` 已完成：编排一级评论 + 缩进二级回复（左边线 + padding 缩进），默认展示 3 条预览回复 + "加载更多评论"按钮 → 展开后切换为 Element Plus 分页模式（prev/pager/next，10条/页，仅 >1 页显示），行内回复编辑器插入（回复一级→插顶部，回复二级→插其下方）；子回复 `hideReplyTarget` 由 `reply.replyToUser.id === comment.user.id` 判断；删除操作 `handleDelete`（ElMessageBox 确认 → `deleteComment` API，一级评论 emit `deleted` 给父组件从列表移除，二级回复本地 filter + replyTotal-1）；emit `reply-added` / `deleted`
  - `CommentCreate.vue` 已完成：当前用户头像+昵称 + ElInput textarea + 发布/取消按钮，回复时预填"回复 @{username}："，按钮灰色禁用→蓝色可点，空模板保护防提交纯前缀
- 前端类型新增：`CommentUser`（含 nickname）、`ReplyToUser`（含 nickname）、`CommentVO`（含 replies/replyCount/hasMoreReplies/parentId/replyToUser）、`CreateCommentDTO`、`CreateCommentVO`、`LikeCommentVO`
- 前端 API `api/comments.ts` 已完成：`fetchComments`（游标分页，cursor/size/replySize）/ `fetchReplies`（页码分页，current/size）/ `createComment` / `likeComment` / `deleteComment`（统一箭头函数 + ApiResult 返回类型）
- **共享工具**：`utils/time.ts`（`formatRelativeTime` / `formatDateTime`），`utils/number.ts`（`formatCount`：≥10000 → x.xw），PostCard/CommentCard/PostDetail 已统一接入格式化
- **帖子详情页 PostDetail.vue**：已完成，分上下两张圆角卡片——
  - 上卡片：作者区（头像+昵称+时间+关注按钮，未关注=红色填充/已关注=灰色填充）+ 帖子主体（Eye 浏览数 + 标题 + DOMPurify 净化富文本 + 4 个交互按钮同 PostCard）
  - 下卡片：CommentCreate 一级评论发布 + CommentThread 无限滚动列表（v-infinite-scroll + useInfiniteList<CommentVO>，游标分页，"加载中..."/"没有更多评论了" 指示），`@deleted` 事件监听从列表中 splice 移除已删评论
  - 淡入动画（0.35s），880px 居中，点赞/收藏/关注均乐观更新 + API 失败回滚，交互区数字统一使用 `formatCount` 格式化（≥10000 → x.xw）
- **API 文档**：4.2 评论列表接口从传统页码分页改为游标分页（`cursor`/`hasMore`/`size`，对齐 3.5 帖子列表），4.3 二级回复保持页码分页不变
- **开发问题记录**：`docs/Question.md` 新增问题 21——游标瀑布流实现方案（SQL keyset pagination + 前端 useInfiniteList 双模式 + API 对齐）
- 前端依赖 `@lucide/vue` 已补录到 `docs/01-requirements-and-plan.md`
- **后端帖子接口 3.1~3.5 已完成**（当前分支 feature/post-module）：
  - `PostController` 7 个端点：POST /api/posts（发布）、GET /api/posts/{id}（详情）、PUT /api/posts/{id}（编辑）、DELETE /api/posts/{id}（软删除）、GET /api/posts（游标分页列表，sort=created_at/hot）、POST /api/posts/{id}/like（点赞/取消 toggle）、POST /api/posts/{id}/favorite（收藏/取消 toggle）
  - `PostService` / `PostServiceImpl`：Jsoup 提取纯文本+封面图、游标编解码（`{timestamp}_{id}`）、乐观锁编辑、所有权校验、浏览数+1、点赞/收藏 toggle（查已有→存在删+count-1/不存在插+count+1，GREATEST 防负值）
  - `PostMapper` + `PostMapper.xml`：3 个自定义查询 SQL（`selectListLatest` 游标分页-最新、`selectListHot` 游标分页-热门、`selectDetailWithAuthor` LEFT JOIN 查点赞/收藏/关注状态）+ 4 个计数增减 UPDATE SQL（`incrementLikeCount`/`decrementLikeCount`/`incrementCollectCount`/`decrementCollectCount`，GREATEST 防负值）
  - 新增 Mapper：`UserLikeMapper`、`UserFavoriteMapper`、`UserFollowMapper`
  - 新增 DTO：`CreatePostDTO`、`UpdatePostDTO`
  - 新增 VO：`AuthorVO`、`PostDetailVO`、`PostItemVO`、`CursorPageVO<T>`、`CreatePostVO`、`LikePostVO`（liked + likeCount）、`FavoritePostVO`（favorited）
  - `Result.java` 新增 `success(T data, String msg)` 工厂方法
  - **点赞/收藏接口 3.6~3.7 已完成**：toggle 模式，同一接口反复调用切换状态；点赞返回最新 `likeCount`，收藏按接口文档仅返回 `favorited`（前端本地 ±1 乐观更新）
- **帖子详情页返回按钮**：PostDetail.vue 左上角圆角方框 `<` 按钮，flex 布局位于卡片左侧外部，上边精确对齐作者块上边，点击跳转首页
- **帖子发布页 PostCreate.vue**：已完成，极简现代风格——
  - 外层白色大卡片（880px 居中），内嵌套两个圆角子框（标题框 + 正文框），标签区分输入区域
  - 标题：原生 input，placeholder "输入标题（1 ~ 200 字）"，maxlength=200
  - 正文：TipTap 富文本编辑器（StarterKit + Image + Link 扩展），flex: 1 撑满剩余空间
  - Toolbar：粗体/斜体 | 标题/引用/代码块 | 图片/链接，@lucide/vue 图标，is-active 高亮当前格式，图片通过隐藏 file input + uploadImage API 上传，链接通过 prompt 弹窗设置
  - 发布按钮：标题+正文均非空时变为蓝色可点，否则灰色禁用；保存按钮：灰色禁用，点击提示"功能开发中"
  - 图片上传：工具栏按钮 + 粘贴/拖入均可触发，前端校验类型（jpg/png/gif/webp）和大小（≤10MB），上传后自动插入编辑器
  - 左上角 `<` 返回按钮（40×40px 圆角方框），淡入动画（0.35s）
- **PostCard 图片适配修复**：移除固定 `aspect-ratio: 16/10`，单张图以原生比例显示（max-height: 400px 防超高），多图网格保留 `aspect-ratio: 4/3` 确保同行对齐
- **前端 API `api/posts.ts`** 新增 `createPost`（POST /api/posts）
- **前端类型新增**：`CreatePostDTO`（title/contentHtml）、`CreatePostVO`（id）
- **前端依赖**：`@tiptap/extension-link` 已纳入（作为 starter-kit 传递依赖已存在），补录到 `docs/01-requirements-and-plan.md`
- **代码审查修复（2026-05-27）**：Sprint 3 分支 feature/post-module 完成一轮前后端代码审查，共修复 12 个问题（详见 `docs/Question.md` #22~#33）：
  - **前端 8 项**：useInfiniteList 空记录死循环、posts.ts 箭头函数语法统一、TipTap Link 协议白名单、handlePublish editor 判空、PostCard liked/collected watch 同步、图片预览 Esc 键修复、CommentThread replyLikes 防 prop mutation、CommentCreate 空模板提交拦截
  - **后端 5 项**：帖子详情 SQL 加软删除过滤、服务端 Jsoup.clean HTML 清洗（Safelist 白名单）、CursorPageVO.size→count 重命名、cursor 解析 NumberFormatException→400、PostMapper.xml 注释补全
  - **安全加固**：Link href 协议白名单（http/https/mailto/tel）、服务端 HTML 清洗（Jsoup.clean + Safelist，img src 仅 http/https、a href 仅 http/https/mailto/tel）、软删除查询补漏
  - **新增后端注释**：16 个文件（Controller/DTO/VO/Mapper/Service/XML）全部补全 Javadoc/XML 注释

**Sprint 4 — 评论后端 CRUD + 2.8~2.10 关注/粉丝 + 4.5 评论点赞（已完成，当前分支）**

- 后端评论模块核心逻辑已完成并编译通过：
  - `CommentController` 5 个端点：POST /posts/{postId}/comments（发表）、GET /posts/{postId}/comments（游标分页列表，含预加载二级回复）、GET /comments/{commentId}/replies（二级回复页码分页）、POST /comments/{id}/like（评论点赞/取消 toggle）、DELETE /comments/{id}（软删除，仅限本人）
  - `CommentService` / `CommentServiceImpl`：发表校验（帖子存在 + 父评论存在 + 帖子匹配 + 只能回复一级评论）、游标分页（`{timestamp}_{id}` 编解码，NumberFormatException → 400）、二级回复预加载（LIMIT replySize）+ 加载更多（MyBatis-Plus 页码分页）、级联软删除（一级评论 → 其下所有二级回复一并软删除，`deletedCount` 扣减帖子评论数）、评论点赞 toggle（targetType=2，查 user_like → 删（取消）或插（点赞）+ likeCount 增减）
  - `CommentMapper` + `CommentMapper.xml`：6 个自定义 SQL — `selectTopLevelComments`（游标分页，LEFT JOIN user_like 判断 isLiked）、`selectReplies`（预加载回复，ASC）、`countReplies`（统计 status=1 回复数）、`selectRepliesPage`（页码分页，ORDER BY created_at ASC）、`incrementLikeCount`/`decrementLikeCount`（评论点赞数增减，GREATEST 防负值）
  - `PostMapper` 新增：`incrementCommentCount(id)` / `decrementCommentCount(id, delta)` — MySQL `GREATEST(comment_count - delta, 0)` 防负值
  - 新增 DTO：`CreateCommentDTO`（parentId/replyToUid/content，@Valid 校验 1~5000 字）
  - 新增 VO：`CommentVO`（含预加载 replies/replyCount/hasMoreReplies）、`ReplyVO`（含嵌套 user + replyToUser）、`CommentUserVO`（id/username/nickname/avatar）、`ReplyToUserVO`（id/username/nickname，无头像）、`CreateCommentVO`（id）
  - 开发计划 `docs/04-development-plan.md` Sprint 4 评论 CRUD 接口已标记完成

**Sprint 5 — 搜索（已完成，已合并至 dev）**

- Elasticsearch 8.x 已配置接入（`spring.elasticsearch.uris: http://localhost:9200`）
- ES 安全配置关闭（`xpack.security.enabled: false` + `xpack.security.http.ssl.enabled: false`）
- ES 磁盘水位线调高（dev 单节点 `disk.watermark.low: 97%, high: 98%, flood_stage: 99%`）
- `PostDocument` ES 索引映射实体（@Document(indexName="posts")，ik_max_word/ik_smart 分词，title/contentText 全文检索，username/nickname keyword 精确匹配，createdAt 使用 epoch millis Long）
- `SearchService` / `SearchServiceImpl`：StringQuery + multi_match 多字段匹配 + fuzziness("AUTO") 模糊容错 → HighlightQuery 高亮（`<em>` 标签包裹）→ 分页排序 → 映射 PostItemVO；索引不存在时捕获 NoSuchIndexException 优雅返回空结果
- `PostController` 新增搜索端点 GET /api/posts/search（q/current/size/sort 参数，返回 PageVO<PostItemVO>，端点总数 7 → 8）
- `PostServiceImpl` 帖子发布/更新时同步写入 ES 索引（`syncPostToEs`，try-catch 兜底，ES 失败不影响 MySQL 主流程）
- `PostService` / `PostServiceImpl` 新增全量同步方法 `syncAllPostsToEs()`（先批量查用户 `selectBatchIds` 避免 N+1），AdminController 新增 POST /api/admin/sync-es 端点
- 新增 `PostDocument` ES 文档类（document 包）、`SearchService` 接口 + `SearchServiceImpl` 实现
- 搜索返回字段：title/contentText 含 `<em>` 高亮标签，author 含 id/username/nickname/avatar，likeCount/commentCount/collectCount/createdAt 完整
- **前端搜索页 SearchPage.vue 已完成**：
  - 极简现代风格，与首页相同布局（880px 居中），复用 PostCard 组件 + useInfiniteList 页码分页（mode: 'page'）
  - 左上角 `<` 返回按钮（40px 方形圆角，hover 蓝色渐变背景 + 2px 左滑动效，<960px 隐藏）
  - 搜索头部："搜索「xxx」"（关键词蓝色可点，dotted 下划线）+ 结果统计 + 排序下拉（最新/最热）
  - 3 个状态：Skeleton 骨架屏（shimmer 动画 3 行）→ 空结果（搜索图标 + "未找到相关结果"/"换个关键词试试吧"）→ 结果列表（v-infinite-scroll 无限滚动）
  - 淡入动画（0.35s fadeSlideIn），页面首次加载和重复搜索切换时均触发
  - 关键词变化时自动重置并重新搜索（watch route.query），LayoutPage 加 `_ts` 时间戳强制触发同关键词导航

**Sprint 4 补充 — 关注/粉丝 + 帖子列表 isLiked/isCollected + 测试数据（已完成）**

- **用户主页 UserProfile.vue 修复完成**：
  - 修复了 `useInfiniteList` 解构了不存在的 `likedPosts`/`collectedPosts`/`toggleLike`/`toggleCollect` 导致页面渲染崩溃的问题
  - 替换为本地 `ref(new Set<number>())` + 乐观更新 `handleToggleLike`/`handleToggleCollect`
- **关注/取消关注接口 2.8 已完成**：`POST /api/users/{id}/follow` — toggle 模式，已关注则取消、未关注则关注，禁止关注自己；新增 `FollowVO`（followed: boolean）
- **粉丝列表接口 2.9 已完成**：`GET /api/users/{id}/followers?current=1&size=10` — MyBatis-Plus 页码分页，按关注时间倒序，未登录也可访问；新增 `FollowItemVO`（id/username/nickname/avatar/followedAt）
- **关注列表接口 2.10 已完成**：`GET /api/users/{id}/following?current=1&size=10` — 同上，查询条件改为 followerId = userId
- **评论点赞接口 4.5 已完成**：`POST /api/comments/{id}/like` — toggle 模式，targetType=2，仅返回 liked 状态（前端本地 ±1 交互，跟 3.7 收藏一致）；新增 `LikeCommentVO`（liked: boolean）
- **帖子列表 isLiked/isCollected 已补全**：2.7 和 3.5 响应增加 `isLiked`/`isCollected` 两个 Boolean 字段，4 个列表 SQL（`selectListLatest`/`selectListByUserLatest`/`selectListHot`/`selectListByUserHot`）全部 LEFT JOIN `user_like` + `user_favorite` + `IF(NULL→FALSE)`，未登录传 0L；PostCard 组件点赞/收藏初始化改为读取 `props.post.isLiked`/`isCollected`
- **测试数据 data.sql 已补全**：38 条评论（20 一级+18 二级回复，含互动链）+ 99 条点赞（帖子 59+评论 40）+ 25 条收藏 + 18 条关注关系，全部符合外键约束，时间戳模拟真实时序

### 开发规范（更新中）

- **日志追踪（强制）**：后续所有后端功能开发，Controller / Service 必须添加 @Slf4j 注解并使用 log.info/log.warn 打印业务流日志，格式统一为 `log.info("操作描述 关键参数={}", value)`
- **敏感数据脱敏（强制）**：日志中涉及手机号、邮箱、密码、Token 等敏感字段时，必须通过 `MaskUtil` 工具类脱敏后再输出（`maskPhone` / `maskEmail` / `maskPassword` / `maskToken` / `maskAccount`），禁止明文打印
- 日志文件通过 logback.xml 输出到 `echo-server/logs/` 目录（已在 .gitignore，不纳入版本管理）
- 后续考虑引入 Spring AOP 统一拦截，详见 `docs/roadmap.md` > 日志系统改造为 AOP

---

## TODO

### 近期（基础功能完善）

- [x] 后端实现资料设置接口（GET/PUT /api/users/me/profile）
- [x] 后端实现账号设置接口（GET/PUT /api/users/me/settings）
- [x] 后端实现修改密码接口（PUT /api/users/me/password）
- [x] 后端实现文件上传接口（POST /api/upload/avatar + /api/upload/image，MinIO）
- [x] 前端开发 ProfileSettingsPage：头像/昵称/简介编辑
- [x] 前端开发 SettingsPage：手机号/邮箱（el-form rules 校验）
- [x] 前端开发 ChangePasswordPage.vue：修改密码（旧密码/新密码/确认密码，el-form rules 校验）
- [ ] 引入 Redis，实现忘记密码 / 重置密码邮件验证流程
- [x] 前端开发 PostCard 帖子卡片组件（用户区/帖子标题摘要/图片网格/交互区/图片预览，@lucide/vue 图标）
- [x] 前端开发 HomePage 帖子列表展示（880px 固定宽度，游标分页已接入后端）
- [x] 后端实现帖子接口 3.1~3.5（发布/详情/编辑/删除/游标分页列表，含 Jsoup、游标编解码、乐观锁）
- [x] 开发 PostDetail：帖子详情、评论列表，左上角返回按钮
- [x] 开发 PostCreate：富文本编辑器（TipTap）发帖，含 toolbar（粗体/斜体/标题/引用/代码块/图片/链接）
- [x] 开发 UserProfile：用户主页、发帖列表、关注/粉丝弹窗
- [x] 前端开发评论组件：CommentCard / CommentThread / CommentCreate
- [x] 后端实现评论 CRUD 接口 4.1~4.4（发表/评论列表游标分页/二级回复页码分页/软删除，含级联删除和评论数同步）

### 中期（社交功能）

- [x] 关注 / 取关（后端 toggle 接口已完成 2.8~2.10，前端 UserProfile 已修复）
- [x] 点赞 / 收藏（后端 toggle 接口已完成 + 前端 PostCard 自闭环交互已完成）
- [x] 评论 / 回复（后端 CRUD 已完成，前端组件已完成）
- [ ] 消息通知（RabbitMQ）

### 远期（搜索 & 存储）

- [x] 接入 Elasticsearch 实现全文搜索
- [x] 接入 MinIO 实现图片上传（头像 + 通用图片）
- [ ] OSS 存储实现（OssFileServiceImpl，storage.type=oss 时切换）
- [ ] 404 页面（替换当前静默重定向）
- [ ] 手机号短信验证码注册（需接入短信服务商）

---

## 环境变量清单

| 变量名                  | 用途                       |
| ----------------------- | -------------------------- |
| `DB_USERNAME`           | MySQL 用户名               |
| `DB_PASSWORD`           | MySQL 密码                 |
| `JWT_SECRET`            | JWT 签名密钥               |
| `MAIL_USERNAME`         | Gmail 发件账号             |
| `MAIL_PASSWORD`         | Gmail 应用专用密码（16位） |
| `MINIO_ROOT_USER`       | MinIO 访问密钥             |
| `MINIO_ROOT_PASSWORD`   | MinIO 密钥                 |
| `OSS_ACCESS_KEY_ID`     | 阿里云 OSS Key ID          |
| `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS Key Secret      |

---

## 关键文件索引

| 文件                                                      | 说明                                                                                                                                                                                                                                                                                          |
| --------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `echo-server/src/main/resources/application.yaml`         | 后端全局配置                                                                                                                                                                                                                                                                                  |
| `echo-server/src/main/resources/logback.xml`              | 日志配置（控制台 + 滚动文件输出到 logs/）                                                                                                                                                                                                                                                     |
| `echo-server/.../security/SecurityConfig.java`            | Security 白名单与无状态配置                                                                                                                                                                                                                                                                   |
| `echo-server/.../security/JwtAuthFilter.java`             | JWT 请求过滤器                                                                                                                                                                                                                                                                                |
| `echo-server/.../security/JwtUtil.java`                   | Token 生成与解析                                                                                                                                                                                                                                                                              |
| `echo-server/.../service/impl/AuthServiceImpl.java`       | 认证业务逻辑                                                                                                                                                                                                                                                                                  |
| `echo-server/.../controller/UserController.java`          | 用户模块控制器（资料设置/账号设置/修改密码）                                                                                                                                                                                                                                                  |
| `echo-server/.../controller/UserPublicController.java`    | 用户公开信息控制器（用户主页 + 用户帖子列表 + 关注/取消 + 粉丝列表 + 关注列表）                                                                                                                                                                                                                |
| `echo-server/.../controller/FileController.java`          | 文件上传控制器（头像/图片上传 + 文件删除，需认证）                                                                                                                                                                                                                                            |
| `echo-server/.../service/FileService.java`                | 文件存储服务接口（uploadAvatar / uploadImage / deleteFile）                                                                                                                                                                                                                                   |
| `echo-server/.../service/impl/UserServiceImpl.java`       | 用户模块业务逻辑（含手机/邮箱唯一性校验、BCrypt 改密、空更新保护、关注/取消关注 toggle、粉丝/关注列表页码分页）                                                                                                                                                                                                                            |
| `echo-server/.../service/impl/MinioFileServiceImpl.java`  | MinIO 文件存储实现（扩展名白名单、文件大小校验、resolveContentType 防 XSS、try-with-resources、endpoint 规范化、deleteFile 回滚清理）                                                                                                                                                         |
| `echo-server/.../mapper/UserMapper.java`                  | 用户模块 Mapper（与 AuthMapper 按业务拆分）                                                                                                                                                                                                                                                   |
| `echo-server/.../config/MinioProperties.java`             | MinIO 配置属性（endpoint/publicBaseUrl/accessKey/secretKey/bucketName，@NotBlank 启动校验）                                                                                                                                                                                                   |
| `echo-server/.../config/MinioConfig.java`                 | MinIO 客户端条件注入（storage.type=minio，含 MinioProperties 条件注册绑定）                                                                                                                                                                                                                   |
| `echo-server/.../vo/UploadAvatarVO.java`                  | 头像上传响应 VO                                                                                                                                                                                                                                                                               |
| `echo-server/.../vo/UploadImageVO.java`                   | 图片上传响应 VO                                                                                                                                                                                                                                                                               |
| `echo-server/.../util/MaskUtil.java`                      | 敏感数据脱敏工具（phone/email/password/token/account）                                                                                                                                                                                                                                        |
| `echo-server/.../common/GlobalExceptionHandler.java`      | 全局异常处理器（含 DuplicateKeyException→409、MissingServletRequestPartException/MultipartException→400 映射）                                                                                                                                                                                |
| `echo-server/.../controller/PostController.java`          | 帖子模块控制器（发布/详情/编辑/删除/列表/点赞/收藏/搜索 8 端点，游标分页 + toggle 模式 + ES 全文检索）                                                                                                                                                                                        |
| `echo-server/.../service/PostService.java`                | 帖子模块服务接口（含 likePost/favoritePost/listUserPosts 方法，isLiked/isCollected 状态注入）                                                                                                                                                                                                                                      |
| `echo-server/.../service/impl/PostServiceImpl.java`       | 帖子模块服务实现（Jsoup.clean HTML 清洗、游标编解码+NumberFormatException→400、乐观锁、所有权校验、文本/封面提取、点赞/收藏 toggle 逻辑、ES 同步写入、批量用户查询优化 N+1、isLiked/isCollected 当前用户状态注入）                                                                                                                                        |
| `echo-server/.../mapper/PostMapper.java`                  | 帖子 Mapper（BaseMapper + 5 自定义查询 + 6 计数增减方法：incrementCommentCount/decrementCommentCount/incrementLikeCount/decrementLikeCount/incrementCollectCount/decrementCollectCount）                                                                                                      |
| `echo-server/.../mapper/CommentMapper.java`               | 评论 Mapper（6 自定义 SQL：游标分页/预加载回复/回复统计/二级回复分页/点赞数增减）                                                                                                                                                                                                             |
| `echo-server/src/main/resources/mapper/CommentMapper.xml` | 评论自定义 SQL（resultMap + 游标分页 + LEFT JOIN user_like + 页码分页 + incrementLikeCount/decrementLikeCount）                                                                                                                                                                                |
| `echo-server/.../controller/CommentController.java`       | 评论控制器（发表/游标分页列表/二级回复分页/点赞/软删除 5 端点，所有权校验）                                                                                                                                                                                                                   |
| `echo-server/.../service/CommentService.java`             | 评论服务接口（createComment/listComments/listReplies/likeComment/deleteComment）                                                                                                                                                                                                             |
| `echo-server/.../service/impl/CommentServiceImpl.java`    | 评论服务实现（发表校验+游标编解码+预加载回复+级联软删除+评论点赞 toggle+GREATEST 防负值扣减）                                                                                                                                                                                                |
| `echo-server/.../dto/CreateCommentDTO.java`               | 发表评论请求 DTO（parentId/replyToUid/content，@Valid 1~5000 字）                                                                                                                                                                                                                             |
| `echo-server/.../vo/CommentVO.java`                       | 一级评论 VO（含预加载 replies/replyCount/hasMoreReplies）                                                                                                                                                                                                                                     |
| `echo-server/.../vo/ReplyVO.java`                         | 二级回复 VO（含嵌套 user + replyToUser + isLiked）                                                                                                                                                                                                                                            |
| `echo-server/.../vo/CommentUserVO.java`                   | 评论用户信息 VO（id/username/nickname/avatar）                                                                                                                                                                                                                                                |
| `echo-server/.../vo/ReplyToUserVO.java`                   | 被回复用户信息 VO（id/username/nickname，无头像）                                                                                                                                                                                                                                             |
| `echo-server/.../vo/CreateCommentVO.java`                 | 创建评论响应 VO（id）                                                                                                                                                                                                                                                                         |
| `echo-server/.../mapper/UserLikeMapper.java`              | 点赞关系 Mapper                                                                                                                                                                                                                                                                               |
| `echo-server/.../mapper/UserFavoriteMapper.java`          | 收藏关系 Mapper                                                                                                                                                                                                                                                                               |
| `echo-server/.../mapper/UserFollowMapper.java`            | 关注关系 Mapper                                                                                                                                                                                                                                                                               |
| `echo-server/src/main/resources/mapper/PostMapper.xml`    | 帖子自定义 SQL（游标分页最新/热门 + 详情多表 JOIN 含软删除过滤 p.status=1 + 6 个计数增减 UPDATE：commentCount/likeCount/collectCount ±1，GREATEST 防负值 + 4 个列表 SQL 全部 LEFT JOIN user_like/user_favorite 获取 isLiked/isCollected）|
| `echo-server/.../dto/CreatePostDTO.java`                  | 发布帖子请求 DTO                                                                                                                                                                                                                                                                              |
| `echo-server/.../dto/UpdatePostDTO.java`                  | 编辑帖子请求 DTO                                                                                                                                                                                                                                                                              |
| `echo-server/.../vo/AuthorVO.java`                        | 作者信息 VO（id/username/nickname/avatar）                                                                                                                                                                                                                                                    |
| `echo-server/.../vo/PostDetailVO.java`                    | 帖子详情 VO（含 isLiked/isCollected/isFollowed 状态）                                                                                                                                                                                                                                         |
| `echo-server/.../vo/PostItemVO.java`                      | 帖子列表项 VO（含嵌套 AuthorVO + isLiked/isCollected）                                                                                                                                                                                                                                          |
| `echo-server/.../vo/CursorPageVO.java`                    | 游标分页通用 VO（cursor/hasMore/count/records，count 为本次返回的实际记录数）                                                                                                                                                                                                                 |
| `echo-server/.../vo/CreatePostVO.java`                    | 创建帖子响应 VO（id）                                                                                                                                                                                                                                                                         |
| `echo-server/.../vo/LikePostVO.java`                      | 帖子点赞/取消响应 VO（liked + likeCount，toggle 模式）                                                                                                                                                                                                                                        |
| `echo-server/.../vo/FavoritePostVO.java`                  | 帖子收藏/取消响应 VO（favorited，toggle 模式）                                                                                                                                                                                                                                                |
| `echo-server/.../vo/FollowVO.java`                        | 关注/取消关注响应 VO（followed: boolean，toggle 模式）                                                                                                                                                                                                                                        |
| `echo-server/.../vo/FollowItemVO.java`                    | 粉丝/关注列表项 VO（id/username/nickname/avatar/followedAt）                                                                                                                                                                                                                                  |
| `echo-server/.../vo/LikeCommentVO.java`                   | 评论点赞/取消响应 VO（liked: boolean，toggle 模式，前端本地 ±1）                                                                                                                                                                                                                              |
| `echo-server/.../vo/PageVO.java`                          | 页码分页通用 VO（records/total/current/size）                                                                                                                                                                                                                                                 |
| `echo-server/.../document/PostDocument.java`              | ES 帖子索引映射实体（@Document(indexName="posts")，ik_max_word/ik_smart 分词）                                                                                                                                                                                                                |
| `echo-server/.../service/SearchService.java`              | 搜索服务接口（全文检索 + 高亮 + 分页）                                                                                                                                                                                                                                                        |
| `echo-server/.../service/impl/SearchServiceImpl.java`     | 搜索服务实现（StringQuery + multi_match + fuzziness + HighlightQuery `<em>` 高亮 + PostItemVO 映射 + NoSuchIndexException 优雅降级）                                                                                                                                                          |
| `echo-web/src/api/users.ts`                               | 用户模块前端 API（含 uploadAvatar/uploadImage/deleteFile/followUser）                                                                                                                                                                                                                         |
| `echo-web/src/api/posts.ts`                               | 帖子模块前端 API（createPost/fetchPosts/fetchPostDetail/likePost/favoritePost/searchPosts，统一箭头函数 + ApiResult 双参数泛型）                                                                                                                                                              |
| `echo-web/src/api/comments.ts`                            | 评论模块前端 API（fetchComments 游标分页/fetchReplies 页码分页/createComment/likeComment/deleteComment，统一箭头函数 + ApiResult 返回类型）                                                                                                                                                   |
| `echo-web/src/api/modules/index.ts`                       | 前端类型定义（DTO/VO/Result 泛型，含 CreatePostDTO/CreatePostVO 等）                                                                                                                                                                                                                          |
| `echo-web/src/composables/useInfiniteList.ts`             | 通用无限列表 composable（泛型 `<T>`，双模式 cursor/page，fetchFn 注入 + baseParams 复用，已修复空记录提前返回死循环；已移除 likedPosts/collectedPosts，点赞/收藏状态由 PostCard 自管理）                                                                                                      |
| `echo-web/src/utils/time.ts`                              | 共享时间格式化工具（formatRelativeTime 相对时间 / formatDateTime 绝对时间）                                                                                                                                                                                                                   |
| `echo-web/src/utils/number.ts`                            | 数字格式化工具（formatCount：≥10000 → x.xw 简写，自动去末尾 0）                                                                                                                                                                                                                               |
| `echo-web/src/utils/result.ts`                            | Axios 封装，含 401 自动刷新逻辑                                                                                                                                                                                                                                                               |
| `echo-server/.../common/Result.java`                      | 统一响应包装（success/error，新增 success(data, msg) 工厂方法）                                                                                                                                                                                                                               |
| `echo-web/src/stores/userStore.ts`                        | 用户状态（Token + 用户信息）                                                                                                                                                                                                                                                                  |
| `echo-web/src/router/index.ts`                            | 路由定义与守卫                                                                                                                                                                                                                                                                                |
| `echo-web/src/components/LayoutPage.vue`                  | 主布局（顶栏 + 可折叠侧边栏 + 内容区），下拉含资料设置/账号设置/修改密码/退出                                                                                                                                                                                                                 |
| `echo-web/src/components/PostCard.vue`                    | 帖子卡片组件（用户区/帖子区/图片网格/交互区/图片预览，@lucide/vue 图标，单图原生比例+多图 4:3 网格，点赞/收藏自闭环：内部调 API + 乐观更新+失败回滚 + 内部维护 likeCount/collectCount ref，formatCount 数字格式化，document 级 Esc 预览关闭，标题/摘要 v-html 渲染 + :deep(em) 搜索高亮样式） |
| `echo-web/src/components/CommentCard.vue`                 | 评论卡片组件（无边框，用户区含"@回复人"昵称+删除按钮仅本人可见+评论内容+底部时间(25%)/点赞回复删除按钮(37.5%)，Heart红色切换+Trash2 hover红色，emit toggle-like/reply/delete，formatCount 数字格式化）                                                                                        |
| `echo-web/src/components/CommentThread.vue`               | 评论线程组件（一级评论+缩进二级回复+加载更多/分页+行内回复编辑器插入，replyLikes 本地覆盖防 prop mutation，handleDelete ElMessageBox 确认→deleteComment API→emit deleted，子回复 hideReplyTarget 按 replyToUser.id === 一级作者 id 判断）                                                     |
| `echo-web/src/components/CommentCreate.vue`               | 评论发布组件（ElInput textarea+发布按钮，回复预填"回复@{username}："，空模板保护防提交纯前缀）                                                                                                                                                                                                |
| `echo-web/src/views/HomePage.vue`                         | 首页帖子列表（v-infinite-scroll 无限滚动 + useInfiniteList composable，PostCard 点赞/收藏交互已下沉到组件内部自闭环）                                                                                                                                                                         |
| `echo-web/src/views/PostDetail.vue`                       | 帖子详情页（淡入动画，作者区+关注按钮+帖子主体 DOMPurify 净化+4 交互按钮，评论区 useInfiniteList<CommentVO> + v-infinite-scroll 无限滚动，乐观更新+回滚，handleCommentDeleted splice 移除已删评论，formatCount 数字格式化）                                                                   |
| `echo-web/src/views/PostCreate.vue`                       | 帖子发布页（TipTap 富文本 + Toolbar 粗体/斜体/标题/引用/代码块/图片/链接 + Link 协议白名单 http/https/mailto/tel + 图片粘贴拖入上传 + 标题/正文双框布局 + 淡入动画 + editor 就绪保护）                                                                                                        |
| `echo-web/src/views/ProfileSettingsPage.vue`              | 资料设置页（头像本地预览+选择图片，保存时统一上传；昵称+简介编辑；保存/取消）                                                                                                                                                                                                                 |
| `echo-web/src/views/SettingsPage.vue`                     | 账号设置页（手机号+邮箱编辑，el-form rules 校验，保存/取消）                                                                                                                                                                                                                                  |
| `echo-web/src/views/ChangePasswordPage.vue`               | 修改密码页（旧密码/新密码/确认密码，show-password 切换，el-form rules 校验，绿色渐变修改密码按钮，忘记密码链接）                                                                                                                                                                              |
| `echo-server/src/main/resources/db/init.sql`              | 数据库初始化 DDL（6 张表 + 索引）                                                                                                                                                                                                                                                             |
| `echo-server/src/main/resources/db/data.sql`              | 测试数据（5 用户 + 23 帖子 + 38 评论 + 99 点赞 + 25 收藏 + 18 关注，全部符合外键约束）                                                                                                                                                                                                       |
| `docs/roadmap.md`                                         | 待开发功能方案设计（含瀑布流加载/游标分页方案）                                                                                                                                                                                                                                               |
| `docs/05-feature-flows.md`                                | 功能模块与流程图（11 张 Mermaid 流程图，含 MinIO/OSS 文件上传）                                                                                                                                                                                                                               |
| `docs/《EchoSpace》系统设计.md`                           | 系统设计文档（架构 + 模块 + 数据库 + 设计要点）                                                                                                                                                                                                                                               |
| `docs/《EchoSpace》系统设计.docx`                         | 系统设计文档 Word 版（同上，含格式化表格）                                                                                                                                                                                                                                                    |
| `docs/01-requirements-and-plan.md`                        | 需求与技术方案文档                                                                                                                                                                                                                                                                            |
| `docs/02-api-documentation.md`                            | API 接口文档（含用户/帖子/评论/文件模块，游标分页响应字段 size→count）                                                                                                                                                                                                                        |
| `docs/Question.md`                                        | 开发问题记录（按主题分组：后端 Security 构建 / 前端 axios 拦截器 / 后端其他 / 前端其他 / Sprint 3 前后端代码审查 #22~#33）                                                                                                                                                                    |
