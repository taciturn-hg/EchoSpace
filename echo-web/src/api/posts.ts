import result from '@/utils/result'
import type {
  CursorPageResult,
  PageResult,
  PostListDTO,
  PostVO,
  PostDetailVO,
  LikePostVO,
  FavoritePostVO,
  CreatePostDTO,
  CreatePostVO,
  ApiResult,
} from '@/api/modules/index'

export const createPost = (dto: CreatePostDTO) =>
  result.post<unknown, ApiResult<CreatePostVO>>('/posts', dto)

export const fetchPosts = (params: PostListDTO = {}) =>
  result.get<unknown, ApiResult<CursorPageResult<PostVO>>>('/posts', { params })

export const fetchPostDetail = (id: number) =>
  result.get<unknown, ApiResult<PostDetailVO>>(`/posts/${id}`)

export const likePost = (id: number) =>
  result.post<unknown, ApiResult<LikePostVO>>(`/posts/${id}/like`)

export const favoritePost = (id: number) =>
  result.post<unknown, ApiResult<FavoritePostVO>>(`/posts/${id}/favorite`)

export const searchPosts = (params: { q?: string; current?: number; size?: number; sort?: string }) =>
  result.get<unknown, ApiResult<PageResult<PostVO>>>('/posts/search', { params })
