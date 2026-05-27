import result from '@/utils/result'
import type {
  CursorPageResult,
  PostListDTO,
  PostVO,
  PostDetailVO,
  LikePostVO,
  FavoritePostVO,
  CreatePostDTO,
  CreatePostVO,
  UpdatePostDTO,
  ApiResult,
} from '@/api/modules/index'

export const createPost = (dto: CreatePostDTO) =>
  result.post<unknown, ApiResult<CreatePostVO>>('/posts', dto)

export const updatePost = (id: number, dto: UpdatePostDTO) =>
  result.put<unknown, ApiResult<void>>(`/posts/${id}`, dto)

export const deletePost = (id: number) =>
  result.delete<unknown, ApiResult<void>>(`/posts/${id}`)

export const fetchPosts = (params: PostListDTO = {}) =>
  result.get<unknown, ApiResult<CursorPageResult<PostVO>>>('/posts', { params })

export const fetchUserPosts = (userId: number, params: PostListDTO = {}) =>
  result.get<unknown, ApiResult<CursorPageResult<PostVO>>>(`/users/${userId}/posts`, { params })

export const fetchPostDetail = (id: number) =>
  result.get<unknown, ApiResult<PostDetailVO>>(`/posts/${id}`)

export const likePost = (id: number) =>
  result.post<unknown, ApiResult<LikePostVO>>(`/posts/${id}/like`)

export const favoritePost = (id: number) =>
  result.post<unknown, ApiResult<FavoritePostVO>>(`/posts/${id}/favorite`)
