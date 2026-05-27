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

// ===== 文件操作 =====

export interface UploadAvatarVO {
  url: string
}

export interface UploadImageVO {
  url: string
}

// ===== 帖子相关 =====

export interface PostAuthor {
  id: number
  username: string
  nickname: string | null
  avatar?: string | null
}

export interface PostVO {
  id: number
  title: string
  coverImage?: string | null
  contentText?: string | null
  author: PostAuthor
  likeCount: number
  commentCount: number
  collectCount: number
  createdAt: string
}

/** 帖子列表请求参数（游标分页） */
export interface PostListDTO {
  cursor?: string | null
  size?: number
  sort?: string
}

/** 游标分页响应 */
export interface CursorPageResult<T> {
  records: T[]
  cursor: string | null
  hasMore: boolean
  /** 本次返回的实际记录数 */
  count: number
}

// ===== 帖子详情相关 =====

export interface PostDetailVO {
  id: number
  title: string
  contentHtml: string
  author: PostAuthor
  likeCount: number
  commentCount: number
  collectCount: number
  viewCount: number
  isLiked: boolean
  isCollected: boolean
  isFollowed: boolean
  createdAt: string
  updatedAt: string
}

export interface CreatePostDTO {
  title: string
  contentHtml: string
}

export interface CreatePostVO {
  id: number
}

export interface LikePostVO {
  liked: boolean
  likeCount: number
}

export interface FavoritePostVO {
  favorited: boolean
}

export interface FollowVO {
  followed: boolean
}

// ===== 评论相关 =====

export interface CommentUser {
  id: number
  username: string
  nickname?: string | null
  avatar?: string | null
}

export interface ReplyToUser {
  id: number
  username: string
  nickname?: string | null
}

export interface CommentVO {
  id: number
  postId: number
  user: CommentUser
  content: string
  likeCount: number
  isLiked: boolean
  createdAt: string
  /** 一级评论专属：预加载的二级回复 */
  replies?: CommentVO[]
  /** 一级评论专属：二级回复总数 */
  replyCount?: number
  /** 一级评论专属：是否还有更多二级回复 */
  hasMoreReplies?: boolean
  /** 二级回复专属：父评论ID */
  parentId?: number
  /** 二级回复专属：被回复的用户 */
  replyToUser?: ReplyToUser
}

export interface CreateCommentDTO {
  parentId: number
  replyToUid?: number
  content: string
}

export interface CreateCommentVO {
  id: number
}

export interface LikeCommentVO {
  liked: boolean
  likeCount: number
}
