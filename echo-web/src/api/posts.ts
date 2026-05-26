import result from '@/utils/result'
import type { CursorPageResult, PostListDTO, PostVO, PostDetailVO, LikePostVO, FavoritePostVO, CreatePostDTO, CreatePostVO } from '@/api/modules/index'

export function createPost(dto: CreatePostDTO) {
  return result.post<CreatePostVO>('/posts', dto)
}

export function fetchPosts(params: PostListDTO = {}) {
  return result.get<CursorPageResult<PostVO>>('/posts', { params })
}

export function fetchPostDetail(id: number) {
  return result.get<PostDetailVO>(`/posts/${id}`)
}

export function likePost(id: number) {
  return result.post<LikePostVO>(`/posts/${id}/like`)
}

export function favoritePost(id: number) {
  return result.post<FavoritePostVO>(`/posts/${id}/favorite`)
}
