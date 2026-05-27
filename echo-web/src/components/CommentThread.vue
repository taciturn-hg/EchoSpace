<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import CommentCard from '@/components/CommentCard.vue'
import CommentCreate from '@/components/CommentCreate.vue'
import { fetchReplies, likeComment, deleteComment } from '@/api/comments'
import type { CommentVO, CommentUser } from '@/api/modules/index'

const props = defineProps<{
  comment: CommentVO
  postId: number
}>()

const emit = defineEmits<{
  'reply-added': []
  'deleted': [commentId: number]
}>()

// ===== Reply pagination state =====
const REPLY_PAGE_SIZE = 10
const expanded = ref(false)
const paginatedReplies = ref<CommentVO[]>([])
const replyTotal = ref(0)
const replyPage = ref(1)
const loadingReplies = ref(false)

// ===== Parent comment local like state (avoid prop mutation) =====
const parentLiked = ref(props.comment.isLiked)
const parentLikeCount = ref(props.comment.likeCount)

// ===== Reply local like state (avoid prop mutation) =====
const replyLikes = ref<Record<number, { liked: boolean; likeCount: number }>>({})

// ===== Inline reply editor state =====
const replyingToId = ref<number | null>(null)

// ===== Computed =====
const visibleReplies = computed<CommentVO[]>(() => {
  if (expanded.value) return paginatedReplies.value
  return props.comment.replies ?? []
})

const hasMoreReplies = computed(() => props.comment.hasMoreReplies ?? false)

const showPagination = computed(() => expanded.value && replyTotal.value > REPLY_PAGE_SIZE)

/** 合并 prop reply 与本地 like 覆盖，避免直接 mutate props */
function resolvedReply(reply: CommentVO): CommentVO {
  const override = replyLikes.value[reply.id]
  if (!override) return reply
  return { ...reply, isLiked: override.liked, likeCount: override.likeCount }
}

const parentComment = computed<CommentVO>(() => ({
  ...props.comment,
  isLiked: parentLiked.value,
  likeCount: parentLikeCount.value,
}))

// ===== Load more replies → switch to paginated mode =====
async function loadMoreReplies() {
  expanded.value = true
  await loadPage(1)
}

async function loadPage(page: number) {
  loadingReplies.value = true
  try {
    const res = await fetchReplies(props.comment.id, {
      current: page,
      size: REPLY_PAGE_SIZE,
    })
    const result = res.data!
    paginatedReplies.value = result.records
    replyTotal.value = result.total
    replyPage.value = result.current
  } catch {
    ElMessage.error('加载回复失败')
  } finally {
    loadingReplies.value = false
  }
}

function handlePageChange(page: number) {
  loadPage(page)
}

// ===== Reply interaction =====
function handleReply(commentId: number) {
  replyingToId.value = commentId
}

function handleReplyCancel() {
  replyingToId.value = null
}

async function handleReplySubmitted() {
  replyingToId.value = null
  if (expanded.value) {
    await loadPage(replyPage.value)
  }
  emit('reply-added')
}

async function handleDelete(commentId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteComment(commentId)
    ElMessage.success('删除成功')

    if (commentId === props.comment.id) {
      emit('deleted', commentId)
    } else {
      if (expanded.value) {
        paginatedReplies.value = paginatedReplies.value.filter((r) => r.id !== commentId)
        replyTotal.value = Math.max(0, replyTotal.value - 1)
      }
      emit('reply-added')
    }
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}

// ===== Like interaction =====
async function handleToggleLike(commentId: number) {
  try {
    const res = await likeComment(commentId)
    const result = res.data!
    if (commentId === props.comment.id) {
      parentLiked.value = result.liked
      parentLikeCount.value = result.likeCount
    } else {
      replyLikes.value = {
        ...replyLikes.value,
        [commentId]: { liked: result.liked, likeCount: result.likeCount },
      }
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

// ===== Get reply target user info =====
function getReplyUser(commentId: number): CommentUser | null {
  if (commentId === props.comment.id) {
    return props.comment.user
  }
  const allReplies = expanded.value ? paginatedReplies.value : (props.comment.replies ?? [])
  const found = allReplies.find((r) => r.id === commentId)
  return found?.user ?? null
}
</script>

<template>
  <div class="comment-thread">
    <!-- ===== 一级评论 ===== -->
    <CommentCard
      :comment="parentComment"
      :is-liked="parentComment.isLiked"
      @toggle-like="handleToggleLike"
      @reply="handleReply"
      @delete="handleDelete"
    />

    <!-- ===== 二级回复区域（缩进） ===== -->
    <div class="comment-thread__replies">
      <!-- 回复一级评论的编辑器（插在二级回复列表最上方） -->
      <CommentCreate
        v-if="replyingToId === comment.id"
        :post-id="postId"
        :parent-id="comment.id"
        :reply-to-user="getReplyUser(comment.id)"
        @submitted="handleReplySubmitted"
        @cancel="handleReplyCancel"
      />

      <!-- 预加载的 3 条回复 / 分页回复 -->
      <template v-for="reply in visibleReplies" :key="reply.id">
        <CommentCard
          :comment="resolvedReply(reply)"
          :is-liked="resolvedReply(reply).isLiked"
          :hide-reply-target="reply.replyToUser?.id === comment.user.id"
          @toggle-like="handleToggleLike"
          @reply="handleReply"
          @delete="handleDelete"
        />
        <!-- 回复某条二级回复的编辑器（插在该回复下方） -->
        <CommentCreate
          v-if="replyingToId === reply.id"
          :post-id="postId"
          :parent-id="comment.id"
          :reply-to-user="getReplyUser(reply.id)"
          @submitted="handleReplySubmitted"
          @cancel="handleReplyCancel"
        />
      </template>

      <!-- 未展开：加载更多按钮 -->
      <button
        v-if="!expanded && hasMoreReplies"
        class="comment-thread__load-more"
        @click="loadMoreReplies"
      >
        加载更多评论
      </button>

      <!-- 展开后加载中 -->
      <p v-if="expanded && loadingReplies" class="comment-thread__loading">加载中...</p>

      <!-- 展开后：仅当超过一页时显示分页 -->
      <div v-if="showPagination && !loadingReplies" class="comment-thread__pagination">
        <ElPagination
          :current-page="replyPage"
          :page-size="REPLY_PAGE_SIZE"
          :total="replyTotal"
          layout="prev, pager, next"
          size="small"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$text-muted: #9ca3af;
$border-subtle: #f0f0f0;
$transition-fast: 150ms ease;

.comment-thread {
  background: transparent;
}

// ===== 二级回复区域 =====
.comment-thread__replies {
  padding-left: 46px;
  border-left: 2px solid $border-subtle;
  margin-left: 18px;
}

.comment-thread__load-more {
  display: block;
  margin: 8px auto 0;
  padding: 6px 20px;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: 13px;
  cursor: pointer;
  border-radius: 6px;
  transition:
    color $transition-fast,
    background $transition-fast;

  &:hover {
    color: #6b7280;
    background: #f3f4f6;
  }
}

.comment-thread__loading {
  text-align: center;
  color: $text-muted;
  font-size: 13px;
  padding: 10px 0;
  margin: 0;
}

.comment-thread__pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0 0;
}
</style>
