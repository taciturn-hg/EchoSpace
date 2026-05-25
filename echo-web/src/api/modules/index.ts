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

export interface RefreshDTO {
  refreshToken: string
}

export interface RefreshVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface MeVO {
  id: number
  username: string
  nickname: string | null
  email: string
  phone: string
  avatar?: string
  bio?: string
  createdAt: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string | null
  email: string
  phone: string
  avatar?: string
  bio?: string
}

// ===== 用户资料相关 =====
export interface UserProfileVO {
  avatar: string | null
  nickname: string | null
  bio: string | null
}

export interface UpdateProfileDTO {
  avatar?: string
  nickname?: string
  bio?: string
}

export interface UploadAvatarVO {
  url: string
}

// ===== 用户账号设置相关 =====
export interface UserSettingsVO {
  phone: string | null
  email: string | null
}

export interface UpdateSettingsDTO {
  phone?: string
  email?: string
}

// ===== Axios 类型扩展 =====

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    /** 401 重试标记，防止无限循环刷新 Token */
    _retry?: boolean
  }
}

// ===== 修改密码 =====
export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// ===== 后续 Sprint 按需补充 =====
// PostVO, PostDetailVO, CommentVO, SearchVO ...
