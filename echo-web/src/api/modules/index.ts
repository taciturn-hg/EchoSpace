// ===== 通用类型 =====

/** 后端统一响应格式 */
export interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

/** 分页响应格式 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// ===== 用户相关 =====

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar?: string
  bio?: string
}

// ===== 认证相关 =====

export interface LoginVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

// ===== 后续 Sprint 按需补充 =====
// PostVO, PostDetailVO, CommentVO, SearchVO ...
