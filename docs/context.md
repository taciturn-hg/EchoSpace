# EchoSpace 项目上下文

> 本文件记录项目当前状态，供开发过程中快速定位上下文使用。随开发进展持续更新。

---

## 技术栈

### 后端（echo-server）

| 层次 | 技术 |
|------|------|
| 框架 | Spring Boot 3.5.x，Java 17 |
| 安全 | Spring Security + JWT（jjwt 0.12.6） |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis（已引入依赖，暂未接入） |
| 搜索 | Elasticsearch（已引入依赖，暂未接入） |
| 存储 | MinIO / 阿里云 OSS（已引入依赖，暂未接入） |
| 邮件 | Spring Mail + Gmail SMTP |
| 消息队列 | RabbitMQ（已引入依赖，暂未接入） |
| 文档 | SpringDoc OpenAPI 2.x（Swagger UI） |
| 构建 | Maven |

### 前端（echo-web）

| 层次 | 技术 |
|------|------|
| 框架 | Vue 3.5.x + TypeScript 6.x |
| 构建 | Vite 8.x |
| UI 组件库 | Element Plus 2.x |
| 状态管理 | Pinia 3.x |
| 路由 | Vue Router 5.x |
| HTTP | Axios 1.x（封装于 `utils/result.ts`） |
| 富文本编辑器 | TipTap 3.x |
| HTML 净化 | DOMPurify 3.x |
| 样式 | SCSS（Sass） |
| 代码规范 | ESLint + Prettier + Oxlint |

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
│    [PostController — 待开发]                             │
│                                                         │
│  Service Layer                                          │
│    AuthServiceImpl                                      │
│    UserServiceImpl (profile / settings / changePassword) │
│    [PostService — 待开发]                               │
│                                                         │
│  Security Layer                                         │
│    JwtAuthFilter → JwtUtil → SecurityUtil               │
│    SecurityConfig（白名单 + 无状态 Session）             │
│                                                         │
│  Data Layer                                             │
│    MyBatis-Plus Mapper → MySQL                          │
│    [Redis — 待接入]                                     │
│    [Elasticsearch — 待接入]                             │
│    [MinIO / OSS — 待接入]                               │
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

### 页面占位（已建文件，内容待开发）

| 页面 | 路由 | 状态 |
|------|------|------|
| ForgotPassword.vue | `/forgot-password` | 占位，待 Redis 接入后开发 |
| ResetPassword.vue | `/reset-password` | 占位，待 Redis 接入后开发 |
| HomePage.vue | `/` | 占位 |
| PostDetail.vue | `/post/:id` | 占位 |
| PostCreate.vue | `/post/create` | 占位 |
| SearchPage.vue | `/search` | 占位 |
| UserProfile.vue | `/user/:id` | 占位 |
| ProfileSettingsPage.vue | `/settings/profile` | 已完成（头像上传/昵称/简介编辑+保存/取消） |
| SettingsPage.vue | `/settings` | 已完成（手机号/邮箱编辑+保存/取消，el-form rules 校验） |
| ChangePasswordPage.vue | `/settings/change-password` | 已完成（el-form rules 校验，旧密码/新密码/确认密码，show-password 切换，修改密码/清空按钮，忘记密码链接） |

### 数据库实体（已建 Entity）

| 实体 | 说明 |
|------|------|
| User | 用户基础信息（id, username, nickname, email, phone, password, avatar, bio, status） |
| Post | 帖子（待完善） |
| Comment | 评论（待完善） |
| UserFavorite | 收藏关系（待完善） |
| UserFollow | 关注关系（待完善） |
| UserLike | 点赞关系（待完善） |

---

## 当前进度

**Sprint 1 — 认证基础（已完成，已合并至 dev）**
- 后端：注册、登录、刷新 Token、获取当前用户信息
- 前端：登录页、注册页、路由守卫、Token 自动刷新
- feature/login-register → dev（PR #3 已合并）

**Sprint 2 — 用户认证与账号管理（进行中）**
- 当前分支：feature/user-settings
- 完成了 LayoutPage.vue 布局系统：固定顶栏、可折叠左侧导航、搜索框、用户卡片、发布帖子按钮、下拉菜单（资料设置/账号设置/修改密码/退出登录）
- 新增 ProfileSettingsPage.vue 完整实现：头像上传（前端校验，上传接口 TODO）、昵称编辑、个人简介编辑（Element Plus textarea，6行固定高度）、取消按钮（重新拉取服务器数据覆盖本地，失败时提示"还原失败"）、保存按钮（绿色渐变，仅脏数据可点击）
- 新增 ChangePasswordPage.vue 完整实现：旧密码/新密码/确认密码三输入（show-password 切换），el-form rules 校验（必填、长度 6~20、新旧密码不同、两次输入一致），修改密码按钮 PUT /api/users/me/password 后端返回消息提示，清空按钮重置表单与校验状态，按钮行左侧"忘记密码？"链接（当前禁用态，待 Redis 接入后启用），绿色渐变按钮仅脏数据可点击
- 新增 SettingsPage.vue 完整实现：手机号/邮箱编辑（el-form rules 必填 + 格式校验），取消回拉服务器数据（失败时提示"还原失败"），保存 PUT /api/users/me/settings 并同步 Pinia store
- 前端 API 层新增 `echo-web/src/api/users.ts`（getProfile / updateProfile / getSettings / updateSettings / changePassword），类型新增 UserProfileVO / UpdateProfileDTO / UserSettingsVO / UpdateSettingsDTO / ChangePasswordDTO；文件上传函数（uploadAvatar / uploadImage）以 TODO 注释占位，待 Sprint 2 后期实现
- 后端 UserSettingsVO 取消手机号/邮箱脱敏，直接返回原始值（前端展示用，与 /api/auth/me 保持一致）
- 用户表 nickname 字段改为可为空，前端 LayoutPage/ProfileSettingsPage 已适配：昵称为空时展示 username
- 接口文档 02-api-documentation.md 已更新：文件上传模块 2 个通用接口（/api/upload/image + /api/upload/avatar），底层由 FileService 按 storage.type 切换 MinIO/OSS 实现；nickname 响应字段标注"非必须（可为空）"；附录接口总数 30
- 开发计划 04-development-plan.md 已同步：文件上传接口在 Sprint 2（累计接口数 9→11），OSS 存储实现（OssFileServiceImpl）留在 Sprint 3，全景图接口数联级更新（最终 30）
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
- [x] 前端开发 ProfileSettingsPage：头像/昵称/简介编辑
- [x] 前端开发 SettingsPage：手机号/邮箱（el-form rules 校验）
- [x] 前端开发 ChangePasswordPage.vue：修改密码（旧密码/新密码/确认密码，el-form rules 校验）
- [ ] 引入 Redis，实现忘记密码 / 重置密码邮件验证流程
- [ ] 开发 HomePage：帖子列表、分页、基础筛选
- [ ] 开发 PostDetail：帖子详情、评论列表
- [ ] 开发 PostCreate：富文本编辑器（TipTap）发帖
- [ ] 开发 UserProfile：用户主页、发帖列表

### 中期（社交功能）

- [ ] 关注 / 取关
- [ ] 点赞 / 收藏
- [ ] 评论 / 回复
- [ ] 消息通知（RabbitMQ）

### 远期（搜索 & 存储）

- [ ] 接入 Elasticsearch 实现全文搜索
- [ ] 接入 MinIO / OSS 实现图片上传
- [ ] 404 页面（替换当前静默重定向）
- [ ] 手机号短信验证码注册（需接入短信服务商）

---

## 环境变量清单

| 变量名 | 用途 |
|--------|------|
| `DB_USERNAME` | MySQL 用户名 |
| `DB_PASSWORD` | MySQL 密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `MAIL_USERNAME` | Gmail 发件账号 |
| `MAIL_PASSWORD` | Gmail 应用专用密码（16位） |
| `MINIO_ROOT_USER` | MinIO 访问密钥 |
| `MINIO_ROOT_PASSWORD` | MinIO 密钥 |
| `OSS_ACCESS_KEY_ID` | 阿里云 OSS Key ID |
| `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS Key Secret |

---

## 关键文件索引

| 文件 | 说明 |
|------|------|
| `echo-server/src/main/resources/application.yaml` | 后端全局配置 |
| `echo-server/src/main/resources/logback.xml` | 日志配置（控制台 + 滚动文件输出到 logs/） |
| `echo-server/.../security/SecurityConfig.java` | Security 白名单与无状态配置 |
| `echo-server/.../security/JwtAuthFilter.java` | JWT 请求过滤器 |
| `echo-server/.../security/JwtUtil.java` | Token 生成与解析 |
| `echo-server/.../service/impl/AuthServiceImpl.java` | 认证业务逻辑 |
| `echo-server/.../controller/UserController.java` | 用户模块控制器（资料设置/账号设置/修改密码） |
| `echo-server/.../service/impl/UserServiceImpl.java` | 用户模块业务逻辑（含手机/邮箱唯一性校验、BCrypt 改密、空更新保护） |
| `echo-server/.../mapper/UserMapper.java` | 用户模块 Mapper（与 AuthMapper 按业务拆分） |
| `echo-server/.../util/MaskUtil.java` | 敏感数据脱敏工具（phone/email/password/token/account） |
| `echo-server/.../common/GlobalExceptionHandler.java` | 全局异常处理器（含 DuplicateKeyException→409 映射） |
| `echo-web/src/api/users.ts` | 用户模块前端 API（getProfile / updateProfile / getSettings / updateSettings / changePassword；uploadAvatar/uploadImage TODO） |
| `echo-web/src/utils/result.ts` | Axios 封装，含 401 自动刷新逻辑 |
| `echo-web/src/stores/userStore.ts` | 用户状态（Token + 用户信息） |
| `echo-web/src/router/index.ts` | 路由定义与守卫 |
| `echo-web/src/components/LayoutPage.vue` | 主布局（顶栏 + 可折叠侧边栏 + 内容区），下拉含资料设置/账号设置/修改密码/退出 |
| `echo-web/src/views/ProfileSettingsPage.vue` | 资料设置页（头像本地预览+选择图片，保存时统一上传；昵称+简介编辑；保存/取消） |
| `echo-web/src/views/SettingsPage.vue` | 账号设置页（手机号+邮箱编辑，el-form rules 校验，保存/取消） |
| `echo-web/src/views/ChangePasswordPage.vue` | 修改密码页（旧密码/新密码/确认密码，show-password 切换，el-form rules 校验，绿色渐变修改密码按钮，忘记密码链接） |
| `docs/roadmap.md` | 待开发功能方案设计 |
| `docs/05-feature-flows.md` | 功能模块与流程图（11 张 Mermaid 流程图，含 MinIO/OSS 文件上传） |
| `docs/《EchoSpace》系统设计.md` | 系统设计文档（架构 + 模块 + 数据库 + 设计要点） |
| `docs/《EchoSpace》系统设计.docx` | 系统设计文档 Word 版（同上，含格式化表格） |
| `docs/01-requirements-and-plan.md` | 需求与技术方案文档 |
| `docs/02-api-documentation.md` | API 接口文档（含用户/帖子/评论/文件模块） |
| `docs/Question.md` | 开发问题记录（按主题分组：后端 Security 构建 / 前端 axios 拦截器 / 后端其他 / 前端其他） |
