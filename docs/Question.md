# Axios 401 拦截器问题与解决方案

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
  error.config.headers.set('Authorization', `Bearer ${store.token}`)
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

**解决方案：`_retry` 标记**

在 `error.config` 上增加一个自定义标记 `_retry`。401 入口先检查该标记：若已为 `true` 说明这个请求已经走过一轮刷新流程，不再继续，直接清除登录态并跳转登录页。重试前设置 `_retry = true`，确保每个请求最多重试一次。

```typescript
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
  error.config.headers.set('Authorization', `Bearer ${store.token}`)
  return result(error.config)
}
```
