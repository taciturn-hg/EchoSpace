# EchoSpace 功能模块与流程图

## 一、功能模块总览

### 1.1 第一阶段（MVP — 最小可行产品）

| 模块 | 功能点 | 涉及接口 | 优先级 |
|------|--------|----------|--------|
| **用户系统** | 注册、登录、JWT 认证、个人信息编辑 | 4 个（auth 模块） | P0 |
| **用户资料** | 个人主页、账号设置、编辑资料、修改密码、用户帖子列表 | 5 个（users 模块） | P0 |
| **帖子系统** | 发布（Tiptap 富文本 + 图片）、编辑、软删除、游标分页列表（无限滚动）、帖子详情 | 5 个（post CRUD + list） | P0 |
| **图片上传** | 上传帖子图片、上传头像、图片回显、文件类型/大小校验 | 2 个 | P0 |
| **评论系统** | 一级评论 + 二级回复、评论分页、删除 | 4 个 | P0 |
| **点赞系统** | 帖子点赞/取消、评论点赞/取消、点赞数展示（Redis Set + MySQL） | 2 个 | P0 |
| **收藏系统** | 收藏帖子、取消收藏、收藏列表 | 2 个 | P1 |
| **搜索系统** | 帖子标题+内容全文搜索（ES + ik 分词）、关键词高亮 | 1 个 | P1 |

> 第一阶段共 25 个接口，覆盖完整的内容生产→消费闭环。

### 1.2 第二阶段（社交增强）

| 模块 | 功能点 | 涉及接口 | 优先级 |
|------|--------|----------|--------|
| **关注系统** | 关注/取消关注用户、关注列表、粉丝列表 | 3 个 | P2 |
| **时间线** | 关注用户的帖子时间线（按时间倒序） | 1 个（复用 posts list） | P2 |
| **通知系统** | 评论通知、点赞通知、关注通知（RabbitMQ 异步推送） | 新增 0 个（MQ 消费端） | P2 |
| **个人主页** | 用户主页（帖子列表、基本信息、统计数据） | 0 个（前端页面，后端接口已在第一阶段完成） | P1 |
| **对象存储升级** | MinIO 切换为阿里云 OSS | 0（新增 OssFileServiceImpl，按配置切换实现） | P2 |

> 第二阶段共新增 3 个接口，2 个消费端任务，主要增强社交属性。

### 1.3 第三阶段（运维 + 体验优化）

| 模块 | 功能点 | 涉及接口 | 优先级 |
|------|--------|----------|--------|
| **Docker 容器化** | Dockerfile、Docker Compose 编排所有基础服务、生产环境部署 | 0 | P3 |
| **热门帖子** | 基于 Redis ZSet 的热度排序（点赞+评论+时间加权） | 1 个（posts?sort=hot） | P3 |
| **内容审核** | 敏感词过滤（DFA 算法）、违规内容举报 | 1 个 | P3 |
| **草稿箱** | 帖子草稿自动保存（localStorage + Redis） | 1 个 | P3 |
| **@用户** | 评论中 @用户 并通知 | 0（复用评论接口 + 解析逻辑） | P3 |

---

## 二、功能流程图

> 以下流程图使用 Mermaid 绘制，VS Code 安装 Markdown Preview Mermaid Support 插件可预览，GitHub 原生支持。

### 2.1 用户注册/登录流程

```mermaid
flowchart TD
    A[访问网站] --> B{localStorage 有 Token?}
    B -->|有| C[携带 Token 请求 GET /api/auth/me]
    C --> D{Token 有效?}
    D -->|有效| E[跳转首页]
    D -->|过期| F[尝试 POST /api/auth/refresh]
    F --> G{RefreshToken 有效?}
    G -->|有效| H["更新 localStorage 中的 Token"]
    H --> E
    G -->|过期| I["清除 localStorage<br>跳转登录页"]
    B -->|无| I

    I --> J[显示登录页]
    J --> K{已注册?}
    K -->|否| L[点击去注册]
    L --> M[填写注册信息]
    M --> N[提交 POST /api/auth/register]
    N --> O{校验通过?}
    O -->|否| P[返回错误提示]
    P --> M
    O -->|是| Q[BCrypt 加密密码]
    Q --> R[写入 MySQL user 表]
    R --> S[返回注册成功]
    S --> J

    K -->|是| T["输入 用户名/邮箱/手机号 + 密码"]
    T --> U[提交 POST /api/auth/login]
    U --> V["后端查询 user 表<br>匹配 username/email/phone"]
    V --> W{校验密码?}
    W -->|否| X[返回错误提示]
    X --> T
    W -->|是| Y[生成 AccessToken + RefreshToken]
    Y --> Z[返回 Tokens 给前端]
    Z --> AA[localStorage 存储 Token]
    AA --> E
```

### 2.2 请求鉴权流程

```mermaid
flowchart TD
    A[客户端发起请求] --> B[Axios 拦截器]
    B --> C{请求路径在白名单?}
    C -->|是| D[直接发送]
    C -->|否| E{AccessToken 存在?}
    E -->|否| F[跳转登录页]
    E -->|是| G{AccessToken 过期?}
    G -->|否| H[Header 带 Authorization: Bearer token]
    G -->|是| I[POST /api/auth/refresh]
    I --> J{RefreshToken 有效?}
    J -->|是| K[获取新 Token 对]
    K --> H
    J -->|否| F

    H --> L[请求到达后端]
    L --> M[SecurityFilterChain]
    M --> N{路径在白名单?}
    N -->|是| O[放行到 Controller]
    N -->|否| P[JwtAuthFilter.doFilterInternal]
    P --> Q[从 Header 取 Token]
    Q --> R[解析 JWT 拿到 userId + username]
    R --> S{用户状态正常?}
    S -->|否| T[返回 401]
    S -->|是| U[写入 SecurityContextHolder]
    U --> O
```

### 2.3 发帖流程（含图片上传）

```mermaid
flowchart TD
    A[用户进入发帖页] --> B[Tiptap 编辑器加载]
    B --> C[用户编辑内容]
    C --> D{操作类型}
    
    D -->|粘贴/拖入图片| E[Tiptap Image Extension 拦截]
    E --> F[FormData 包装图片]
    F --> G[POST /api/upload/image]
    G --> H[后端校验文件类型+大小]
    H --> I{校验通过?}
    I -->|否| J[返回错误信息]
    J --> E
    I -->|是| K[上传到 MinIO]
    K --> L[返回图片 URL]
    L --> M[Tiptap 插入 img 标签到编辑器]
    M --> C

    D -->|点击发布| N[前端提取 contentHtml]
    N --> O[提交 POST /api/posts]
    O --> P[后端 jsoup 清洗 HTML 防 XSS<br/>并提取纯文本存入 content_text]
    P --> R[写入 MySQL post 表]
    R --> S["同步写入 ES 索引<br/>（try-catch 兜底，失败不影响主流程）"]
    S --> T[返回帖子ID]
    T --> U[前端跳转帖子详情页]
```

### 2.4 文件上传流程（MinIO / OSS 两阶段）

> 文件上传模块按部署阶段区分存储后端：第一阶段本地 MinIO，第二阶段切换阿里云 OSS。业务侧通过 `FileService` 接口统一抽象，`application.yml` 中 `storage.type` 配置项决定注入 `MinioFileServiceImpl` 还是 `OssFileServiceImpl`；上层调用方只依赖接口，无需感知底层实现。

```mermaid
flowchart TD
    A[用户上传图片] --> B{上传场景?}
    B -->|帖子图片| C[Tiptap Image Extension 拦截]
    B -->|用户头像| D[头像上传组件]

    C --> E["前端校验<br>类型: jpg/png/gif/webp, ≤10MB"]
    D --> F["前端校验<br>类型: jpg/png, ≤2MB"]

    E --> G{校验通过?}
    F --> G
    G -->|否| H[前端提示错误信息]
    H --> A

    G -->|是| I[构建 FormData 包装文件]
    I --> J{上传类型?}
    J -->|帖子图片| K[POST /api/upload/image]
    J -->|用户头像| L[POST /api/upload/avatar]

    K --> M["后端校验<br>文件类型（魔数）+ 大小 + 扩展名"]
    L --> M

    M --> N{校验通过?}
    N -->|否| O["返回 Result(code=0, msg=错误原因)"]
    O --> A

    N -->|是| P["生成存储路径<br>images: echospace/images/{yyyy}/{MM}/{uuid}.{ext}<br>avatars: echospace/avatars/{userId}/{uuid}.{ext}"]

    P --> Q{当前存储方案<br>application.yml 配置?}

    Q -->|"第一阶段: MinIO"| R["MinIOClient.putObject()<br>本地 http://localhost:9000"]
    R --> S["返回 URL<br>http://localhost:9000/{bucket}/{path}"]

    Q -->|"第二阶段: 阿里云 OSS"| T["OSSClient.putObject()<br>Endpoint: oss-cn-xxx.aliyuncs.com"]
    T --> U["返回 URL<br>https://{bucket}.oss-cn-xxx.aliyuncs.com/{path}"]

    S --> V["封装 Result(code=1, data={url}) 返回前端"]
    U --> V

    V --> W{上传场景?}
    W -->|帖子图片| X["Tiptap 插入 img 标签<br>图片即时回显在编辑器"]
    W -->|用户头像| Y["更新 user.avatar 字段<br>头像即时回显"]

    X --> Z1[用户继续编辑帖子]
    Y --> Z2[用户继续编辑资料]
```

> **切换要点**：MinIO Java SDK 兼容 S3 协议，但阿里云 OSS 官方 Java SDK 使用自有 API 而非 S3 协议。因此不能简单通过改配置切换，需要在 Service 层定义 `FileService` 接口，分别提供 `MinioFileServiceImpl` 和 `OssFileServiceImpl` 两种实现，由 `storage.type` 配置决定注入哪个 Bean。已有图片数据通过 `mc mirror` 命令从 MinIO 同步到 OSS。

### 2.5 评论流程（一级评论 + 二级回复）

```mermaid
flowchart TD
    A[用户浏览帖子详情] --> B[加载评论列表]
    B --> C["GET /api/posts/{postId}/comments?page=1&size=10&replySize=3"]
    C --> D[后端查询一级评论]
    D --> E[对每条一级评论查询最多3条二级回复]
    E --> F[组装嵌套 JSON 返回]

    F --> G[前端渲染评论列表]
    G --> H{用户操作}

    H -->|写一级评论| I[输入框输入内容]
    I --> J["POST /api/posts/{postId}/comments<br>parentId=0"]
    J --> K[写入 comment 表]
    K --> L[更新 post.comment_count +1]
    L --> M["MQ 发送评论通知<br/>（Phase 2+）"]
    M --> N[刷新评论列表]

    H -->|回复某条评论| O[点击回复按钮]
    O --> P[展开回复输入框]
    P --> Q{被回复的是二级回复?}
    Q -->|是| Q1["输入框自动补全 @昵称<br>（前端根据 replyToUser 自动填充）"]
    Q -->|否（一级评论）| Q2["输入框直接输入内容<br>（无需 @昵称）"]
    Q1 --> R
    Q2 --> R["POST /api/posts/{postId}/comments<br>parentId=xxx, replyToUid=xxx"]
    R --> S[写入 comment 表]
    S --> T[更新 post.comment_count +1]
    T --> U["MQ 发送回复通知<br/>（Phase 2+）"]
    U --> V[刷新该条评论的 replies]

    H -->|查看更多回复| W["点击查看更多回复"]
    W --> X["GET /api/comments/{commentId}/replies?page=2"]
    X --> Y[追加渲染二级回复]

    H -->|删除自己的评论| Z["DELETE /api/comments/{id}"]
    Z --> AA[软删除 status=-1]
    AA --> AB{是一级评论?}
    AB -->|是| AC[级联软删除所有二级回复]
    AB -->|否| AD[仅删除该条回复]
    AC --> N
    AD --> N
```

### 2.6 点赞/取消流程

```mermaid
flowchart TD
    A[用户点击点赞按钮] --> B{点赞对象类型?}
    B -->|帖子| C["POST /api/posts/{id}/like"]
    B -->|评论| D["POST /api/comments/{id}/like"]

    C --> E
    D --> E[后端处理]
    E --> F["SADD Redis Set<br>post:like:{targetId} {userId}"]
    F -->|返回1 首次点赞| G[点赞成功]
    F -->|返回0 已点过| H[SREM Redis Set<br>取消点赞]

    G --> G2["INSERT INTO user_like<br/>（持久化点赞关系）"]
    H --> H2["DELETE FROM user_like<br/>（移除点赞记录）"]

    G2 --> I["返回 {liked:true, likeCount}"]
    H2 --> J["返回 {liked:false, likeCount}"]

    I --> K[前端更新 UI: 图标亮起, 数字+1]
    J --> L[前端更新 UI: 图标变灰, 数字-1]

    M[定时任务 @Scheduled<br>每5分钟执行] --> N[遍历 Redis 所有 like key]
    N --> O[SCARD 获取点赞数]
    O --> P[UPDATE post/comment SET like_count = count]
    P --> Q{version 乐观锁<br>affected_rows=0?}
    Q -->|是 冲突| R[重试]
    R --> O
    Q -->|否 成功| S[同步完成]
```

### 2.7 收藏/取消流程

```mermaid
flowchart TD
    A[用户点击收藏按钮] --> B["POST /api/posts/{id}/favorite"]
    B --> C{查询 user_favorite 表<br>user_id + post_id 是否存在}
    C -->|不存在| D[INSERT INTO user_favorite]
    D --> E[UPDATE post SET collect_count = collect_count + 1]
    E --> F["返回 {favorited: true}"]
    F --> G[前端 UI: 收藏图标亮起]

    C -->|已存在| H[DELETE FROM user_favorite]
    H --> I[UPDATE post SET collect_count = collect_count - 1]
    I --> J["返回 {favorited: false}"]
    J --> K[前端 UI: 收藏图标变灰]

    L[用户进入收藏列表] --> M[GET /api/posts/favorites?page=1]
    M --> N[JOIN user_favorite + post 分页查询]
    N --> O[返回收藏的帖子列表]
```

### 2.8 搜索流程

```mermaid
flowchart TD
    A[用户在搜索框输入关键词] --> B{搜索方式?}
    
    B -->|实时建议| C[防抖 300ms]
    C --> D[GET /api/posts/search?q=xxx&size=5]
    D --> E[ES suggest/multi_match 查询]
    E --> F[返回建议列表]
    F --> G[前端下拉展示搜索建议]

    B -->|回车搜索| H[GET /api/posts/search?q=xxx&page=1&size=10]
    H --> I[ES multi_match 查询<br>title + content, ik 分词]
    I --> J[ES highlight 返回<br>关键词用 em 标签包裹]
    J --> K[返回搜索结果 + 分页信息]
    K --> L[前端渲染搜索结果<br>关键词高亮显示]

    L --> M{用户操作}
    M -->|翻页| H
    M -->|点击结果| N[跳转帖子详情页]
    M -->|修改关键词| A
```

### 2.9 关注/取关流程

```mermaid
flowchart TD
    A[用户访问对方主页] --> B{当前关注状态?}
    B -->|未关注| C[按钮显示: 关注]
    B -->|已关注| D[按钮显示: 已关注]

    C --> E[点击关注]
    D --> F[点击取消关注]

    E --> G["POST /api/users/{id}/follow"]
    F --> G

    G --> H{查询 user_follow 表<br>follower_id + followed_id}
    H -->|不存在| I[INSERT INTO user_follow]
    I --> J["返回 {followed: true}"]
    J --> K[按钮变为: 已关注]

    H -->|已存在| L[DELETE FROM user_follow]
    L --> M["返回 {followed: false}"]
    M --> N[按钮变为: 关注]

    O[用户查看关注列表] --> P["GET /api/users/{id}/following"]
    P --> Q[查询 user_follow WHERE follower_id = id]

    R[用户查看粉丝列表] --> S["GET /api/users/{id}/followers"]
    S --> T[查询 user_follow WHERE followed_id = id]
```

### 2.10 个人主页 + 时间线流程

```mermaid
flowchart TD
    A[用户访问个人主页] --> B["GET /api/users/{id}"]
    B --> C[查询 user 表基本信息]
    C --> D[统计 postCount / followerCount / followingCount]
    D --> E[返回用户资料]

    E --> F["GET /api/users/{id}/posts?size=10"]
    F --> G[游标分页查询该用户的帖子列表]
    G --> H[渲染帖子列表，滚动到底部加载更多]

    I[用户访问首页时间线] --> J{已登录?}
    J -->|是| K["GET /api/posts?sort=created_at<br>过滤关注用户的帖子"]
    K --> L[JOIN user_follow + post<br>WHERE follower_id = currentUserId]
    L --> M[游标分页，按时间倒序返回]

    J -->|否/未关注任何人| N["GET /api/posts?sort=created_at"]
    N --> O[全站最新帖子列表（无限滚动）]
```

### 2.11 系统全景流程

```mermaid
flowchart LR
    subgraph 前端
        A1[登录注册]
        A2[首页帖子列表]
        A3[帖子发布/编辑]
        A4[帖子详情]
        A5[评论互动]
        A6[搜索页]
        A7[个人主页]
    end

    subgraph 网关层
        B1[Nginx<br>反向代理 + 静态资源]
    end

    subgraph 后端服务
        C1[SecurityFilterChain<br>白名单放行]
        C2[JwtAuthFilter<br>JWT 解析 + 鉴权]
        C3[Controller 层<br>接收请求 + 参数校验]
        C4[Service 层<br>业务逻辑编排]
        C5[Mapper 层<br>MyBatis-Plus 数据访问]
    end

    subgraph 中间件
        D1[(MySQL<br>主数据库)]
        D2[(Redis<br>缓存/点赞/锁)]
        D3[(MinIO / OSS<br>文件存储)]
        D4[(Elasticsearch<br>全文搜索)]
        D5[[RabbitMQ<br>异步消息]]
    end

    前端 --> 网关层
    网关层 --> 后端服务
    C3 --> C4
    C4 --> D1
    C4 --> D2
    C4 --> D3
    C4 --> D4
    C4 -.-> D5
    D5 -.-> D4
```

---

## 三、核心数据流向

### 3.1 写入路径（以发帖为例）

```text
用户 → Vue → Axios → Nginx → SecurityFilterChain → JwtAuthFilter
     → PostController → PostService
         ├─ HTML 清洗: jsoup 白名单过滤 + 提取纯文本
         ├─ 图片上传: FileService → MinIO → 返回 URL
         ├─ 数据写入: PostMapper → MySQL (post 表)
         ├─ 缓存失效: Redis DEL post:detail:{postId}
         └─ 搜索同步: RestClient → ES 索引（try-catch 兜底）
     → 返回 PostID → Vue 跳转详情页
```

### 3.2 读取路径（以帖子详情为例）

```text
用户 → Vue → Axios → Nginx → JwtAuthFilter
     → PostController → PostService
         ├─ 查 Redis: GET post:detail:{postId}
         │    ├─ 命中 → 直接返回
         │    └─ 未命中 ↓
         ├─ 查 MySQL: PostMapper.selectById(id)
         ├─ 查点赞状态: Redis SISMEMBER post:like:{postId} {userId}
         ├─ 查收藏状态: MySQL user_favorite
         ├─ 查关注状态: MySQL user_follow
         ├─ 写 Redis: SET post:detail:{postId} ... EX 600
         └─ 组装 PostDetailVO 返回
     → Vue 渲染详情页
```

### 3.3 点赞数据一致性策略

```text
┌─ 实时 ──────────────────────────────────────┐
│ Redis SADD/SREM (原子，毫秒级)               │
│ MySQL INSERT/DELETE user_like (持久化关系)    │
│ 前端即时展示点赞状态变更                     │
└────────────────────────────────────────────┘
         │
         ▼ (每5分钟)
┌─ 近实时同步 ────────────────────────────────┐
│ @Scheduled 定时任务                         │
│ SCARD 获取 Redis 点赞数                      │
│ UPDATE post/comment SET like_count = ?      │
│   WHERE id = ? AND version = ?              │
│ (乐观锁保证并发安全)                         │
└────────────────────────────────────────────┘
```

---

## 四、模块依赖关系

```mermaid
flowchart TD
    A[用户系统] --> B[帖子系统]
    A --> C[评论系统]
    A --> D[点赞系统]
    A --> E[收藏系统]
    A --> F[搜索系统]
    A --> G[关注系统]
    A --> H[通知系统]

    B --> C
    B --> D
    B --> E
    B --> F

    C --> D
    C --> H

    G --> I[时间线]
    I --> B

    D --> H

    B --> J[图片上传]
    J --> K[MinIO/OSS]

    B -.-> L[内容安全<br/>(Phase 3)]
    L -.-> B
```

> **说明**：箭头表示"依赖/使用"方向。例如帖子系统依赖用户系统（需要知道谁发的），评论依赖帖子（需要知道评论哪篇）。

---

## 五、接口与模块映射

完整接口定义见 [API 接口文档](./02-api-documentation.md)，此处仅做模块→接口的快速索引。

| 模块 | 涉及接口序号 | 接口数 |
|------|-------------|--------|
| 认证 | #1 ~ #4 | 4 |
| 用户 | #5 ~ #12 | 8 |
| 帖子 | #13 ~ #21 | 9 |
| 评论 | #22 ~ #26 | 5 |
| 文件上传 | #29 ~ #30 | 2 |

---

## 六、数据表与模块映射

完整表结构见 [数据库设计文档](./03-database-design.md)。

| 表名 | 关联模块 |
|------|----------|
| `user` | 认证、用户、关注 |
| `post` | 帖子、搜索、收藏 |
| `comment` | 评论、通知 |
| `user_like` | 点赞（帖子+评论通用） |
| `user_favorite` | 收藏 |
| `user_follow` | 关注 |
