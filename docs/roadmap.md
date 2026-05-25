# EchoSpace 功能路线图

> 记录待开发功能的方案设计、页面规划与接口清单。
> 完成后在对应条目前打勾 `[x]`。

---

## 功能列表

- [ ] [忘记密码 / 重置密码](#一忘记密码--重置密码)
- [ ] [日志系统改造为 AOP](#二日志系统改造为-aop)

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
