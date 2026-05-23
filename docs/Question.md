# EchoSpace 开发问题与解决方案

## 1、并发 401 导致重复刷新 Token

**问题**

多个请求同时返回 401 时，每个请求的响应拦截器都会检测到 401 并各自发起 `/auth/refresh` 调用，导致同一时刻多次刷新 Token，浪费资源且可能因竞态条件导致 Token 错乱。

**解决方案：Promise 锁**

在 Pinia userStore 中维护一个 `refreshingPromise`，首个 401 请求创建一个刷新 Promise 并存入 store，后续 401 请求检测到该 Promise 已存在时直接 `await` 等待它完成，然后复用新 Token 重试，避免重复刷新。

```typescript
// 已有刷新在进行中，等它完成
if (store.refreshingPromise) {
  await store.refreshingPromise
  // 刷新完成后用新 Token 重试
  error.config._retry = true
  setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
  return result(error.config)
}

// 发起刷新（首个 401）
const promise = (async () => { /* 调用 /auth/refresh */ })()
store.refreshingPromise = promise
try { await promise } finally { store.refreshingPromise = null }
```

## 2、Token 刷新失败后重试导致无限循环

**问题**

Token 刷新成功后，拦截器会用新 Token 自动重试原请求。但如果 refresh token 本身也已过期，刷新接口返回失败（或重试后再次 401），拦截器会再次进入 401 分支 → 再次尝试刷新 → 再次失败 → 无限循环。

**解决方案：`_retry` 标记 + 类型扩展 + 判空**

在 `api/modules/index.ts` 中通过 `declare module 'axios'` 扩展 `InternalAxiosRequestConfig`，为其增加可选的 `_retry?: boolean` 属性，解决 TypeScript 类型报错。

```typescript
// api/modules/index.ts — Axios 类型扩展
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    /** 401 重试标记，防止无限循环刷新 Token */
    _retry?: boolean
  }
}
```

在 result.ts 的 401 入口先对 `error.config` 判空（config 在 AxiosError 中为可选字段，可能为 undefined），再检查 `_retry` 标记。若已为 `true` 说明已走过一轮刷新，直接清除登录态并跳转登录页。重试前设置 `_retry = true`，确保每个请求最多重试一次。

```typescript
// config 不存在则无法重试，直接拒绝
if (!error.config) {
  return Promise.reject(error)
}

// 401 入口守卫：已重试过，直接踢到登录页
if (error.config._retry) {
  store.clearAuth()
  router.push('/login')
  return Promise.reject(error)
}

// 重试前打标
error.config._retry = true
setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
return result(error.config)
```

## 3、等待共享刷新时刷新失败，等待方仍用空 Token 重试

**问题**

当请求 A 正在刷新、请求 B 等待时，若 A 的刷新失败（store.token 被清空），B 在 `await` 之后直接用空 Token 重试，必然会再次 401，浪费一次请求。

**解决方案：等待后增加 Token 有效性检查**

等待共享刷新 Promise 完成后，先检查 `store.token` 是否存在。若刷新失败导致 Token 为空，直接 `Promise.reject`，不再发起无意义的重试。

```typescript
if (store.refreshingPromise) {
  await store.refreshingPromise
  if (!store.token) return Promise.reject(error) // 共享刷新失败，不再重试
  error.config._retry = true
  setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
  return result(error.config)
}
```

## 4、Spring Security 开启后 CORS 失效，OPTIONS 预检被拦截

**问题**

仅通过 `WebMvcConfigurer#addCorsMappings` 配置 CORS 时，CORS 处理发生在 Spring MVC 层（DispatcherServlet）。但 Spring Security 的 FilterChain 在请求到达 MVC 层之前就会拦截所有请求，包括浏览器的 CORS 预检请求（`OPTIONS`）。由于 Security 层未处理 CORS，OPTIONS 请求直接被拒绝或未附加 CORS 响应头，浏览器端所有跨域请求失败。

**解决方案：CORS 提升到 Security Filter 层**

1. 在 `CorsConfig` 中新增 `CorsConfigurationSource` Bean，将 CORS 规则注册为 Spring Security 可识别的配置源。
2. 在 `SecurityFilterChain` 中显式开启 `.cors(withDefaults())`，让 CORS 校验在 Security 过滤器链中最先执行（早于认证/授权）。
3. 移除原有的 `addCorsMappings`（在 Security 激活时不再生效）。

```java
// CorsConfig.java — 提供 CorsConfigurationSource Bean
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

// SecurityConfig.java — 显式开启 CORS
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(withDefaults())  // 在 Security Filter 层处理 CORS
        .csrf(csrf -> csrf.disable())
        // ...
    return http.build();
}
```

> Spring Security 的 `CorsFilter` 会在 `cors()` 开启后自动拾取容器中的 `CorsConfigurationSource` Bean。整个 CORS 校验在 Security 过滤器链的最前端完成，OPTIONS 预检请求不再被拦截。

## 5、Spring Security 构建流程与常见问题

### 5.1 `@Value` 注入 static 字段无效

**问题**

Spring 的 `@Value` 注解通过 Bean 后处理器在实例化后注入，但 static 字段属于类而非实例，Spring 不会为其赋值。若 JwtUtil 中的 `SECRET_KEY` 或过期时间用 `private static` + `@Value`，运行时字段始终为 `null`（或默认值），导致签名或解析时 NPE。

**修复点**

- 类加 `@Component`，字段改为实例字段。
- 用 `@PostConstruct` 将 String 类型的 secret 转为 `SecretKey`（`@Value` 只能注入基本类型/String，无法直接注入 `SecretKey`）。
- 过期时间也从 `application.yaml` 注入，避免硬编码。

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:1800000}")
    private long accessExpire;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshExpire;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    // ...
}
```

### 5.2 `parseEncryptedClaims` 误用于签名 JWT（JWS）

**问题**

代码中生成的是签名 JWT（JWS），但解析时调用了 `parseEncryptedClaims()`。该方法是给 JWE（加密）Token 用的，对签名 Token 会直接报错。

**修复点**

签名 Token 应使用 `parseSignedClaims()`：

```java
// ❌ JWE 解析
.parseEncryptedClaims(token)

// ✅ JWS 解析
.parseSignedClaims(token)
```

### 5.3 jjwt API 演进：废弃的 `SignatureAlgorithm` 和参数顺序

**问题**

jjwt 0.12.x 中 `SignatureAlgorithm.HS256` 已废弃，且 `signWith(Algorithm, Key)` 的参数顺序改为 `signWith(Key, Algorithm)`。旧写法编译通过但 IDE 会标黄，且容易与新代码混用造成不一致。

**修复点**

```java
// ❌ 旧 API（HS256 废弃，参数顺序反了）
.signWith(SignatureAlgorithm.HS256, SECURITY)

// ✅ 新 API（Jwts.SIG.HS256，key 在前）
.signWith(secretKey, Jwts.SIG.HS256)
```

### 5.4 JwtAuthFilter 未注册到 SecurityFilterChain

**问题**

Filter 类写好了（继承 `OncePerRequestFilter`），但如果没有通过 `http.addFilterBefore()` 注册到 Security 过滤器链中，这个 Filter 根本不会被执行——请求不会经过 JWT 解析，SecurityContextHolder 始终为空。

**修复点**

1. `JwtAuthFilter` 加 `@Component`，`@Autowired` 注入 `JwtUtil`。
2. `SecurityConfig` 中 `@Autowired` 注入 `JwtAuthFilter`，并通过 `addFilterBefore` 将其注册到 `UsernamePasswordAuthenticationFilter` 之前。

```java
// SecurityConfig.java
@Autowired
private JwtAuthFilter jwtAuthFilter;

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ... cors, csrf, session ...
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### 5.5 Filter 中 JWT 解析异常未处理 → 500 而非 401，且响应体与统一 Result 格式不一致

**问题**

两个层面：

1. **异常未处理 → 500**：`JwtUtil.parseToken()` 在 Token 过期、签名错误、格式错误时会抛 `ExpiredJwtException` / `JwtException`。若 Filter 中不 catch，异常会向上传播到 Servlet 容器，最终返回 500，而不是业务期望的 401。

2. **手动拼 JSON → 格式不一致**：即使 catch 了，如果直接 `response.getWriter().write("{\"code\":401,...}")` ，手写的 JSON 字符串与后端统一 `Result` 结构（`code=0` 表示错误、必有 `data` 字段）不一致，前端拦截器按统一格式解析时会出错（如 `error.response.data.msg` 取到 undefined）。

**修复点**

- catch 后在 Filter 中写 401 响应时，使用 `ObjectMapper` 将 `Result.error("...")` 序列化输出，确保响应的 JSON 结构与 Controller 层完全一致。
- `ObjectMapper` 通过 `@Autowired` 注入（Spring Boot 自动配置已提供），避免手动拼字符串。

```java
@Autowired
private ObjectMapper objectMapper;

// catch 块中：
} catch (ExpiredJwtException e) {
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), Result.error("Token已过期"));
    return;
} catch (JwtException e) {
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), Result.error("Token无效"));
    return;
}
```

> **关键点**：Filter 中不能像 Controller 那样直接 return `Result<T>`，因为 Filter 工作在 Servlet 层（Controller 之前），只能通过 `HttpServletResponse` 写入。使用 `ObjectMapper` 序列化 `Result` 对象可以保证格式与 Controller 返回的完全一致：`{"code":0,"msg":"Token已过期","data":null}`。

### 5.6 JWT subject 解析为 Long 时未捕获 NumberFormatException

**问题**

`Long.valueOf(claims.getSubject())` 在以下情况会抛 `NumberFormatException`，进而导致 500：
- `claims.getSubject()` 返回 `null`（Token 生成时未设置 subject）
- subject 是非数字字符串（Token 被篡改或来自其他系统）

`NumberFormatException` 不是 `JwtException` 的子类，不会被上方的 JWT 解析 catch 块捕获。

**修复点**

在提取 userId 时单独 try-catch `NumberFormatException`，先做非空校验，解析失败统一按"Token 无效"返回 401：

```java
Long userId;
try {
    String subject = claims.getSubject();
    if (subject == null || subject.isBlank()) {
        throw new NumberFormatException("subject is empty");
    }
    userId = Long.valueOf(subject);
} catch (NumberFormatException e) {
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), Result.error("Token无效"));
    return;
}
```

> **关键点**：`NumberFormatException` 继承自 `IllegalArgumentException`，与 `JwtException` 无关，必须单独捕获。不要用 `catch (Exception e)` 大包——那会把真正的系统异常也吞掉，掩盖 bug。

### 5.7 SecurityUtil 缺少 SecurityContext 空判断 + 强转风险

**问题**

两个层面的缺陷：

1. **NPE 风险**：未登录请求或白名单接口不会经过 JWT 认证，`SecurityContextHolder.getContext().getAuthentication()` 返回 `null`。此时直接调用 `.getPrincipal()` 会 NPE。

2. **ClassCastException 风险**：Spring Security 在未认证状态下可能注入 `AnonymousAuthenticationToken`，其 `principal` 是字符串 `"anonymousUser"`，而非业务对象。直接强转会抛 `ClassCastException`。

**修复点**

三层防御：判空 → 排除匿名 Token → `instanceof` 类型检查（利用 Java 16+ 模式匹配）。

同时，将 `userId` 和 `username` 封装进自定义 `UserPrincipal` record，统一存入 `principal`（见 5.12），两个工具方法都从 `getPrincipal()` 读取，不再依赖 `details` 字段：

```java
public static Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
            || !auth.isAuthenticated()
            || auth instanceof AnonymousAuthenticationToken) {
        return null;
    }
    if (auth.getPrincipal() instanceof UserPrincipal up) {
        return up.userId();
    }
    return null;
}

public static String getCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
            || !auth.isAuthenticated()
            || auth instanceof AnonymousAuthenticationToken) {
        return null;
    }
    if (auth.getPrincipal() instanceof UserPrincipal up) {
        return up.username();
    }
    return null;
}
```

> **关键点**：`auth.isAuthenticated()` 对 `AnonymousAuthenticationToken` 返回 `false`，但显式 `instanceof` 检查是最安全的做法——即使 Spring Security 未来版本改变行为也不会出错。`UserPrincipal` 的引入见 5.12，两处修复配合使用。

### 5.8 未配置 exceptionHandling → 未认证访问返回默认 403 HTML 而非 401 JSON

**问题**

仅配置 `.anyRequest().authenticated()` 时，Spring Security 对未认证请求的默认行为是：
- 触发 `AuthenticationEntryPoint`，默认实现返回 **403**（无状态场景下不会重定向到登录页，但也不是 401）
- 响应体是 Spring 默认的 HTML 错误页，而非项目统一的 JSON `Result` 结构
- 前端拦截器按 `error.response?.data?.msg` 解析时取到 `undefined`，无法给用户有意义的提示

**修复点**

在 `SecurityFilterChain` 中显式配置 `.exceptionHandling()`，分别处理两种场景：
- `authenticationEntryPoint`：未认证（没有 Token 或 Token 无效后未被 Filter 拦截）→ 返回 **401**
- `accessDeniedHandler`：已认证但无权限 → 返回 **403**

两者都用 `ObjectMapper` 序列化 `Result.error(...)` 保持响应格式统一：

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error("未登录或登录已过期"));
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error("无访问权限"));
    })
)
```

> **关键点**：`AuthenticationEntryPoint` 和 `AccessDeniedHandler` 工作在 Security 异常处理层，与 `@ControllerAdvice` 全局异常处理器无关——Security 层的异常不会流转到 Spring MVC，必须在这里单独处理。

### 5.9 JWT 未携带/校验 Token 类型，RefreshToken 可冒充 AccessToken

**问题**

`generateToken` 仅用 `type` 参数决定过期时间，但如果不把 `type` 写入 JWT payload，`JwtAuthFilter` 就无法区分收到的是 AccessToken 还是 RefreshToken。这意味着：
- RefreshToken（7 天有效期）可以直接放在 `Authorization` Header 里访问受保护接口
- 一旦 RefreshToken 泄露，攻击者拥有长达 7 天的访问窗口，而非 AccessToken 的 30 分钟

**修复点**

两步：

1. **生成时写入 type claim**（通过 `TokenType.claimValue()` 写入，见 5.13）：

```java
.claim("type", type.claimValue())   // "access" 或 "refresh"
```

2. **过滤器中区分两种拒绝场景**：`type` 缺失/空说明 Token 格式不完整（历史 Token 或被裁剪），按无效处理；存在但非 `"access"` 才是类型错误：

```java
String tokenType = claims.get("type", String.class);
if (tokenType == null || tokenType.isBlank()) {
    // type claim 缺失，Token 格式不完整
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), Result.error("Token无效"));
    return;
}
if (!JwtUtil.TokenType.ACCESS.claimValue().equals(tokenType)) {
    // type 存在但不是 access，明确是类型错误
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), Result.error("Token类型错误，请使用AccessToken"));
    return;
}
```

> **关键点**：`/api/auth/refresh` 接口在白名单中（`permitAll()`），不经过 `JwtAuthFilter`，所以 RefreshToken 仍然可以正常提交给刷新接口。类型校验只影响受保护接口，不影响刷新流程。

### 5.10 白名单接口携带过期 Token 被 Filter 拦截返回 401

**问题**

前端 Axios 请求拦截器会对所有请求无差别注入 `Authorization: Bearer {token}` Header。当 Token 已过期时，即使请求的是 `/api/auth/login`、`/api/auth/refresh` 等白名单接口，`JwtAuthFilter` 也会先解析 Token，遇到 `ExpiredJwtException` 直接返回 401，导致登录/刷新请求被挡在 Filter 层，用户无法完成重新登录。

**修复点**

在 `JwtAuthFilter` 的 `doFilterInternal` 最开头，用 `AntPathMatcher` 匹配白名单路径，命中则直接 `filterChain.doFilter()` 放行，跳过所有 Token 校验逻辑：

```java
private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

private static final String[] WHITELIST_PATHS = {
        "/api/auth/login", "/api/auth/register", "/api/auth/refresh"
};

// doFilterInternal 最开头：
String uri = request.getRequestURI();
for (String pattern : WHITELIST_PATHS) {
    if (PATH_MATCHER.match(pattern, uri)) {
        filterChain.doFilter(request, response);
        return;
    }
}
```

> **关键点**：`SecurityConfig` 中的 `permitAll()` 只控制 Spring Security 的授权层（认证通过后是否允许访问），不能阻止 `JwtAuthFilter` 在授权层之前执行。Filter 的白名单必须在 Filter 内部自己维护，两处配置各司其职、缺一不可。`AntPathMatcher` 声明为 `static final` 避免每次请求重复实例化。

### 5.11 Spring Security 过滤器链执行顺序（整体流程）

了解过滤器的执行顺序有助于理解上述修复的必要性：

```
请求 → CorsFilter  → CsrfFilter  → ...  → JwtAuthFilter  → 认证 → 授权 → Controller
        ↑                                    ↑
     最早执行，                            在此验证 JWT，
     处理 OPTIONS                          写入 SecurityContextHolder
```

- **CorsFilter** 必须在最前面（OPTIONS 预检不需要认证）。
- **JwtAuthFilter** 在 `UsernamePasswordAuthenticationFilter` 之前执行，将 JWT 中的用户信息写入 `SecurityContextHolder`。
- 后续的认证/授权组件从 `SecurityContextHolder` 中读取当前用户。
- Filter 中未捕获的异常会绕过 Spring 全局异常处理器，直接返回 500 → 必须在 Filter 内部 try-catch。

### 5.12 username 存入 details 字段，可能被其他组件覆盖

**问题**

`Authentication.details` 在 Spring Security 语义上用于存储请求级元数据（如 `WebAuthenticationDetails` 包含的 IP、SessionId），并非业务数据的存储位置。将 `username` 写入 `details` 存在两个风险：

1. **被覆盖**：Spring Security 内置的 `WebAuthenticationDetailsSource` 会在认证流程中自动向 `details` 写入 `WebAuthenticationDetails`；若后续引入其他过滤器或审计组件也调用 `authentication.setDetails()`，username 会被静默覆盖，`SecurityUtil.getCurrentUsername()` 返回错误值或 `null`。
2. **语义混乱**：`details` 是框架约定的基础设施字段，业务代码读取它会与框架行为产生隐式耦合，难以维护。

**修复点**

定义 `UserPrincipal` record，将 `userId` 和 `username` 一起封装进 `principal`，通过 `getPrincipal()` 读取：

```java
// UserPrincipal.java
public record UserPrincipal(Long userId, String username) {}
```

```java
// JwtAuthFilter — 写入 SecurityContextHolder
UserPrincipal principal = new UserPrincipal(userId, username);
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, List.of());
SecurityContextHolder.getContext().setAuthentication(authentication);
```

```java
// SecurityUtil — 从 principal 读取
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
```

> **关键点**：`principal` 是 Spring Security 中"当前认证主体"的标准存储位置，框架不会在认证完成后再修改它。将业务数据放在 `principal` 而非 `details`，既符合框架语义，也避免了被其他组件意外覆盖的风险。

### 5.13 generateToken 用字符串分支决定过期时间，传错 type 静默生成长效 Token

**问题**

```java
long expire = "access".equals(type) ? accessExpire : refreshExpire;
```

这段逻辑只要 `type` 不是 `"access"`（包括拼写错误、大小写错误、传入 `null`），就会静默走 `refreshExpire`（7 天），生成一个长效 Token。调用方不会收到任何错误提示，安全问题难以排查。

同时，`JwtAuthFilter` 中校验 type 的 `"access"` 是魔法字符串，与生成侧的字符串字面量各自维护，一旦其中一处改动就会产生不一致。

**修复点**

两步：

1. **将 `TokenType` 改为 `public` enum，并提供 `claimValue()` 方法**，统一管理写入 JWT payload 的字符串值：

```java
public enum TokenType {
    ACCESS, REFRESH;

    public String claimValue() {
        return name().toLowerCase(); // "access" / "refresh"
    }
}
```

2. **拆分为 `generateAccessToken` / `generateRefreshToken` 两个公开方法**，内部共用 `private buildToken(TokenType, ...)`：

```java
public String generateAccessToken(String sub, String username) {
    return buildToken(TokenType.ACCESS, sub, username);
}

public String generateRefreshToken(String sub, String username) {
    return buildToken(TokenType.REFRESH, sub, username);
}

private String buildToken(TokenType type, String sub, String username) {
    long expire = type == TokenType.ACCESS ? accessExpire : refreshExpire;
    return Jwts.builder()
            .subject(sub)
            .claim("username", username)
            .claim("type", type.claimValue())
            // ...
            .compact();
}
```

3. **`JwtAuthFilter` 中用 enum 替换魔法字符串**：

```java
if (!JwtUtil.TokenType.ACCESS.claimValue().equals(tokenType)) { ... }
```

> **关键点**：拆成两个方法后，调用方在编译期就被约束只能选择 `generateAccessToken` 或 `generateRefreshToken`，不存在传错字符串的可能。`claimValue()` 集中维护 payload 中的字符串值，生成侧和校验侧引用同一个来源，彻底消除魔法字符串不一致的风险。

### 5.14 未显式禁用 formLogin / httpBasic / logout，默认行为偏离预期

**问题**

无状态 JWT API 场景下，Spring Security 默认仍会启用以下行为：

- **formLogin**：未认证请求触发重定向到 `/login` 登录页（或返回 302），而非 JSON 401。即使配置了 `authenticationEntryPoint`，`UsernamePasswordAuthenticationFilter` 仍在过滤器链中，暴露了不必要的端点。
- **httpBasic**：响应头携带 `WWW-Authenticate: Basic realm="..."` Challenge，浏览器弹出原生认证对话框，与 JSON API 的交互预期完全不符。
- **logout**：默认注册 `POST /logout` 端点，在纯 JWT 场景下无意义，属于多余的攻击面。

**修复点**

在 `SecurityFilterChain` 中显式禁用三者：

```java
.csrf(csrf -> csrf.disable())
.formLogin(form -> form.disable())
.httpBasic(basic -> basic.disable())
.logout(logout -> logout.disable())
```

> **关键点**：显式禁用比依赖"默认不触发"更安全——Spring Security 版本升级可能改变默认行为，显式配置让意图清晰且不受版本影响。禁用后，未认证请求完全由 `authenticationEntryPoint` 接管，统一返回 JSON 401，行为可预期。

### 5.15 Filter 内 401 响应写出逻辑重复，散落多处难以统一维护

**问题**

`JwtAuthFilter` 中每个拒绝分支都重复三行相同的代码：

```java
response.setStatus(401);
response.setContentType("application/json;charset=UTF-8");
objectMapper.writeValue(response.getWriter(), Result.error("..."));
```

共出现 6 次。一旦需要统一添加响应头（如 `Cache-Control: no-store`、`X-Trace-Id`）或调整 Content-Type 格式，必须逐一修改，极易漏改导致行为不一致。

**修复点**

抽取私有方法 `writeUnauthorized`，集中管理状态码、Content-Type 和序列化逻辑，调用方只传错误描述：

```java
private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    objectMapper.writeValue(response.getWriter(), Result.error(message));
}
```

各拒绝分支简化为：

```java
writeUnauthorized(response, "Token已过期");
return;
```

> **关键点**：使用 `HttpServletResponse.SC_UNAUTHORIZED`（值为 401）替代魔法数字，使用 `MediaType.APPLICATION_JSON_VALUE` 替代字符串字面量，语义更清晰。后续如需统一加响应头或切换序列化方式，只改一处即可。

### 5.16 白名单硬编码为 public static final String[]，数组内容可被外部篡改

**问题**

将白名单定义为 `public static final String[] WHITELIST_PATHS` 存在两个问题：

1. **数组可变性**：Java 数组即使声明为 `final`，`final` 只保证引用不变，数组元素仍可被任意代码修改（`SecurityConfig.WHITELIST_PATHS[0] = "/api/admin/..."`），导致白名单被静默篡改。
2. **硬编码耦合**：白名单路径散落在代码中，新增/删除路径需要重新编译部署，无法在不同环境（开发/测试/生产）灵活调整。

**修复点**

三步：

1. **在 `application.yaml` 中配置白名单**：

```yaml
security:
  whitelist:
    - /api/auth/login
    - /api/auth/register
    - /api/auth/refresh
    - /error
```

2. **用 `@ConfigurationProperties` 绑定为 `List<String>`，默认值为空列表防止 NPE**：

```java
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {
    /** 不需要 JWT 认证的路径白名单，对应 yaml 中的 security.whitelist */
    private List<String> whitelist = List.of();
}
```

3. **`SecurityConfig` 和 `JwtAuthFilter` 均注入 `SecurityProperties` 读取白名单**，不再引用任何静态数组：

```java
// SecurityConfig — 授权层白名单
.requestMatchers(securityProperties.getWhitelist().toArray(new String[0])).permitAll()

// JwtAuthFilter — @PostConstruct 缓存后由 shouldNotFilter 使用（见 5.17）
@PostConstruct
public void init() {
    this.whitelist = securityProperties.getWhitelist();
}
```

> **关键点**：`List<String>` 由 Spring 绑定，外部无法通过静态字段直接访问，消除了数组元素被篡改的风险。白名单集中在配置文件维护，两处消费方（Filter 层和授权层）引用同一数据源，新增路径只改 yaml 即可，无需重新编译。

### 5.17 白名单匹配在 doFilterInternal 内遍历，per-request 开销随白名单增长

**问题**

原实现在 `doFilterInternal` 开头遍历白名单并调用 `AntPathMatcher.match`：

```java
for (String pattern : securityProperties.getWhitelist()) {
    if (PATH_MATCHER.match(pattern, uri)) {
        filterChain.doFilter(request, response);
        return;
    }
}
```

两个问题：
1. **每次请求都进入 `doFilterInternal`**：即使是白名单路径，也要先进入方法体才能判断并 `return`，无法在框架层面跳过。
2. **每次请求都调用 `getWhitelist()`**：白名单列表在运行期不会变化，没有必要每次重新获取。

**修复点**

两步：

1. **`@PostConstruct` 缓存白名单列表**，避免每次请求重复调用 getter：

```java
private List<String> whitelist;

@PostConstruct
public void init() {
    this.whitelist = securityProperties.getWhitelist();
}
```

2. **重写 `shouldNotFilter`**，命中白名单时框架直接跳过整个过滤器，不进入 `doFilterInternal`：

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return whitelist.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
}
```

`doFilterInternal` 中的白名单遍历块随之删除。

> **关键点**：`OncePerRequestFilter.shouldNotFilter` 在框架层面决定是否执行过滤器，返回 `true` 时整个 filter 被跳过，比在方法体内 `return` 更彻底。白名单路径的请求不再进入 `doFilterInternal`，也不会触发任何 JWT 解析逻辑。
