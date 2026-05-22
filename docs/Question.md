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
