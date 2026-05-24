# EchoSpace 开发问题与解决方案

---

## 后端：Spring Security 构建

### 1、Spring Security 开启后 CORS 失效，OPTIONS 预检被拦截

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

### 2、Spring Security 构建流程与常见问题

#### 2.1 `@Value` 注入 static 字段无效

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

#### 2.2 `parseEncryptedClaims` 误用于签名 JWT（JWS）

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

#### 2.3 jjwt API 演进：废弃的 `SignatureAlgorithm` 和参数顺序

**问题**

jjwt 0.12.x 中 `SignatureAlgorithm.HS256` 已废弃，且 `signWith(Algorithm, Key)` 的参数顺序改为 `signWith(Key, Algorithm)`。旧写法编译通过但 IDE 会标黄，且容易与新代码混用造成不一致。

**修复点**

```java
// ❌ 旧 API（HS256 废弃，参数顺序反了）
.signWith(SignatureAlgorithm.HS256, SECURITY)

// ✅ 新 API（Jwts.SIG.HS256，key 在前）
.signWith(secretKey, Jwts.SIG.HS256)
```

#### 2.4 JwtAuthFilter 未注册到 SecurityFilterChain

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

#### 2.5 Filter 中 JWT 解析异常未处理 → 500 而非 401，且响应体与统一 Result 格式不一致

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

#### 2.6 JWT subject 解析为 Long 时未捕获 NumberFormatException

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

#### 2.7 SecurityUtil 缺少 SecurityContext 空判断 + 强转风险

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

#### 2.8 未配置 exceptionHandling → 未认证访问返回默认 403 HTML 而非 401 JSON

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

#### 2.9 JWT 未携带/校验 Token 类型，RefreshToken 可冒充 AccessToken

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

#### 2.10 白名单接口携带过期 Token 被 Filter 拦截返回 401

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

#### 2.11 Spring Security 过滤器链执行顺序（整体流程）

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

#### 2.12 username 存入 details 字段，可能被其他组件覆盖

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

#### 2.13 generateToken 用字符串分支决定过期时间，传错 type 静默生成长效 Token

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

#### 2.14 未显式禁用 formLogin / httpBasic / logout，默认行为偏离预期

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

#### 2.15 Filter 内 401 响应写出逻辑重复，散落多处难以统一维护

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

#### 2.16 白名单硬编码为 public static final String[]，数组内容可被外部篡改

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

#### 2.17 白名单匹配在 doFilterInternal 内遍历，per-request 开销随白名单增长

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

### 3、白名单路径与 context-path 的匹配对齐

**问题**

`application.yaml` 中配置了 `server.servlet.context-path: /api`，白名单路径最初写成了带前缀的形式（`/api/auth/register`），导致 Spring Security 的 `requestMatchers` 匹配失败返回 401。

根本原因是两处消费方对路径的理解不一致：

| 消费方 | 路径来源 | 实际值 |
|--------|----------|--------|
| `SecurityConfig.requestMatchers` | Servlet 路径（不含 context-path） | `/auth/register` |
| `JwtAuthFilter.shouldNotFilter`（修复前） | `getRequestURI()`（含 context-path） | `/api/auth/register` |

**解决方案**

分两步对齐：

1. **白名单路径统一去掉 `/api` 前缀**，只写 Servlet 路径，与 `requestMatchers` 保持一致：

```yaml
security:
  whitelist:
    - /auth/login
    - /auth/register
    - /auth/refresh
    - /error
    - /swagger-ui/**
    - /v3/api-docs/**
```

2. **`JwtAuthFilter.shouldNotFilter` 改用 `getServletPath()` 取路径**，与 `requestMatchers` 使用同一路径来源，彻底消除不一致：

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    return whitelist.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, servletPath));
}
```

修复后两处消费方均基于 Servlet 路径（不含 context-path）匹配，白名单配置只需维护一份，行为完全一致。

> **关键点**：`getRequestURI()` 返回含 context-path 的完整路径，`getServletPath()` 返回去掉 context-path 后的路径。Spring Security 的 `requestMatchers` 内部使用的是 Servlet 路径，Filter 中应统一使用 `getServletPath()` 而非 `getRequestURI()`，避免 context-path 带来的路径偏移。

---

## 前端：axios 拦截器

### 4、刷新 Token 复用 result 实例导致拦截器递归 + 成功拦截器吃掉 code 分支 + 误用 ref 包裹 DTO

**问题**

`result.ts` 的 401 拦截器内部调用了 `refresh()`，而 `refresh()` 又是基于同一个 `result` axios 实例发起的，刷新请求自然会再次进入这套拦截器。三个隐患叠加：

1. **拦截器递归 / 死锁**：`/auth/refresh` 自身返回 401 时，刷新请求进入 401 分支，看到 `store.refreshingPromise` 已存在（就是它自己），于是 `await store.refreshingPromise`——等待自己完成，必然死锁。即便 refresh 不返回 401，业务码 `code !== 1` 时成功拦截器会 `Promise.reject`，触发外层的 catch 分支链路，逻辑分裂、难以推理。
2. **成功拦截器吃掉 `else` 分支**：成功拦截器对 `code !== 1` 直接 reject，因此 `await refresh(...)` 拿到的 `res` 必然 `code === 1`。下游 `if (res?.code) { ... } else { ... }` 的 else 分支永远不可达，刷新失败的清理逻辑实际上写在了死代码里。
3. **错误地用 Vue 的 `ref` 包裹 DTO**：`refreshDTO` 只是个普通对象，没有响应式需求；`ref({ ... })` 之后还要 `.value` 取值，徒增复杂度和噪音。

**解决方案**

刷新 Token 走一个**不挂任何拦截器**的独立 axios 实例，与业务 `result` 实例彻底解耦；同时用原生抛错驱动外层 try/catch，不再依赖死代码分支。

```typescript
// 独立 axios 实例：用于刷新 Token，不挂任何拦截器
const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

async function callRefresh(refreshTokenStr: string): Promise<ApiResult<RefreshVO>> {
  const dto: RefreshDTO = { refreshToken: refreshTokenStr }
  const response = await refreshClient.post<ApiResult<RefreshVO>>('/auth/refresh', dto)
  return response.data
}
```

刷新 Promise 内部业务码不为 1 时直接 throw，让外层 catch 集中处理清理与跳转，避免死分支：

```typescript
const promise = (async () => {
  const res = await callRefresh(store.refreshToken)
  if (res?.code !== 1 || !res.data) {
    throw new Error(res?.msg || '登录已过期，请重新登录')
  }
  const { accessToken, refreshToken: newRefreshToken } = res.data
  store.setToken(accessToken, newRefreshToken)
})()

store.refreshingPromise = promise

try {
  await promise
} catch (e) {
  store.clearAuth()
  router.push('/login')
  ElMessage.error(e instanceof Error ? e.message : '登录已过期，请重新登录')
  return Promise.reject(error)
} finally {
  store.refreshingPromise = null
}
```

等待方也要捕获共享 Promise 的拒绝，防止首个刷新失败时其它请求未捕获导致 `unhandledrejection`：

```typescript
if (store.refreshingPromise) {
  try {
    await store.refreshingPromise
  } catch {
    return Promise.reject(error)
  }
  if (!store.token) return Promise.reject(error)
  error.config._retry = true
  setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
  return result(error.config)
}
```

> **关键点**：拦截器是**绑定在实例上**的——任何走该实例发起的请求都会触发拦截器。在拦截器内再用同一实例发起请求，等于把当前调用路径再嵌套一次，极易形成「等待自己 / 重入死循环 / 栈失控」。要在拦截器里发起辅助请求（刷新 Token、上报错误等），固定做法是另起一个干净的 axios 实例（或直接用 `axios.request`），物理隔离拦截链。另一种等价做法是给请求 config 打一个标记（如 `config._skipAuthRefresh`），拦截器里看到标记直接跳过 401 自刷逻辑——本质都是断开递归路径。最后，普通 DTO 不需要 `ref` 包裹，直接构造对象传入即可。

### 5、单拦截器内 onFulfilled reject 无法触发同一 use 的 onRejected，导致业务码错误无提示

**问题**

`result.ts` 最初将业务码转换和错误提示写在同一个 `use(onFulfilled, onRejected)` 里：`onFulfilled` 对 `code !== 1` 打上 `__business` 标记后 `Promise.reject`，期望同一个 `use` 的 `onRejected` 捕获并弹出提示。

这是对 axios 拦截器链模型的误解，导致业务码错误**完全不弹提示**，用户无感失败：

- `use(onFulfilled, onRejected)` 本质是 `.then(onFulfilled, onRejected)`
- `onFulfilled` 返回 `Promise.reject` 时，拒绝会传给**链上下一个节点**的 `onRejected`，而不是同一个 `use` 的 `onRejected`
- 当前只注册了一个拦截器，没有"下一个节点"，`__business` 错误直接穿透到调用方
- 调用方写了空 `catch {}`，错误被吞掉，用户看不到任何提示

**解决方案：拆成两个拦截器**

第一个拦截器只做转换（`onFulfilled` 把 `code !== 1` 转为带 `__business` 标记的 reject），第一个的 reject 自然流入第二个拦截器的 `onRejected`，在那里统一弹消息：

```ts
// 拦截器 1：转换，不弹消息
result.interceptors.response.use((response) => {
  const data = response.data
  if (data?.code !== 1) {
    const err = new Error(data?.msg || '请求失败') as Error & { __business?: boolean }
    err.__business = true
    return Promise.reject(err)
  }
  return data
})

// 拦截器 2：统一处理所有错误（业务码 + HTTP + 401 刷新）
result.interceptors.response.use(
  undefined,
  async (error) => {
    if (error?.__business) {
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    if (error.response?.status !== 401) {
      const msg = error.response?.data?.msg || error.message || '请求失败'
      ElMessage.error(msg)
      return Promise.reject(error)
    }
    // ...401 → 刷新 Token 流程
  },
)
```

拦截器 2 的 `onFulfilled` 传 `undefined`，表示成功路径直接透传，不做任何处理。

> **关键点**：axios 拦截器链的传递规则——`onFulfilled` 返回 reject（或 throw）时，错误流向**下一个**拦截器的 `onRejected`，而非同一个 `use` 的 `onRejected`。同一个 `use` 的 `onRejected` 只处理**上一个**节点传来的拒绝。要让业务码错误被 `onRejected` 捕获，必须把转换和处理拆到两个 `use` 里，形成真正的链式传递。

### 6、callRefresh 遇到 HTTP 错误时丢失后端 msg

**问题**

`callRefresh` 直接 `await refreshClient.post(...)`，当 `/auth/refresh` 返回 HTTP 400/401 时，axios 会 reject 一个 `AxiosError`，其 `.message` 是通用的 `"Request failed with status code 400"`。后端实际返回的 `msg`（如"refreshToken 已过期"）藏在 `error.response.data.msg` 里，上层 catch 拿不到，只能展示无意义的通用提示。

**解决方案：catch + 提取 msg 后 rethrow**

在 `callRefresh` 内部 catch `AxiosError`，用 `axios.isAxiosError()` 类型收窄后提取 `error.response?.data?.msg`，再 throw 一个携带真实 msg 的普通 `Error`，保持函数签名 `Promise<ApiResult<RefreshVO>>` 不变（只在成功时 resolve）：

```typescript
async function callRefresh(refreshTokenStr: string): Promise<ApiResult<RefreshVO>> {
  const dto: RefreshDTO = { refreshToken: refreshTokenStr }
  try {
    const response = await refreshClient.post<ApiResult<RefreshVO>>('/auth/refresh', dto)
    return response.data
  } catch (e) {
    const msg = axios.isAxiosError(e) ? e.response?.data?.msg : undefined
    throw new Error(msg || '登录已过期，请重新登录')
  }
}
```

上层 catch 已有 `e instanceof Error ? e.message : '...'` 的处理，后端 msg 自然透传到 `ElMessage.error`。

> **为什么不用 `validateStatus: () => true`**：该方案让 axios 对所有 HTTP 状态码都 resolve，函数返回类型就必须同时表达成功和失败两种形态，调用方需要额外判断，契约变复杂。HTTP 错误本就是异常路径，用 throw 表达更自然；`validateStatus` 适合需要统一处理所有状态码的场景（如代理转发），不适合这里。

### 7、并发 401 导致重复刷新 Token

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

### 8、Token 刷新失败后重试导致无限循环

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

### 9、等待共享刷新时刷新失败，等待方仍用空 Token 重试

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

---

## 后端：其他

### 10、Swagger 全局 addSecurityItem 导致匿名接口显示为需要授权

**问题**

`SwaggerConfig` 通过 `.addSecurityItem(new SecurityRequirement().addList("BearerAuth"))` 在 OpenAPI 全局声明了 BearerAuth，导致 `login`、`register`、`refresh` 等匿名接口在 Swagger UI 中也显示为需要授权（右上角锁图标为锁定状态）。这会误导接口调用者，也会影响客户端代码生成工具（如 OpenAPI Generator）为这些接口错误地生成携带 Token 的请求代码。

原有的 `@SecurityRequirement(name = "")` 是非标准 workaround——OpenAPI 规范要求 security requirement 的 name 必须对应 `components.securitySchemes` 中已声明的 scheme，空字符串是无效值，springdoc 对它的处理行为在不同版本间不一致，有些版本会渲染成一个空的 security 条目而非真正清空。

**解决方案：用 `@SecurityRequirements`（复数）显式覆盖为空**

`@SecurityRequirements`（无参数，value 为空数组）对应 OpenAPI 3 规范中的 `security: []`，明确表示"此操作覆盖全局 security，且不需要任何鉴权"，是规范定义的正确用法：

```java
// 匿名接口：显式覆盖全局 security 为空
@Operation(summary = "用户注册")
@PostMapping("/register")
@SecurityRequirements
public Result<Void> register(...) { ... }

@Operation(summary = "用户登录")
@PostMapping("/login")
@SecurityRequirements
public Result<LoginVO> login(...) { ... }

@Operation(summary = "刷新 Token")
@PostMapping("/refresh")
@SecurityRequirements
public Result<LoginVO> refresh(...) { ... }

// 需要鉴权的接口：不加注解，继承全局 BearerAuth
@Operation(summary = "获取当前用户信息")
@GetMapping("/me")
public Result<UserInfoVO> me() { ... }
```

同时将 import 从 `SecurityRequirement` 改为 `SecurityRequirements`。

> **关键点**：全局 `addSecurityItem` 是"默认需要鉴权"的声明，适合大多数接口都需要 Token 的场景，不需要在每个 Controller 上重复声明。少数匿名接口用 `@SecurityRequirements`（空数组）显式覆盖，比逐一添加 `@SecurityRequirement` 更简洁，也比 `name = ""` 的 workaround 更符合规范。

---

## 前端：其他

### 11、Vue Router 子路由无法继承父路由 meta，需用 to.matched.some 匹配

**问题**

Vue Router 中，子路由不会自动继承父路由的 `meta` 字段。在路由守卫里直接读 `to.meta.requiresAuth`，只能拿到当前匹配路由自身的 meta，父路由上定义的 `requiresAuth: true` 对子路由不可见：

```typescript
// ❌ 只读当前路由的 meta，父路由的 requiresAuth 对子路由无效
if (to.meta.requiresAuth && !store.isLoggedIn) {
  return next('/login')
}
```

例如将 `requiresAuth: true` 设置在 `/` 父路由上，期望其下所有子路由（`/`、`/post/:id`、`/settings` 等）都需要登录，但子路由的 `to.meta.requiresAuth` 为 `undefined`，守卫不会触发，未登录用户可以直接访问。

**解决方案**

用 `to.matched.some()` 遍历当前路由的完整匹配链（从根路由到当前路由的所有层级），只要链上任意一层声明了 `requiresAuth: true` 即触发守卫：

```typescript
// ✅ 遍历匹配链，父路由的 meta 对所有子路由生效
if (to.matched.some((r) => r.meta.requiresAuth) && !store.isLoggedIn) {
  return next('/login')
}
```

路由配置只需在父路由声明一次，子路由无需重复：

```typescript
{
  path: '/',
  component: () => import('@/components/LayoutPage.vue'),
  meta: { requiresAuth: true },   // 声明一次，所有子路由均受保护
  children: [
    { path: '', name: 'home', component: () => import('@/views/HomePage.vue') },
    { path: 'settings', name: 'settings', component: () => import('@/views/SettingsPage.vue') },
    // ...其他子路由无需重复声明 meta
  ],
}
```

> **关键点**：`to.meta` 只包含当前路由自身的 meta，`to.matched` 是从根到当前路由的完整路由记录数组。需要"继承"父路由 meta 的场景，必须用 `to.matched.some()` 或 `to.matched.find()` 遍历整条链，而不能直接读 `to.meta`。
