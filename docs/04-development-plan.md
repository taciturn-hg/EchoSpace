# EchoSpace 开发阶段规划

## Sprint 1（1-2天）：项目脚手架

- [x] 安装并本地启动 MySQL 8.0、Redis 7、Elasticsearch 8
- [x] 下载 MinIO 二进制文件，本地启动（`minio server /data`）
- [x] 初始化 Spring Boot 3 项目 + 基础配置（yml、CORS、MyBatis-Plus）
- [x] 初始化 Vue 3 + Vite 项目 + Element Plus + 路由
- [x] 数据库建表 + Flyway 迁移脚本（暂用 init.sql 直接执行，后续引入 Flyway）
- [x] 统一响应格式封装 + 全局异常处理

### Sprint 1 产出物

```text
echo-server/src/main/java/com/echospace/
├── common/
│   ├── Result.java                     # 统一响应 {code, message, data}
│   ├── PageResult.java                 # 分页响应 {records, total, page, size}
│   └── GlobalExceptionHandler.java     # 全局异常处理
├── config/
│   ├── CorsConfig.java                 # 跨域配置
│   └── MyBatisPlusConfig.java          # MyBatis-Plus 配置（分页插件、乐观锁）
├── entity/                             # 实体类（6张表）
├── mapper/                             # MyBatis-Plus Mapper 接口
└── EchoServerApplication.java

echo-web/src/
├── router/index.ts                     # 路由配置
├── stores/                             # Pinia
├── api/                                # Axios 封装
├── views/                              # 页面占位
└── App.vue
```

---

## Sprint 2（2-3天）：用户认证与账号管理（方案C：Spring Security 轻量集成）

- [x] JwtUtil 工具类（生成 Token、解析 Token、校验过期）
- [x] JwtAuthFilter（继承 OncePerRequestFilter，解析 JWT → 写入 SecurityContextHolder）
- [x] SecurityConfig（配置白名单路径 /api/auth/register, /api/auth/login, /api/auth/refresh，关闭 CSRF/Session，注册 JwtAuthFilter）
- [x] SecurityUtil 工具类（封装 getCurrentUserId / getCurrentUsername）
- [x] 注册/登录接口（BCrypt 加密密码，登录支持用户名/邮箱/手机号，返回 accessToken + refreshToken）
- [x] 刷新 Token 接口
- [x] 获取当前用户信息接口 (GET /api/auth/me)
- [ ] 账号设置信息接口 (GET /api/users/me/settings)
- [ ] 更新个人信息接口 (PUT /api/users/{id})
- [ ] 修改密码接口 (PUT /api/users/me/password)
- [ ] 前端登录/注册页面 + Axios 拦截器（Token 注入 + 过期刷新）

### Sprint 2 产出物

```text
echo-server/src/main/java/com/echospace/
├── security/
│   ├── JwtUtil.java                    # JWT 生成、解析、校验
│   ├── JwtAuthFilter.java              # OncePerRequestFilter 实现
│   ├── SecurityConfig.java             # SecurityFilterChain 配置
│   └── SecurityUtil.java               # 取当前用户工具类
├── controller/
│   ├── AuthController.java             # /api/auth/register, login, refresh, me
│   └── UserController.java             # /api/users/me/settings, /api/users/{id} (PUT), /api/users/me/password
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       └── UserServiceImpl.java
├── dto/
│   ├── RegisterDTO.java                # 含 username, phone, email, password
│   ├── LoginDTO.java
│   ├── RefreshTokenDTO.java
│   ├── UpdateProfileDTO.java           # avatar, nickname, phone, email, bio
│   └── ChangePasswordDTO.java          # oldPassword, newPassword, confirmPassword
└── vo/
    ├── LoginVO.java                    # {accessToken, refreshToken, expiresIn}
    └── UserSettingsVO.java             # {avatar, nickname, phone, email, bio}

echo-web/src/
├── views/
│   ├── Login.vue
│   ├── Register.vue
│   └── Settings.vue                      # 账号设置 / 编辑资料 / 修改密码
├── api/
│   ├── auth.ts                        # Axios 拦截器 + 认证相关请求
└── stores/
    └── userStore.ts                    # 用户登录态管理
```

---

## Sprint 3（2-3天）：帖子核心

- [ ] MinIO 文件上传接口（帖子图片上传 + 头像上传）
- [ ] 帖子 CRUD 接口
- [ ] 前端 Tiptap 富文本编辑器集成（含图片上传 extension）
- [ ] 前端帖子发布页 + 帖子列表页 + 帖子详情页
- [ ] jsoup HTML 清洗 + 纯文本提取

### Sprint 3 产出物

```text
echo-server/src/main/java/com/echospace/
├── config/
│   └── MinioConfig.java                # MinIO 客户端配置
├── controller/
│   ├── PostController.java             # /api/posts CRUD
│   └── UploadController.java           # /api/upload/image（帖子图片）, /api/upload/avatar（头像）
├── service/
│   ├── PostService.java
│   ├── FileService.java
│   └── impl/
│       ├── PostServiceImpl.java
│       └── FileServiceImpl.java
├── dto/
│   └── CreatePostDTO.java
├── vo/
│   ├── PostDetailVO.java
│   └── PostListVO.java
└── utils/
    └── HtmlUtil.java                   # jsoup 清洗 HTML + 提取纯文本

echo-web/src/
├── views/
│   ├── PostCreate.vue                  # 发布页（Tiptap）
│   ├── PostDetail.vue                  # 帖子详情页
│   └── Home.vue                        # 首页帖子列表
├── components/
│   ├── RichTextEditor.vue              # Tiptap 封装组件
│   └── PostCard.vue                    # 帖子卡片组件
└── api/
    ├── post.ts
    └── upload.ts
```

---

## Sprint 4（2天）：评论 + 互动

- [ ] 评论 CRUD 接口（一级评论 + 二级回复）
- [ ] 点赞/取消接口（Redis Set + MySQL user_like 双写 + 定时同步计数）
- [ ] 收藏/取消接口
- [ ] 前端评论组件（嵌套展示）
- [ ] 前端点赞/收藏交互

### Sprint 4 产出物

```text
echo-server/src/main/java/com/echospace/
├── controller/
│   └── CommentController.java          # /api/posts/{postId}/comments, /api/comments/{id}
├── service/
│   ├── CommentService.java
│   ├── LikeService.java
│   ├── FavoriteService.java
│   └── impl/
│       ├── CommentServiceImpl.java
│       ├── LikeServiceImpl.java        # Redis Set + MySQL user_like 双写 + 定时同步计数
│       └── FavoriteServiceImpl.java
├── dto/
│   └── CreateCommentDTO.java
├── vo/
│   └── CommentVO.java                  # 嵌套 replies 结构
└── task/
    └── LikeSyncTask.java               # @Scheduled 定时同步点赞数到 MySQL

echo-web/src/
├── components/
│   └── CommentList.vue                 # 嵌套评论组件
├── api/
│   └── comment.ts
```

---

## Sprint 5（2天）：搜索

- [ ] Elasticsearch 索引初始化（ik 分词器）
- [ ] 帖子发布时同步到 ES（直接同步，try-catch 兜底）
- [ ] 搜索接口（关键词 + 高亮 + 分页）
- [ ] 前端搜索页面

### Sprint 5 产出物

```text
echo-server/src/main/java/com/echospace/
├── config/
│   └── ElasticsearchConfig.java        # ES 客户端配置
├── service/
│   ├── SearchService.java
│   └── impl/SearchServiceImpl.java
├── repository/
│   └── PostDocumentRepository.java     # ES 操作封装
├── document/
│   └── PostDocument.java               # ES 索引映射实体
└── vo/
    └── SearchVO.java

echo-web/src/
├── views/
│   └── Search.vue
└── api/
    └── search.ts
```

---

## Sprint 6（1-2天）：关注 + 个人主页

- [ ] 关注/取消关注接口
- [ ] 粉丝列表 / 关注列表接口
- [ ] 用户个人主页接口（基本信息 + 统计数据）
- [ ] 用户帖子列表接口
- [ ] 关注时间线
- [ ] 前端个人主页 + 关注/粉丝页面

### Sprint 6 产出物

```text
echo-server/src/main/java/com/echospace/
├── controller/
│   └── UserController.java             # /api/users/{id}, /api/users/{id}/posts, /api/users/{id}/follow, followers, following
├── service/
│   ├── FollowService.java
│   ├── TimelineService.java
│   └── impl/
│       ├── FollowServiceImpl.java
│       └── TimelineServiceImpl.java
└── vo/
    ├── UserProfileVO.java
    ├── UserPostVO.java
    └── TimelineVO.java

echo-web/src/
├── views/
│   ├── UserProfile.vue                 # 个人主页
│   └── FollowList.vue                  # 关注/粉丝列表
└── api/
    └── user.ts
```

---

## Sprint 7（后续，第三阶段）：Docker 容器化 + 部署上线

- [ ] 学习 Docker 基础（镜像、容器、Dockerfile、Docker Compose）
- [ ] 编写 MySQL、Redis、ES、MinIO 的 Docker Compose 编排文件
- [ ] 编写 Spring Boot 和 Vue 项目的 Dockerfile
- [ ] Nginx 反向代理配置 + HTTPS
- [ ] MinIO 数据迁移到阿里云 OSS（第二阶段）
- [ ] 性能测试

### Sprint 7 产出物

```text
docker/
├── docker-compose.yml                  # 基础服务编排
├── docker-compose.prod.yml             # 生产环境编排
├── Dockerfile.server                   # 后端镜像
├── Dockerfile.web                      # 前端镜像
└── nginx.conf                          # Nginx 反向代理
```

---

## Sprint 全景图

| Sprint | 周期 | 主题 | 模块 | 累计接口数 |
|--------|------|------|------|-----------|
| 1 | 1-2天 | 项目脚手架 | 基础框架、数据库 | 0 |
| 2 | 2-3天 | 用户认证与账号管理 | 注册/登录/JWT/账号设置/改密 | 7 |
| 3 | 2-3天 | 帖子核心 | 帖子 CRUD + 图片上传 + 富文本 | 14 |
| 4 | 2天 | 评论+互动 | 评论/点赞/收藏 | 22 |
| 5 | 2天 | 搜索 | ES 全文搜索 | 23 |
| 6 | 1-2天 | 关注+主页 | 关注/粉丝/个人主页/时间线 | 28 |
| 7 | 后续 | 部署上线 | Docker/Nginx/OSS 迁移 | 28 |
