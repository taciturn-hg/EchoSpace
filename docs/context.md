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
│    [PostController / UserController — 待开发]           │
│                                                         │
│  Service Layer                                          │
│    AuthServiceImpl                                      │
│    [PostService / UserService — 待开发]                 │
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
| ProfileSettingsPage.vue | `/settings/profile` | 占位，待开发 |
| SettingsPage.vue | `/settings` | 占位 |

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

**Sprint 2 — 基础功能页面（进行中）**
- 当前分支：dev
- 所有业务页面当前为占位状态，尚未实现具体功能
- 忘记密码 / 重置密码流程已规划，等待 Redis 接入后实现

---

## TODO

### 近期（基础功能完善）

- [ ] 引入 Redis，实现忘记密码 / 重置密码邮件验证流程
- [ ] 开发 HomePage：帖子列表、分页、基础筛选
- [ ] 开发 PostDetail：帖子详情、评论列表
- [ ] 开发 PostCreate：富文本编辑器（TipTap）发帖
- [ ] 开发 UserProfile：用户主页、发帖列表
- [ ] 开发 SettingsPage：修改昵称、头像、密码

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
| `echo-server/.../security/SecurityConfig.java` | Security 白名单与无状态配置 |
| `echo-server/.../security/JwtAuthFilter.java` | JWT 请求过滤器 |
| `echo-server/.../security/JwtUtil.java` | Token 生成与解析 |
| `echo-server/.../service/impl/AuthServiceImpl.java` | 认证业务逻辑 |
| `echo-web/src/utils/result.ts` | Axios 封装，含 401 自动刷新逻辑 |
| `echo-web/src/stores/userStore.ts` | 用户状态（Token + 用户信息） |
| `echo-web/src/router/index.ts` | 路由定义与守卫 |
| `docs/roadmap.md` | 待开发功能方案设计 |
| `docs/01-requirements-and-plan.md` | 需求与技术方案文档 |
| `docs/Question.md` | 开发问题记录（按主题分组：后端 Security 构建 / 前端 axios 拦截器 / 后端其他 / 前端其他） |
