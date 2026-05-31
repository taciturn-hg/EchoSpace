<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElAvatar, ElMessage } from 'element-plus'
import { MessageCircle, Heart, Bookmark, Share2, Pencil, Trash2 } from '@lucide/vue'
import type { PostVO } from '@/api/modules/index'
import { formatRelativeTime } from '@/utils/time'
import { formatCount } from '@/utils/number'
import { likePost, favoritePost } from '@/api/posts'

const props = withDefaults(
  defineProps<{
    post: PostVO
    images?: string[]
    showActions?: boolean
  }>(),
  {
    images: () => [],
    showActions: false,
  },
)

// ===== Emits =====
const emit = defineEmits<{
  'toggle-like': [postId: number]
  'toggle-collect': [postId: number]
  edit: [postId: number]
  delete: [postId: number]
}>()

// ===== Router =====
const router = useRouter()

const liked = ref(props.post.isLiked || false)
const collected = ref(props.post.isCollected || false)
const likeCount = ref(props.post.likeCount || 0)
const collectCount = ref(props.post.collectCount || 0)
const submittingLike = ref(false)
const submittingCollect = ref(false)
const previewVisible = ref(false)
const previewSrc = ref('')

const images = computed(() => {
  if (props.images.length > 0) return props.images.slice(0, 3)
  if (props.post.coverImage) return [props.post.coverImage]
  return []
})

const imageCount = computed(() => images.value.length)
const imageGridClass = computed(() => `images--count-${imageCount.value}`)

const displayName = computed(() => props.post.author.nickname || props.post.author.username)

function handleUserClick() {
  router.push(`/user/${props.post.author.id}`)
}

function handleBodyClick() {
  router.push(`/post/${props.post.id}`)
}

function handleImageClick(src: string, e: MouseEvent) {
  e.stopPropagation()
  previewSrc.value = src
  previewVisible.value = true
}

function handleCommentClick(e: MouseEvent) {
  e.stopPropagation()
  router.push(`/post/${props.post.id}#comments`)
}

async function handleLikeClick(e: MouseEvent) {
  e.stopPropagation()
  if (submittingLike.value) return
  submittingLike.value = true

  const prevLiked = liked.value
  const prevCount = likeCount.value
  liked.value = !liked.value
  likeCount.value += liked.value ? 1 : -1

  try {
    const res = await likePost(props.post.id)
    liked.value = res.data!.liked
  } catch {
    liked.value = prevLiked
    likeCount.value = prevCount
    ElMessage.error('操作失败')
  } finally {
    submittingLike.value = false
  }
}

async function handleCollectClick(e: MouseEvent) {
  e.stopPropagation()
  if (submittingCollect.value) return
  submittingCollect.value = true

  const prevCollected = collected.value
  const prevCount = collectCount.value
  collected.value = !collected.value
  collectCount.value += collected.value ? 1 : -1

  try {
    const res = await favoritePost(props.post.id)
    collected.value = res.data!.favorited
  } catch {
    collected.value = prevCollected
    collectCount.value = prevCount
    ElMessage.error('操作失败')
  } finally {
    submittingCollect.value = false
  }
}

function handleShareClick(e: MouseEvent) {
  e.stopPropagation()
  ElMessage.info('该功能还在开发')
}

function handleEditClick(e: MouseEvent) {
  e.stopPropagation()
  emit('edit', props.post.id)
}

function handleDeleteClick(e: MouseEvent) {
  e.stopPropagation()
  emit('delete', props.post.id)
}

function closePreview() {
  previewVisible.value = false
}

watch(previewVisible, (visible) => {
  if (visible) {
    document.addEventListener('keydown', onEscape)
  } else {
    document.removeEventListener('keydown', onEscape)
  }
})

function onEscape(e: KeyboardEvent) {
  if (e.key === 'Escape') closePreview()
}

onUnmounted(() => {
  document.removeEventListener('keydown', onEscape)
})
</script>

<template>
  <article class="post-card">
    <!-- ===== 用户部分 ===== -->
    <header class="post-card__header">
      <div class="post-card__header-left" tabindex="0" role="link" @click="handleUserClick">
        <ElAvatar :src="post.author.avatar ?? undefined" :size="40" class="post-card__avatar">
          {{ displayName.charAt(0) }}
        </ElAvatar>
        <div class="post-card__user-info">
          <span class="post-card__nickname">{{ displayName }}</span>
          <time class="post-card__time">{{ formatRelativeTime(post.createdAt) }}</time>
        </div>
      </div>

      <div v-if="showActions" class="post-card__header-actions">
        <button
          class="post-card__action-btn post-card__action-btn--edit"
          aria-label="编辑"
          @click="handleEditClick"
        >
          <Pencil :size="15" />
        </button>
        <button
          class="post-card__action-btn post-card__action-btn--delete"
          aria-label="删除"
          @click="handleDeleteClick"
        >
          <Trash2 :size="15" />
        </button>
      </div>
    </header>

    <hr class="post-card__divider" />

    <!-- ===== 帖子部分 ===== -->
    <div class="post-card__body" @click="handleBodyClick">
      <h3 class="post-card__title" v-html="post.title" />
      <p v-if="post.contentText" class="post-card__summary" v-html="post.contentText" />

      <!-- 图片列表 -->
      <div v-if="imageCount > 0" :class="['post-card__images', imageGridClass]">
        <img
          v-for="(src, i) in images"
          :key="i"
          :src="src"
          :alt="`${post.title} 图片 ${i + 1}`"
          class="post-card__image"
          loading="lazy"
          @click="handleImageClick(src, $event)"
        />
      </div>
    </div>

    <hr class="post-card__divider" />

    <!-- ===== 交互部分 ===== -->
    <footer class="post-card__footer">
      <button class="post-card__action" aria-label="评论" @click="handleCommentClick">
        <MessageCircle :size="18" />
        <span>{{ formatCount(post.commentCount || 0) }}</span>
      </button>

      <button
        :class="['post-card__action', { 'post-card__action--liked': liked }]"
        aria-label="点赞"
        @click="handleLikeClick"
      >
        <Heart :size="18" :fill="liked ? 'currentColor' : 'none'" />
        <span>{{ formatCount(likeCount || 0) }}</span>
      </button>

      <button
        :class="['post-card__action', { 'post-card__action--collected': collected }]"
        aria-label="收藏"
        @click="handleCollectClick"
      >
        <Bookmark :size="18" :fill="collected ? 'currentColor' : 'none'" />
        <span>{{ formatCount(collectCount || 0) }}</span>
      </button>

      <button class="post-card__action" aria-label="分享" @click="handleShareClick">
        <Share2 :size="18" />
      </button>
    </footer>

    <!-- ===== 图片预览遮罩 ===== -->
    <Teleport to="body">
      <Transition name="preview-fade">
        <div v-if="previewVisible" class="image-preview-overlay" @click="closePreview">
          <button class="image-preview-close" aria-label="关闭预览" @click="closePreview">
            <svg
              viewBox="0 0 24 24"
              width="24"
              height="24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
          <img :src="previewSrc" class="image-preview-content" @click.stop />
        </div>
      </Transition>
    </Teleport>
  </article>
</template>

<style lang="scss" scoped>
// ===== Design Tokens =====
$bg-card: #ffffff;
$border-color: #e8eaed;
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent-red: #ef4444;
$accent-yellow: #eab308;
$accent-blue: #3b82f6;
$radius-card: 12px;
$radius-image: 8px;
$transition-fast: 150ms ease;

// ===== Card Container =====
.post-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-card;
  padding: 20px 24px;
  transition:
    box-shadow $transition-fast,
    border-color $transition-fast;
  cursor: default;

  &:hover {
    box-shadow:
      0 1px 3px rgba(0, 0, 0, 0.06),
      0 4px 12px rgba(0, 0, 0, 0.04);
    border-color: #d1d5db;
  }
}

// ===== Header: User =====
.post-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-card__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
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

.post-card__header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.post-card__action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid $border-color;
  background: $bg-card;
  border-radius: 6px;
  cursor: pointer;
  transition:
    color $transition-fast,
    background $transition-fast,
    border-color $transition-fast;

  &--edit {
    color: $text-muted;

    &:hover {
      color: $accent-blue;
      background: #eff6ff;
      border-color: $accent-blue;
    }
  }

  &--delete {
    color: $text-muted;

    &:hover {
      color: $accent-red;
      background: #fef2f2;
      border-color: $accent-red;
    }
  }
}

.post-card__avatar {
  flex-shrink: 0;
  cursor: pointer;
}

.post-card__user-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  min-width: 0;
  height: 40px;
}

.post-card__nickname {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-card__time {
  font-size: 12px;
  color: $text-muted;
  line-height: 1.3;
}

// ===== Body: Post Content =====
.post-card__body {
  margin-top: 16px;
  cursor: pointer;
  border-radius: 8px;
  padding: 4px;
  margin-left: -4px;
  margin-right: -4px;
  transition: background $transition-fast;

  &:hover {
    background: #fafbfc;
  }
}

.post-card__title {
  font-size: 17px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card__summary {
  margin: 8px 0 0;
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card__title,
.post-card__summary {
  :deep(em) {
    font-style: normal;
    color: #2563eb;
    background: rgba(37, 99, 235, 0.08);
    border-radius: 3px;
    padding: 0 2px;
  }
}

// ===== Images =====
.post-card__images {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.post-card__image {
  object-fit: cover;
  border-radius: $radius-image;
  cursor: pointer;
  transition: opacity $transition-fast;

  &:hover {
    opacity: 0.85;
  }
}

// Image grid variants
.images--count-1 {
  .post-card__image {
    width: 60%;
    max-width: 460px;
    max-height: 400px;
  }
}

.images--count-2 {
  .post-card__image {
    width: calc((100% - 10px) / 2);
    aspect-ratio: 4 / 3;
  }
}

.images--count-3 {
  .post-card__image {
    width: calc((100% - 20px) / 3);
    aspect-ratio: 4 / 3;
  }
}

// ===== Footer: Actions =====
.post-card__footer {
  display: flex;
  margin-top: 14px;
}

.post-card__divider {
  border: none;
  border-top: 1px solid #f3f4f6;
  margin: 14px 0 0;
}

.post-card__action {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 0;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px;
  transition:
    color $transition-fast,
    background $transition-fast;

  &:hover {
    background: #f3f4f6;
  }

  // Liked state
  &--liked {
    color: $accent-red;
  }

  // Collected state
  &--collected {
    color: $accent-yellow;
  }
}

// ===== Image Preview Overlay =====
.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.image-preview-close {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }
}

.image-preview-content {
  max-width: 88vw;
  max-height: 88vh;
  object-fit: contain;
  border-radius: 4px;
}

// ===== Transitions =====
.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.2s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}
</style>
