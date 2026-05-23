import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import router from '@/router'
import { setHeader } from './header.ts'

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

// 响应拦截器：Token 过期自动刷新（Promise 锁 + _retry 防无限循环）
result.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data?.code !== 1) {
      ElMessage.error(data?.msg || '请求失败')
      return Promise.reject(new Error(data?.msg || '请求失败'))
    }
    return data
  },
  async (error) => {
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
      await store.refreshingPromise
      if (!store.token) return Promise.reject(error)
      error.config._retry = true
      setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
      return result(error.config)
    }

    // 发起刷新
    const promise = (async () => {
      try {
        const res = await axios.post(`${result.defaults.baseURL}/auth/refresh`, {
          refreshToken: store.refreshToken,
        })
        if (res.data?.code) {
          const { accessToken, refreshToken: newRefreshToken } = res.data.data
          store.setToken(accessToken, newRefreshToken)
        } else {
          store.clearAuth()
          router.push('/login')
          ElMessage.error(res.data.msg)
        }
      } catch {
        store.clearAuth()
        router.push('/login')
        throw new Error('refresh failed')
      }
    })()

    store.refreshingPromise = promise

    try {
      await promise
    } finally {
      store.refreshingPromise = null
    }

    if (!store.token) return Promise.reject(error)

    error.config._retry = true
    setHeader(error.config, 'Authorization', `Bearer ${store.token}`)
    return result(error.config)
  },
)

export default result
