# EchoSpace 社交社区项目 — 需求分析与技术方案

## 一、项目定位

**EchoSpace** 是一个面向用户的社交社区平台，功能形态类似贴吧/论坛，支持用户注册登录、发布富文本帖子、评论互动（一级评论+二级回复）、点赞收藏、全文搜索，以及基础的关注系统。项目采用前后端分离架构，目标是作为个人练手项目、简历项目，并最终部署到服务器上线运行。

---

## 二、技术栈选型

| 层次 | 技术 | 说明 |
|------|------|------|
| **运行时** | JDK 17 + Node.js 24 | 后端/前端运行时环境 |
| **后端框架** | Spring Boot 3.x | 主流 Java 后端框架，生态成熟 |
| **前端框架** | Vue 3 + Vite | 渐进式前端框架，上手友好 |
| **前端 UI** | Element Plus | 成熟的 Vue 3 组件库 |
| **CSS 预处理器** | SCSS | 变量、嵌套、混入，提升样式可维护性 |
| **富文本编辑器** | Tiptap | 基于 ProseMirror，Vue 3 原生支持，插件化架构 |
| **认证方案** | JWT（jjwt + Spring Security） | 无状态认证，适合前后端分离 |
| **数据库** | MySQL 8.0 | 主力关系型存储 |
| **缓存** | Redis 7 | 缓存/点赞/分布式锁/排行榜 |
| **搜索引擎** | Elasticsearch 8 | 帖子全文搜索，ik 中文分词 |
| **对象存储（第一阶段）** | MinIO（本地启动） | 兼容 S3 API，零成本本地开发，直接下载二进制启动 |
| **对象存储（第二阶段）** | 阿里云 OSS | 线上部署后切换，新增 OssFileServiceImpl 实现 FileService 接口 |
| **消息队列** | RabbitMQ（预留） | 异步解耦，ES 数据同步、通知推送 |
| **容器化（第三阶段）** | Docker + Docker Compose | 学完 Docker 后，将所有基础服务容器化，便于部署 |

### 图片存储的阶段式策略

| 阶段 | 方案 | 说明 |
|------|------|------|
| **第一阶段（开发+MVP）** | MinIO（本地直接启动） | 下载 MinIO 二进制文件直接运行，无需 Docker |
| **第二阶段（上线部署）** | 阿里云 OSS | 部署到服务器后切换为 OSS。MinIO SDK 兼容 S3 协议，但 OSS 官方 SDK 使用自有 API，无法通过改配置直接切换；需新增 OssFileServiceImpl 实现 FileService 接口，由 storage.type 配置决定注入哪个实现 |

切换时需要做的改动：
- `application.yml` 中修改 `storage.type` 配置（minio → oss），并配置 OSS 的 endpoint、accessKey、secretKey
- 新增 `OssFileServiceImpl` 实现 `FileService` 接口，上层业务代码无需改动（依赖接口而非实现）
- 已有图片数据需要迁移：用 `mc mirror` 命令从 MinIO 同步到 OSS

### 为什么不选别的

- **RabbitMQ vs Kafka**：这个项目的数据量级不需要 Kafka 的吞吐能力，RabbitMQ 更轻量、学习曲线更平缓，足够异步处理搜索索引同步和通知。
- **Tiptap vs Quill/Tinymce**：Tiptap 对 Vue 3 原生支持最好，插件化架构扩展性强，输出结构化 JSON 便于搜索和审核。详细对比见文档末尾附录。

---

## 三、功能模块拆解

> 完整功能模块总览、功能流程图、数据流向、模块依赖关系见 **[功能模块与流程图文档](./05-feature-flows.md)**。

---

## 四、数据库设计（MySQL）

> 完整数据库设计（表结构、字段、索引、设计要点、建表 SQL）见 **[数据库设计文档](./03-database-design.md)**。

---

## 五、Redis 使用场景（重点学习）

以下是 Redis 在这个项目中的具体应用场景：

### 5.1 缓存层

| 场景 | Key 设计 | 过期策略 |
|------|----------|----------|
| 热门帖子列表 | `hot:posts:{page}` | 5 分钟过期 |
| 用户信息 | `user:info:{userId}` | 更新用户信息时主动删除 |
| 帖子详情 | `post:detail:{postId}` | 10 分钟过期 |

### 5.2 点赞去重与计数

```text
// Redis Set 判断用户是否已点赞（原子操作）
SADD post:like:{targetId} {userId}   // 返回1=首次点赞，返回0=已经点过
SREM post:like:{targetId} {userId}   // 取消点赞

// 同步写入 MySQL user_like 表（持久化点赞关系，支持"我点赞过的内容"查询）
// INSERT INTO user_like (user_id, target_type, target_id) VALUES (?, ?, ?)
// 或 DELETE FROM user_like WHERE user_id = ? AND target_type = ? AND target_id = ?

// 定时任务：每5分钟将 Redis 点赞数同步到 MySQL 冗余计数字段
// 1. SCARD post:like:{targetId} 获取点赞总数
// 2. UPDATE post/comment SET like_count = {count} WHERE id = {targetId} AND version = ?
```

### 5.3 分布式锁

```text
// 场景：更新帖子计数等并发敏感操作
SET lock:post:update:{postId} {requestId} NX EX 10
// ... 执行业务逻辑 ...
// Lua 脚本校验 requestId 后 DEL（防止误删别人的锁）
```

### 5.4 排行榜（ZSet）

```text
// 热门帖子排行：score = 点赞*3 + 评论*2 + 收藏*5
ZADD hot:posts:rank {score} {postId}
ZREVRANGE hot:posts:rank 0 49   // 取 Top50
```

### 5.5 布隆过滤器（缓存穿透防护）

```text
// 查询前先判断 key 是否存在
// 比如查询不存在的帖子ID时，布隆过滤器直接返回"不存在"，不穿透到 MySQL
```

### 5.6 接口限流

```text
// 发帖限流：每人每分钟最多发3帖
INCR rate:post:{userId}
EXPIRE rate:post:{userId} 60
// 如果返回值 > 3，拒绝请求
```

---

## 六、数据库锁机制学习

### 6.1 乐观锁

用在帖子表的 `version` 字段（见 [数据库设计文档](./03-database-design.md) post 表）：

```sql
-- 更新帖子浏览量（MyBatis-Plus 自带乐观锁支持）
UPDATE post
SET view_count = view_count + 1, version = version + 1
WHERE id = #{id} AND version = #{oldVersion};
-- affected_rows = 0 说明被其他线程改过，重试即可
```

适用场景：并发编辑概率低的操作（浏览数、帖子编辑）

### 6.2 悲观锁（SELECT ... FOR UPDATE）

```sql
-- 确保点赞数同步时读到的是最新值
SELECT like_count FROM post WHERE id = #{id} FOR UPDATE;
-- 计算并更新...
UPDATE post SET like_count = #{newCount} WHERE id = #{id};
```

适用场景：需要绝对数据一致性的操作

### 6.3 推荐策略总结

| 场景 | 锁策略 | 理由 |
|------|--------|------|
| 点赞/收藏去重 | Redis Set（天然去重） + 同步写入 MySQL user_like 表 + 定时同步计数 | 高并发，Redis 扛实时去重，MySQL 持久化关系，计数最终一致 |
| 帖子计数更新 | 乐观锁（version 字段） | 避免行锁竞争，偶尔重试即可 |
| 关键数据扣减 | 悲观锁（FOR UPDATE） | 需要强一致性 |
| 帖子编辑 | 乐观锁 | 并发编辑概率很低 |

---

## 七、Elasticsearch 搜索设计

### 7.1 索引映射

```json
{
  "mappings": {
    "properties": {
      "id":          { "type": "long" },
      "title":       { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "content":     { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "username":    { "type": "keyword" },
      "nickname":    { "type": "keyword" },
      "created_at":  { "type": "date" },
      "like_count":  { "type": "integer" },
      "comment_count": { "type": "integer" }
    }
  }
}
```

### 7.2 数据同步策略

第一阶段采用直接同步（try-catch 兜底，失败不影响主流程），后续引入 RabbitMQ 后改为异步：

```text
帖子发布/更新 → MySQL 写入成功 → RestClient 同步写入 ES 索引（try-catch）
```

后续 MQ 异步方案（第二阶段引入）：
```text
帖子发布/更新 → MySQL 写入成功 → 发消息到 RabbitMQ（post.sync.queue）
                                      ↓
ES 同步消费者 ← 消费消息 ← 更新 ES 索引
```

优点：解耦，MySQL 写入不受 ES 同步速度影响。

### 7.3 搜索功能清单

- 关键词搜索（标题 + 内容，ik 中文分词）
- 搜索建议（输入时实时提示热门搜索词）
- 搜索结果中关键词高亮
- 按时间/热度 排序
- 搜索结果分页

---

## 八、系统架构图

```text
┌─────────────────────────────────────────────────────┐
│                    前端 (Vue 3)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│  │ 登录注册  │ │ 帖子模块  │ │ 用户中心  │             │
│  └──────────┘ └──────────┘ └──────────┘             │
└──────────────┬──────────────────────────────────────┘
               │ HTTP (Axios) + JWT Header
┌──────────────▼──────────────────────────────────────┐
│              Nginx (反向代理 + 静态资源)                │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│           Spring Boot (后端服务)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│  │ 认证过滤器│ │ 业务服务  │ │ 文件服务  │             │
│  └──────────┘ └──────────┘ └──────────┘             │
└───┬─────────┬─────────┬─────────┬───────────────────┘
    │         │         │         │
┌───▼───┐ ┌──▼──┐ ┌───▼────┐ ┌─▼──────────┐
│ MySQL │ │Redis│ │  MinIO │ │Elasticsearch│
└───────┘ └─────┘ └────────┘ └─────────────┘
      (第三阶段) ┌──────────┐
               │ RabbitMQ  │
               └───────────┘
```

> 说明：第一阶段所有基础服务（MySQL、Redis、ES、MinIO）都在本地直接启动；第三阶段学习 Docker 后，统一迁移到 Docker Compose 编排。RabbitMQ 按需引入。Nginx 在部署上线时配置。

---

## 九、认证鉴权设计（方案C：Spring Security 轻量集成）

### 9.1 方案选择

采用 **Spring Security 壳 + 自定义 JWT Filter** 的混合方案，不用 UserDetailsService、不用 AuthenticationProvider。

- **第一阶段**：SecurityFilterChain 控制哪些路径放行 + 自定义 OncePerRequestFilter 做 JWT 解析 → 存入 SecurityContextHolder → Controller 通过 `@AuthenticationPrincipal` 或手动从 SecurityContextHolder 获取当前用户
- **第二阶段**：按需引入方法级注解 `@PreAuthorize`、角色权限体系（RBAC）

### 9.2 JWT 认证流程

```text
1. 用户登录        → 后端验证账号+密码（支持用户名/邮箱/手机号） → 返回 AccessToken(30min) + RefreshToken(7d)
2. 前端存储 Token  → localStorage
3. 后续请求        → Axios 拦截器自动加 Header: Authorization: Bearer {accessToken}
4. AccessToken过期 → 前端用 RefreshToken 换新的 AccessToken（POST /api/auth/refresh）
5. RefreshToken也过期 → 清除登录态，跳转登录页
```

JWT Payload 设计：

```json
{
  "sub": "用户ID",
  "username": "用户名",
  "type": "access | refresh",
  "iat": "签发时间戳",
  "exp": "过期时间戳"
}
```

### 9.3 鉴权链路架构

```text
请求 → SecurityFilterChain（白名单放行，其余拦截）
     → JwtAuthFilter（OncePerRequestFilter）
          ├─ 从 Header 取 Token
          ├─ 解析 JWT，拿到 userId + username
          ├─ 查 Redis/MySQL 确认用户状态（是否被禁用）
          ├─ 写入 SecurityContextHolder
          └─ 放行到 Controller
     → Controller
          └─ 从 SecurityContextHolder 取当前用户，执行业务逻辑
```

### 9.4 Spring Security 配置要点

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 白名单：无需认证（路径不含 context-path 前缀）
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll()
                // 其余全部需要认证
                .anyRequest().authenticated()
            )
            // 自定义 JWT Filter 插在 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

> **注意**：白名单路径不含 `context-path`（`/api`）前缀。`requestMatchers` 匹配的是 Servlet 路径，`server.servlet.context-path` 不影响其匹配逻辑，详见 Question.md 第 6 章。

### 9.5 自定义 JWT 过滤器

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 白名单路径直接跳过，不进行 JWT 解析
        String servletPath = request.getServletPath();
        return whitelist.stream().anyMatch(p -> PATH_MATCHER.match(p, servletPath));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 从 Header 取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);

        // 2. 解析 JWT → userId + username，异常时返回 401
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            writeUnauthorized(response, "Token已过期");
            return;
        } catch (JwtException e) {
            writeUnauthorized(response, "Token无效");
            return;
        }

        // 3. 校验 Token 类型，必须是 access
        if (!JwtUtil.TokenType.ACCESS.claimValue().equals(claims.get("type", String.class))) {
            writeUnauthorized(response, "Token类型错误，请使用AccessToken");
            return;
        }

        // 4. 写入 SecurityContextHolder（userId + username 封装进 UserPrincipal）
        UserPrincipal principal = new UserPrincipal(
            Long.valueOf(claims.getSubject()),
            claims.get("username", String.class)
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
```

### 9.6 Controller 获取当前用户

通过 `SecurityUtil` 工具类从 `SecurityContextHolder` 取当前用户，`userId` 和 `username` 封装在 `UserPrincipal` record 中：

```java
// SecurityUtil.java
public class SecurityUtil {
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        if (auth.getPrincipal() instanceof UserPrincipal up) return up.userId();
        return null;
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        if (auth.getPrincipal() instanceof UserPrincipal up) return up.username();
        return null;
    }
}

// UserPrincipal.java — 存入 principal 的自定义主体
public record UserPrincipal(Long userId, String username) {}
```

Controller 中直接调用：

```java
@PostMapping("/api/posts")
public Result<Void> createPost(@RequestBody CreatePostDTO dto) {
    Long userId = SecurityUtil.getCurrentUserId();
    // 业务逻辑...
}
```

> **注意**：`username` 存入 `UserPrincipal.principal` 而非 `details`，避免被 Spring Security 内置组件覆盖，详见 Question.md 5.12。

### 9.7 关键类一览

| 类 | 职责 | 第一阶段需掌握 |
|----|------|---------------|
| `SecurityConfig` | 配置白名单路径、关闭 CSRF、关闭 Session、注册 JWT Filter | 必须 |
| `JwtAuthFilter`（继承 OncePerRequestFilter） | 解析 JWT → 查用户状态 → 写入 SecurityContextHolder | 必须 |
| `JwtUtil` | JWT 生成、解析、校验过期 | 必须 |
| `SecurityContextHolder` | 存取当前请求的用户信息 | 必须 |
| `SecurityUtil`（工具类） | 封装取当前用户的逻辑 | 推荐 |
| `UserDetailsService` | Spring Security 用户加载接口 | **第一阶段不用** |
| `AuthenticationProvider` | 认证提供者 | **第一阶段不用** |
| `@PreAuthorize` | 方法级权限注解 | **第二阶段引入** |

### 9.8 与方案B（手写 Filter+Interceptor）的对比

| 维度 | 方案B | 方案C（推荐） |
|------|-------|--------------|
| 第一阶段代码量 | 约3个类 | 约3~4个类（差不多） |
| 需要理解的概念 | Filter、Interceptor 基础 | SecurityFilterChain、OncePerRequestFilter |
| 第二阶段加角色鉴权 | 需要重写拦截器链 | 直接加 `@PreAuthorize` 注解 |
| 简历含金量 | 一般 | 更高 |
| 代码迁移成本 | 切 Security 时需重写鉴权层 | 无需迁移，本身就是 Security 体系 |

---

## 十、API 接口文档

> 完整 API 文档（通用说明、认证模块、用户模块、帖子模块、评论模块、文件上传模块、接口汇总表）见 **[API 接口文档](./02-api-documentation.md)**。

---

## 十一、开发阶段规划

> 完整开发阶段规划（Sprint 1~7 任务清单、每个 Sprint 的产出物目录结构、Sprint 全景图）见 **[开发阶段规划文档](./04-development-plan.md)**。

---

## 十二、项目结构规划

```text
EchoSpace/
├── echo-server/                          # Spring Boot 后端
│   ├── src/main/java/com/echospace/
│   │   ├── config/                       # Security, Redis, ES, MinIO 配置
│   │   ├── controller/                   # 控制器层
│   │   ├── service/                      # 业务接口
│   │   ├── service/impl/                 # 业务实现
│   │   ├── mapper/                       # MyBatis-Plus Mapper
│   │   ├── entity/                       # 数据库实体
│   │   ├── dto/                          # 请求 DTO
│   │   ├── vo/                           # 响应 VO
│   │   ├── common/                       # Result, PageResult, GlobalExceptionHandler
│   │   ├── security/                     # JwtFilter, SecurityConfig, JwtUtil
│   │   └── utils/                        # 工具类
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/                 # Flyway SQL
├── echo-web/                             # Vue 3 前端
│   ├── src/
│   │   ├── views/                        # 页面：Login, Register, Home, PostDetail, PostCreate, Search, UserProfile, Settings
│   │   ├── components/                   # Layout, PostCard, CommentList, RichTextEditor, NavBar, Avatar
│   │   ├── router/                       # 路由配置
│   │   ├── stores/                       # Pinia: userStore, postStore
│   │   ├── api/                          # API 类型 + 请求封装
│   │   │   └── modules/                  # 共享类型定义
│   │   └── utils/                        # 工具函数（Axios 实例等）
│   └── public/
├── docker/                               # Docker 相关（第三阶段学习后加入）
│   ├── docker-compose.yml                # 基础服务编排
│   ├── docker-compose.prod.yml           # 生产环境编排
│   ├── Dockerfile.server
│   ├── Dockerfile.web
│   └── nginx.conf
└── docs/
    ├── 01-requirements-and-plan.md       # 需求分析与技术方案（本文档）
    ├── 02-api-documentation.md           # API 接口文档
    ├── 03-database-design.md             # 数据库设计文档
    ├── 04-development-plan.md            # 开发阶段规划
    └── 05-feature-flows.md               # 功能模块与流程图
```

---

## 十三、关键依赖版本

### 后端（pom.xml 核心依赖）

**Spring Boot Starters**（版本由父工程 3.5.x 统一管理，无需手写版本号）：

| 依赖 | 说明 |
|------|------|
| spring-boot-starter-web | Spring MVC + Tomcat + Jackson（REST API 基础） |
| spring-boot-starter-security | Spring Security 6.x 安全框架 |
| spring-boot-starter-data-redis | Redis 集成（Lettuce 连接池） |
| spring-boot-starter-validation | 参数校验（@Valid / @NotBlank 等） |
| spring-boot-starter-test | 测试框架（JUnit 5 / Mockito / AssertJ） |
| spring-boot-starter-amqp | RabbitMQ 消息队列（按需引入） |

**第三方独立依赖**（需显式声明版本号）：

| 依赖 | 版本 | 说明 |
|------|------|------|
| mybatis-plus-spring-boot3-starter | 3.5.x | MyBatis-Plus ORM（含分页插件、乐观锁） |
| mybatis-plus-join-boot-starter | 1.4.x | MyBatis-Plus 连表查询 |
| mysql-connector-j | — | MySQL 8.0 驱动 |
| jjwt-api | 0.12.x | JWT 生成与解析（API 层） |
| jjwt-impl | 0.12.x | JWT 实现（runtime 必需） |
| jjwt-jackson | 0.12.x | JWT JSON 序列化（runtime 必需） |
| elasticsearch-java | 8.x | Elasticsearch 8.x 新版 Java 客户端 |
| minio | 8.x | MinIO 对象存储客户端（第一阶段） |
| aliyun-sdk-oss | 3.x | 阿里云 OSS 对象存储客户端（第二阶段） |
| jsoup | 1.22.x | HTML 白名单清洗 + 纯文本提取 |
| springdoc-openapi-starter-webmvc-ui | 2.8.x | OpenAPI 3 / Swagger UI 接口文档（开发环境） |
| flyway-core | — | 数据库版本迁移 |
| flyway-mysql | — | Flyway MySQL 8.0 支持（计划引入，当前暂未添加） |
| lombok | — | 简化 Getter/Setter/Builder 等样板代码 |
| spring-boot-starter-mail | — | 邮件发送（忘记密码重置链接，版本由父工程管理） |

> **说明**：`jjwt` 从 0.12.x 起拆分为三个独立模块，`jjwt-api` 是编译期接口，`jjwt-impl` 和 `jjwt-jackson` 是运行期必需实现，三个都要引入、版本保持一致。

### 前端（package.json 核心依赖）

**Vue 脚手架自带**（`npm create vue@latest` 勾选后自动安装）：

| 依赖 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| Vue Router | 5.x | 前端路由 |
| Pinia | 3.x | 状态管理 |
| Vite | 8.x | 构建工具（devDependency） |

**需手动安装**（`npm install`）：

| 依赖 | 版本 | 说明 |
|------|------|------|
| Axios | — | HTTP 请求 |
| pinia-plugin-persistedstate | — | Pinia 持久化插件 |
| Element Plus | 2.x | UI 组件库 |
| @tiptap/vue-3 | — | Tiptap Vue 3 集成 |
| @tiptap/starter-kit | — | Tiptap 基础扩展包 |
| @tiptap/extension-image | — | Tiptap 图片扩展 |
| DOMPurify | — | 前端 XSS 过滤（渲染前最后一道防线） |

> **注意**：DOMPurify 安装后不会自动生效，需在渲染富文本 HTML 的组件中手动调用：
> ```js
> import DOMPurify from 'dompurify';
> const sanitizedHTML = DOMPurify.sanitize(contentHtml);
> ```

---

## 十四、学习路径建议

建议在开发过程中按以下顺序理解和实践，循序渐进：

1. **先做基础 CRUD**：理解 MySQL 普通查询和索引使用
2. **开始做缓存**：把帖子详情、用户信息用 Redis 缓存 → 理解缓存穿透/击穿/雪崩
3. **做点赞功能**：用 Redis Set 做去重 → 理解 Redis 原子操作
4. **做计数同步**：Redis 定时同步 MySQL → 理解乐观锁（version 字段）
5. **做排行榜**：Redis ZSet 的热度排序 → 理解有序集合的 score 机制
6. **加分布式锁**：并发敏感操作加锁 → 理解 `SET NX EX` vs Redisson
7. **上消息队列**：引入 RabbitMQ → 帖子发布异步更新 ES 索引 → 理解生产-消费模型和异步解耦（先用直接同步理解基础流程，再引入 MQ 升级为异步）
8. **学 Docker 容器化**：编写 Dockerfile + Docker Compose → 理解容器化部署 → 所有服务一键编排

---

## 附录：富文本方案对比（为什么选 Tiptap）

### 方案A：浏览器端富文本编辑器 — Tiptap（本项目选择）

**流程**：
1. 用户在前端用 Tiptap 编辑帖子，图文混排，所见即所得
2. 用户在编辑器中粘贴/拖入图片 → Tiptap 自定义 extension 自动将图片通过 FormData 上传到 MinIO
3. 上传成功后，后端返回图片 URL → Tiptap 将 URL 插入编辑器
4. 点击发布 → Tiptap 输出 HTML 字符串 → 提交到后端 → 后端用 jsoup 白名单清洗 HTML（防 XSS）并提取纯文本 → 存入 MySQL
5. 展示时前端用 DOMPurify 再做一次清洗后渲染

**优点**：
- 用户操作流畅，像写 Word 一样
- 图片上传是即时的（粘贴即上传），不需要等到发布
- Tiptap 输出结构化 JSON，方便做搜索和审核
- 这是 Web 社区的主流做法（掘金、知乎都是这种模式）

**缺点**：
- 需要额外写图片上传接口（但很简单）
- 需要做 XSS 过滤（jsoup + DOMPurify 双保险）

### 方案B：本地编辑后上传文档

**流程**：用户在本地用 Word/Markdown 写好 → 上传 `.md` / `.docx` → 后端解析 → 提取文字和图片 → 转 HTML 存储

**优点**：
- 用户可以在离线环境写长文

**缺点**：
- `.docx` 解析非常复杂（图片提取、排版还原都是天坑）
- 用户体验割裂，不符合 Web 应用的操作习惯
- Markdown 的图片路径处理也麻烦（本地路径无法直接使用）

### 结论

选择 **方案A（Tiptap 富文本编辑器）**，理由：
1. Web 社区的标准做法，学习价值高
2. 用户操作流程顺畅，不需要离开页面
3. 图片上传本质上只是一个独立接口，实现非常简单（50 行后端代码）
4. 作为练手项目，掌握 Tiptap 这样的主流方案比折腾文档解析更有价值
