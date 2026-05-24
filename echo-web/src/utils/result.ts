import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import router from '@/router'
import { setHeader } from './header'
import type { ApiResult, RefreshDTO, RefreshVO } from '@/api/modules/index'

let userStore: ReturnType<typeof useUserStore>

function getStore() {
  if (!userStore) {
    userStore = useUserStore()
  }
  return userStore
}

const result = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 独立 axios 实例：用于刷新 Token，不挂任何拦截器
// 避免在 result 的 401 拦截器里再次走 result，造成拦截器递归 / 死锁
const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

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

// 请求拦截器：注入 Token
result.interceptors.request.use(
  (config) => {
    const token = getStore().token
    if (token) {
      setHeader(config, 'Authorization', `Bearer ${token}`)
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：业务错误与 HTTP 错误统一在 error 分支提示，避免「弹两次」
// 同时 401 触发自动刷新（Promise 锁 + _retry 防无限循环）
result.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data?.code !== 1) {
      // 仅 reject，不在此处弹消息；error 分支会通过 __business 标记识别并统一提示
      const err = new Error(data?.msg || '请求失败') as Error & { __business?: boolean }
      err.__business = true
      return Promise.reject(err)
    }
    return data
  },
  async (error) => {
    // 业务码错误（来自 success 分支 reject）：在此处统一弹一次后向上抛
    if (error?.__business) {
      ElMessage.error(error.message)
      return Promise.reject(error)
    }

    if (error.response?.status !== 401) {
      const msg = error.response?.data?.msg || error.message || '请求失败'
      ElMessage.error(msg)
      return Promise.reject(error)
    }

    // config 不存在则无法重试，直接拒绝
    if (!error.config) {
      return Promise.reject(error)
    }

    const store = getStore()

    // 已重试过一次，不再继续，直接踢到登录页
    if (error.config._retry) {
      store.clearAuth()
      router.push('/login')
      return Promise.reject(error)
    }

    if (!store.refreshToken) {
      store.clearAuth()
      router.push('/login')
      return Promise.reject(error)
    }

    // 已有刷新在进行中，等它完成
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

    // 发起刷新（使用独立 refreshClient，避开 result 拦截器）
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

    error.config._retry = true
    setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
    return result(error.config)
  },
)

export default result
