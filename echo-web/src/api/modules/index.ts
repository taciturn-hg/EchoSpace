// ===== 通用类型 =====

/** 后端统一响应格式 */
export interface ApiResult<T> {
  code: number
  msg: string
  data: T | null
}

/** 分页响应格式 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// ===== 认证相关 =====
export interface LoginDTO {
  account: string
  password: string
}

export interface LoginVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface RegisterDTO {
  username: string
  phone: string
  email: string
  password: string
  confirmPassword: string
}

export interface RefreshVO {
  accessToken: string
  refreshToken: string
  expiresIn: string
}

export interface meVO {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar?: string
  bio?: string
  createdAt: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar?: string
  bio?: string
}

// ===== Axios 类型扩展 =====

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    /** 401 重试标记，防止无限循环刷新 Token */
    _retry?: boolean
  }
}

// ===== 后续 Sprint 按需补充 =====
// PostVO, PostDetailVO, CommentVO, SearchVO ...
