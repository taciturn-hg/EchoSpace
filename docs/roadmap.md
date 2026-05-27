# EchoSpace 功能路线图

> 记录待开发功能的方案设计、页面规划与接口清单。
> 完成后在对应条目前打勾 `[x]`。

---

## 功能列表

- [ ] [忘记密码 / 重置密码](#一忘记密码--重置密码)
- [ ] [日志系统改造为 AOP](#二日志系统改造为-aop)
- [ ] [帖子列表瀑布流加载（游标分页）](#三帖子列表瀑布流加载游标分页)

---

## 一、忘记密码 / 重置密码

### 背景

登录页"忘记密码"入口当前已禁用。未登录状态下无 JWT Token，无法通过现有受保护接口验证用户身份。需引入带外验证机制（邮件或短信）后再开放。

### 方案分析

**核心思路**：用邮件/短信下发一次性重置 Token，以此替代 JWT 完成身份验证，整个流程不依赖用户已登录。

| 方案 | 可行性 | 说明 |
|------|--------|------|
| 邮件验证链接 | ✅ 推荐 | 发送含重置 Token 的链接到注册邮箱，用户点击后跳转重置页面 |
| 短信验证码 | ✅ 可选 | 发送 6 位验证码到注册手机号，需接入短信服务商（成本较高） |
| 安全问题 | ❌ 不推荐 | 安全性低，用户体验差，已被主流产品淘汰 |

**推荐方案：邮件验证链接**，理由：
- 零成本（JavaMailSender + 免费 SMTP）
- 安全性高（链接含随机 Token，有时效，一次性）
- 实现复杂度低，无需接入第三方付费服务

### 流程设计

```
用户点击"忘记密码"
    → 跳转忘记密码页，输入注册邮箱
    → POST /api/auth/forgot-password { email }
        → 后端查询邮箱是否存在
        → 生成 UUID resetToken，存入 Redis（key: reset:token:{resetToken} → userId，TTL 15分钟）
        → 发送邮件：包含重置链接 https://domain/reset-password?token={resetToken}
    → 前端提示"重置邮件已发送，请查收"

用户点击邮件中的链接
    → 跳转重置密码页，URL 携带 token 参数
    → 用户输入新密码 + 确认密码
    → POST /api/auth/reset-password { token, newPassword, confirmPassword }
        → 后端从 Redis 取 reset:token:{token}，获取 userId
        → 校验 token 存在且未过期
        → BCrypt 加密新密码，UPDATE user SET password = ?
        → 删除 Redis 中的 resetToken（一次性，用完即删）
    → 前端提示"密码重置成功"，跳转登录页
```

### 安全要点

- resetToken 使用 UUID 随机生成，不可预测
- Redis TTL 设为 15 分钟，过期自动失效
- 重置成功后立即删除 Redis key，防止链接重复使用
- 邮箱不存在时返回相同提示（"重置邮件已发送"），防止用户枚举
- 新密码长度校验与注册保持一致（6~20 位）
- `/auth/forgot-password` 和 `/auth/reset-password` 加入 Security 白名单

### 新增页面

| 页面文件 | 路由 | 说明 |
|----------|------|------|
| `ForgotPassword.vue` | `/forgot-password` | 输入注册邮箱，提交后显示"邮件已发送"提示 |
| `ResetPassword.vue` | `/reset-password?token=xxx` | 读取 URL token 参数，输入新密码 + 确认密码提交 |

两个页面均为 `meta: { guest: true }`，已登录用户访问自动跳转首页。

### 新增接口

| 方法 | 路径 | 说明 | 是否需要 Token |
|------|------|------|---------------|
| POST | `/api/auth/forgot-password` | 发送重置密码邮件 | 否（加白名单） |
| POST | `/api/auth/reset-password` | 用 resetToken 重置密码 | 否（加白名单） |

**请求/响应结构**

```
POST /api/auth/forgot-password
Request:  { "email": "user@example.com" }
Response: { "code": 1, "msg": "重置邮件已发送，请在15分钟内完成操作", "data": null }

POST /api/auth/reset-password
Request:  { "token": "uuid", "newPassword": "xxx", "confirmPassword": "xxx" }
Response: { "code": 1, "msg": "密码重置成功", "data": null }
```

### 新增后端类

| 类 | 说明 |
|----|------|
| `ForgotPasswordDTO` | `{ email }` |
| `ResetPasswordDTO` | `{ token, newPassword, confirmPassword }` |
| `MailService` / `MailServiceImpl` | 封装 JavaMailSender，发送重置邮件 |

**依赖**（pom.xml 新增）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**配置**（application.yaml 新增）：

```yaml
spring:
  mail:
    host: smtp.qq.com        # 或 smtp.163.com / smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}   # QQ 邮箱授权码，非登录密码
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

### 开发前置条件

- [x] 引入 `spring-boot-starter-mail` 依赖
- [x] 配置 SMTP 邮箱账号（环境变量注入）
- [ ] Redis 已接入（用于存储 resetToken）
- [x] 前端路由已有 `/forgot-password` 占位页（当前已存在 `ForgotPassword.vue`）

---

## 二、日志系统改造为 AOP

### 背景

当前日志通过在每个 Controller / Service 方法中手动写 `log.info` / `log.warn` 来实现业务追踪。
随着接口增多，手动日志面临以下问题：

- **代码侵入性强**：每个方法都需要写日志代码，与业务逻辑混杂
- **日志格式不统一**：不同开发者写法各异，排查时难以关联
- **容易遗漏**：新增接口时可能忘记加日志
- **维护成本高**：修改日志格式需要逐个方法改动

### 方案

采用 **Spring AOP 切面** 统一拦截 Controller 和 Service 方法，自动记录入参、出参、异常，不再在各方法中手动写日志。

### 技术选型

- **Spring AOP**（`spring-boot-starter-aop`，Spring Boot 已自带，无需额外依赖）
- 自定义 `@Log` 注解标记需要日志追踪的类或方法（也可直接按包路径切 Controller/Service 层）

### 计划实现

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `@Log` 注解（`annotation/Log.java`），支持按方法/类级别控制 |
| 2 | 创建 `LogAspect` 切面类（`aspect/LogAspect.java`），环绕通知统一记录入参、耗时、出参、异常 |
| 3 | 移除现有 Controller / Service 中的 `log.info` / `log.warn` 手动日志 |
| 4 | 保留必要的 `log.warn`（如业务校验失败的关键路径），避免切面日志噪音过大 |

### 切面日志格式

```
INFO  [>] 请求进入：UserController.login(account=zhangsan)
INFO  [<] 响应返回：UserController.login -> LoginVO(expiresIn=1800) 耗时=45ms
ERROR [!] 异常捕获：UserController.login -> BusinessException: 账号或密码错误 耗时=12ms
```

### 参考

- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/reference/core/aop.html)
- `@Around` 环绕通知 + `ProceedingJoinPoint` 获取方法签名和参数

---

## 三、帖子列表瀑布流加载（游标分页）

### 背景

当前接口文档（3.5 帖子列表、2.7 用户帖子列表）设计为传统页码分页：前端传 `current`（页码），后端返回 `total`（总数）+ `current`（当前页）+ `records`。前端需改用**无限滚动**（触底加载更多，类似贴吧），不展示页码，不计算总页数。

### 两种分页方式对比

| | 页码分页（当前设计） | 游标分页（目标） |
|---|---|---|
| 请求参数 | `current=1, size=10` | `cursor=xxx, size=10`（首次不传 cursor） |
| 响应参数 | `records, total, current, size` | `records, cursor, hasMore, size` |
| 翻页方式 | 点"下一页" | 滚到底部自动触发 |
| SQL 性能 | 越后面越慢（OFFSET 扫描） | 始终恒定（走索引） |
| 数据一致性 | 翻页间新数据插入会导致重复/遗漏 | 游标天然避免此问题 |
| 总数查询 | 每次请求都 COUNT | 不查总数，多查 1 条判断 hasMore |

### 数据库现状分析

当前 `post` 表已有联合索引：

```sql
INDEX idx_status_created (status, created_at)
```

该索引**已基本满足**游标分页需求。游标查询 SQL 模式：

```sql
-- 首页（无游标）
SELECT * FROM post WHERE status = 1 ORDER BY created_at DESC, id DESC LIMIT 21

-- 后续页（传入上一页最后一条的 created_at + id）
SELECT * FROM post 
WHERE status = 1 
  AND (created_at < #{cursorTime} OR (created_at = #{cursorTime} AND id < #{cursorId}))
ORDER BY created_at DESC, id DESC 
LIMIT 21
```

> 多查 1 条（size + 1）：若实际返回条数 > size 则 `hasMore = true`，前端取前 size 条，第 size+1 条作为下一次的 cursor。

### 改动清单

#### 接口文档改动

**3.5 帖子列表：**

```
改前: GET /api/posts?current=1&size=10&sort=created_at
改后: GET /api/posts?cursor=1704067200000_100&size=10&sort=created_at
      首次不传 cursor；cursor 值为上一页最后一条的 "createTime_id"
```

请求参数变化：

| 参数 | 改前 | 改后 |
|------|------|------|
| `current` | 页码，number | **删除** |
| `cursor` | — | **新增**，游标，string，格式 `{timestamp}_{id}`，首次不传 |
| `size` | 每页条数 | 不变 |
| `sort` | 排序 | 不变 |

响应数据变化：

| 字段 | 改前 | 改后 |
|------|------|------|
| `total` | 总记录数 | **删除** |
| `current` | 当前页 | **删除** |
| `cursor` | — | **新增**，下页游标（无更多时为 null） |
| `hasMore` | — | **新增**，boolean，是否还有更多 |
| `records` | 帖子数组 | **不变** |

```
改前响应: { records: [...], total: 500, current: 1, size: 10 }
改后响应: { records: [...], cursor: "1704067200000_95", hasMore: true, size: 10 }
```

**2.7 用户帖子列表**：同上模式修改。

#### 数据库改动

**无需改表**。`idx_status_created (status, created_at)` 已满足需求。

可选优化：将索引调整为 `INDEX idx_status_created (status, created_at, id)` 让 tie-breaker 走索引覆盖，但当前索引下 MySQL 对 `id` 做 filesort 的代价极小（LIMIT 只有几十条），暂不必要。

#### 后端改动

| 文件 | 改动 |
|------|------|
| `vo/PageVO.java` | 新增 `CursorPageVO<T>`：`records, cursor, hasMore, size`，保留 `PageVO` 给其他仍用页码分页的接口 |
| `mapper/PostMapper.java` | 新增游标查询方法（自定义 SQL） |
| `service/PostService.java` | 接口新增 `listPosts(String cursor, int size, String sort)` |
| `service/impl/PostServiceImpl.java` | 实现游标查询逻辑（解析 cursor → WHERE 条件 → LIMIT size+1 → 组装 CursorPageVO） |
| `controller/PostController.java` | `GET /api/posts` 参数 `cursor` 替换 `current` |
| `controller/UserController.java` | `GET /api/users/{id}/posts` 同上 |

游标编码/解码工具方法：

```java
// cursor 格式: "{timestamp}_{id}"，如 "1704067200000_100"
// 前端不解析，原样传回即可
public record Cursor(long time, long id) {
    public static Cursor parse(String cursor) {
        String[] parts = cursor.split("_");
        return new Cursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }
    public String encode() { return time + "_" + id; }
}
```

#### 前端改动

| 文件 | 改动 |
|------|------|
| `api/modules/index.ts` | 新增 `fetchPosts(cursor?, size)` API 函数 |
| `views/HomePage.vue` | 替换原有分页逻辑为 `v-infinite-scroll` 无限滚动 |
| `stores/` | 可选：新增 `postStore` 管理帖子列表状态（累积、重置） |

Element Plus 内置指令直接支持：

```vue
<template>
  <div v-infinite-scroll="loadMore" :infinite-scroll-disabled="!hasMore" infinite-scroll-distance="100">
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
  </div>
  <p v-if="loading">加载中...</p>
  <p v-if="!hasMore && posts.length > 0">没有更多了</p>
</template>
```

### 先做页码分页再重构 vs 直接用游标分页

#### 结论：直接用游标分页，不要分两步走

#### 分析

| 维度 | 先页码再游标 | 直接用游标 |
|------|-------------|-----------|
| **后端重复工作** | 写两套查询逻辑（OFFSET + 游标），第一版代码会被完全替换 | 只写一套 |
| **前端重复工作** | 先做分页器 UI，再改成无限滚动，交互逻辑完全不同 | 只写一次 |
| **VO 层** | PageVO → 再补 CursorPageVO，Controller 返回值要改 | 一步到位 CursorPageVO |
| **浪费的代码量** | 约 60% 的帖子列表代码需要重写 | 0% |
| **额外风险** | 切换分页方式时可能引入回归 bug | 无 |

**具体估算**：

先做页码分页时需写的代码，在迁移时会被改掉的部分：

```
后端（会被重写的）:
  - PostServiceImpl.listPosts() 中的 MyBatis-Plus Page 查询 → 改为游标 SQL
  - PostController 的 @RequestParam current → 改为 cursor
  - 响应类型 PageVO → CursorPageVO

前端（会被重写的）:
  - HomePage.vue 的分页器 + 页码状态管理 → 改为 v-infinite-scroll + 累积逻辑
  - API 函数的 current 参数 → cursor 参数
  - 响应类型里的 total/current → cursor/hasMore

不变的部分：
  - PostCard 组件（帖子卡片渲染）—— 这部分不动
  - Post 类型定义 —— 记录字段不变
  - 其他帖子接口（详情/发布/编辑/删除）—— 完全不动
```

迁移成本约占帖子列表功能总工作量的 **50-60%**——相当于做 1.5 遍。考虑到当前 Sprint 3 还没开始写代码，没有理由先走弯路。

### 实施建议

1. 在 Sprint 3 开始时就按游标分页实现 3.5（帖子列表）
2. 2.7（用户帖子列表）同样使用游标分页
3. 其他列表接口（2.9 粉丝列表、2.10 关注列表、4.x 评论列表）后续开发时评估是否需要无限滚动——评论通常用页码分页（用户可能想跳页），粉丝/关注列表数据量小，也可以用页码分页
4. 保留 `PageVO` 不动，只新增 `CursorPageVO`，不需要把所有分页都改成游标
