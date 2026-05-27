import result from '@/utils/result'
import type {
  PageResult,
  CursorPageResult,
  CommentVO,
  CreateCommentDTO,
  CreateCommentVO,
  LikeCommentVO,
  ApiResult,
} from '@/api/modules/index'

export const fetchComments = (postId: number, params: { cursor?: string | null; size?: number; replySize?: number }) =>
  result.get<unknown, ApiResult<CursorPageResult<CommentVO>>>(`/posts/${postId}/comments`, { params })

export const fetchReplies = (commentId: number, params: { current?: number; size?: number }) =>
  result.get<unknown, ApiResult<PageResult<CommentVO>>>(`/comments/${commentId}/replies`, { params })

export const createComment = (postId: number, data: CreateCommentDTO) =>
  result.post<unknown, ApiResult<CreateCommentVO>>(`/posts/${postId}/comments`, data)

export const likeComment = (commentId: number) =>
  result.post<unknown, ApiResult<LikeCommentVO>>(`/comments/${commentId}/like`)

export const deleteComment = (commentId: number) =>
  result.delete<unknown, ApiResult<void>>(`/comments/${commentId}`)
