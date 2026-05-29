<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElAvatar } from 'element-plus'
import { Heart, MessageCircle, Trash2 } from '@lucide/vue'
import { useUserStore } from '@/stores/userStore'
import type { CommentVO } from '@/api/modules/index'
import { formatRelativeTime, formatDateTime } from '@/utils/time'
import { formatCount } from '@/utils/number'

const props = withDefaults(
  defineProps<{
    comment: CommentVO
    isLiked?: boolean
    hideReplyTarget?: boolean
  }>(),
  {
    isLiked: false,
    hideReplyTarget: false,
  },
)

const emit = defineEmits<{
  'toggle-like': [commentId: number]
  'reply': [commentId: number]
  'delete': [commentId: number]
}>()

const router = useRouter()
const userStore = useUserStore()

const currentUserId = computed(() => userStore.userInfo?.id)

const displayName = computed(() => props.comment.user.nickname || props.comment.user.username)

const replyTargetName = computed(() => {
  if (!props.comment.replyToUser) return ''
  return props.comment.replyToUser.nickname || props.comment.replyToUser.username
})

const relativeTime = computed(() => formatRelativeTime(props.comment.createdAt))
const absoluteTime = computed(() => formatDateTime(props.comment.createdAt))

const liked = computed(() => props.isLiked || props.comment.isLiked)

function handleUserClick() {
  router.push(`/user/${props.comment.user.id}`)
}

function handleLikeClick(e: MouseEvent) {
  e.stopPropagation()
  emit('toggle-like', props.comment.id)
}

function handleReplyClick(e: MouseEvent) {
  e.stopPropagation()
  emit('reply', props.comment.id)
}

function handleDeleteClick(e: MouseEvent) {
  e.stopPropagation()
  emit('delete', props.comment.id)
}
</script>

<template>
  <div class="comment-card">
    <!-- ===== 用户部分 ===== -->
    <header class="comment-card__header" tabindex="0" role="link" @click="handleUserClick">
      <ElAvatar :src="comment.user.avatar ?? undefined" :size="36" class="comment-card__avatar">
        {{ displayName.charAt(0) }}
      </ElAvatar>
      <div class="comment-card__user-info">
        <div class="comment-card__name-row">
          <span class="comment-card__nickname">{{ displayName }}</span>
          <span v-if="replyTargetName && !hideReplyTarget" class="comment-card__reply-target">@{{ replyTargetName }}</span>
        </div>
        <time class="comment-card__time">{{ relativeTime }}</time>
      </div>
    </header>

    <!-- ===== 评论内容 + 底部操作栏 ===== -->
    <div class="comment-card__body">
      <p class="comment-card__content">{{ comment.content }}</p>

      <div class="comment-card__meta">
        <time class="comment-card__datetime">{{ absoluteTime }}</time>
        <div class="comment-card__actions">
          <button
            :class="['comment-card__action', { 'comment-card__action--liked': liked }]"
            aria-label="点赞"
            @click="handleLikeClick"
          >
            <Heart :size="16" :fill="liked ? 'currentColor' : 'none'" />
            <span>{{ formatCount(comment.likeCount || 0) }}</span>
          </button>
          <button class="comment-card__action" aria-label="回复" @click="handleReplyClick">
            <MessageCircle :size="16" />
            <span>回复</span>
          </button>
          <button
            v-if="currentUserId && comment.user.id === currentUserId"
            class="comment-card__action comment-card__action--delete"
            aria-label="删除"
            @click="handleDeleteClick"
          >
            <Trash2 :size="16" />
            <span>删除</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// ===== Design Tokens =====
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent-red: #ef4444;
$transition-fast: 150ms ease;

// ===== Card Container (borderless) =====
.comment-card {
  background: transparent;
  padding: 14px 0;
}

// ===== Header: User =====
.comment-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  outline: none;
  border-radius: 8px;
  padding: 2px 4px;
  margin: -2px -4px;
  transition: background $transition-fast;

  &:hover,
  &:focus-visible {
    background: #f8f9fa;
  }
}

.comment-card__avatar {
  flex-shrink: 0;
  cursor: pointer;
}

.comment-card__user-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  min-width: 0;
  height: 36px;
}

.comment-card__name-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.comment-card__nickname {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-card__reply-target {
  font-size: 13px;
  color: $text-muted;
  line-height: 1.3;
  white-space: nowrap;
  flex-shrink: 0;
}

.comment-card__time {
  font-size: 12px;
  color: $text-muted;
  line-height: 1.3;
}

// ===== Body =====
.comment-card__body {
  margin-top: 8px;
  padding-left: 46px;
}

.comment-card__content {
  margin: 0;
  font-size: 14px;
  color: $text-primary;
  line-height: 1.7;
  word-break: break-word;
}

// ===== Bottom Meta Row =====
.comment-card__meta {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.comment-card__datetime {
  width: 25%;
  font-size: 12px;
  color: $text-muted;
  flex-shrink: 0;
}

.comment-card__actions {
  width: 37.5%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  margin-left: auto;
}

.comment-card__action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
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
    background: #f3f4f6;
  }

  &--liked {
    color: $accent-red;
  }

  &--delete {
    &:hover {
      color: $accent-red;
      background: #fef2f2;
    }
  }
}
</style>
