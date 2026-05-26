import result from '@/utils/result'
import type { PageResult, CursorPageResult, CommentVO, CreateCommentDTO, CreateCommentVO, LikeCommentVO } from '@/api/modules/index'

export function fetchComments(postId: number, params: { cursor?: string | null; size?: number; replySize?: number }) {
  return result.get<CursorPageResult<CommentVO>>(`/posts/${postId}/comments`, { params })
}

export function fetchReplies(commentId: number, params: { current?: number; size?: number }) {
  return result.get<PageResult<CommentVO>>(`/comments/${commentId}/replies`, { params })
}

export function createComment(postId: number, data: CreateCommentDTO) {
  return result.post<CreateCommentVO>(`/posts/${postId}/comments`, data)
}

export function likeComment(commentId: number) {
  return result.post<LikeCommentVO>(`/comments/${commentId}/like`)
}

export function deleteComment(commentId: number) {
  return result.delete(`/comments/${commentId}`)
}
