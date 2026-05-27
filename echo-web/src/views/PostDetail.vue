<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAvatar, ElMessage } from 'element-plus'
import {
  Eye,
  MessageCircle,
  Heart,
  Bookmark,
  Share2,
  ArrowLeft,
  UserPlus,
  UserCheck,
} from '@lucide/vue'
import DOMPurify from 'dompurify'
import { fetchPostDetail, likePost, favoritePost } from '@/api/posts'
import { fetchComments as apiFetchComments } from '@/api/comments'
import { followUser } from '@/api/users'
import { useInfiniteList } from '@/composables/useInfiniteList'
import CommentCreate from '@/components/CommentCreate.vue'
import CommentThread from '@/components/CommentThread.vue'
import { formatRelativeTime, formatDateTime } from '@/utils/time'
import type { PostDetailVO, CommentVO } from '@/api/modules/index'

// ===== Router =====
const route = useRoute()
const router = useRouter()

// ===== Post data =====
const post = ref<PostDetailVO | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)

// ===== Local interaction state (optimistic) =====
const liked = ref(false)
const collected = ref(false)
const followed = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const submitting = ref({ like: false, collect: false, follow: false })

// ===== Computed =====
const postId = computed(() => Number(route.params.id))
const displayName = computed(() => {
  if (!post.value) return ''
  return post.value.author.nickname || post.value.author.username
})
const createdAt = computed(() => (post.value ? formatRelativeTime(post.value.createdAt) : ''))
const updatedAt = computed(() => (post.value ? formatDateTime(post.value.updatedAt) : ''))
const sanitizedHtml = computed(() => {
  if (!post.value?.contentHtml) return ''
  return DOMPurify.sanitize(post.value.contentHtml)
})

// ===== Comments infinite list =====
const {
  posts: comments,
  loading: commentsLoading,
  hasMore: commentsHasMore,
  fetchPosts: fetchCommentsList,
  reset: resetComments,
} = useInfiniteList<CommentVO>({
  fetchFn: (params: Record<string, unknown>) => {
    return apiFetchComments(postId.value, {
      cursor: (params.cursor as string | null) ?? null,
      size: (params.size as number) ?? 12,
      replySize: 3,
    }) as unknown as Promise<{ data: Record<string, unknown> }>
  },
  mode: 'cursor',
  pageSize: 12,
})

// ===== Load post detail =====
async function loadPost() {
  loading.value = true
  loadError.value = null
  try {
    const res = await fetchPostDetail(postId.value)
    const data = res.data!
    post.value = data
    liked.value = data.isLiked
    collected.value = data.isCollected
    followed.value = data.isFollowed
    likeCount.value = data.likeCount
    collectCount.value = data.collectCount
  } catch {
    loadError.value = '帖子加载失败，请稍后重试'
    ElMessage.error('帖子加载失败')
  } finally {
    loading.value = false
  }
}

// ===== Interaction handlers =====
async function handleToggleLike() {
  if (submitting.value.like || !post.value) return
  submitting.value.like = true

  const prevLiked = liked.value
  const prevCount = likeCount.value
  liked.value = !liked.value
  likeCount.value += liked.value ? 1 : -1

  try {
    const res = await likePost(post.value.id)
    const result = res.data!
    liked.value = result.liked
    likeCount.value = result.likeCount
  } catch {
    liked.value = prevLiked
    likeCount.value = prevCount
    ElMessage.error('操作失败')
  } finally {
    submitting.value.like = false
  }
}

async function handleToggleCollect() {
  if (submitting.value.collect || !post.value) return
  submitting.value.collect = true

  const prevCollected = collected.value
  collected.value = !collected.value

  try {
    const res = await favoritePost(post.value.id)
    const result = res.data!
    collected.value = result.favorited
  } catch {
    collected.value = prevCollected
    ElMessage.error('操作失败')
  } finally {
    submitting.value.collect = false
  }
}

async function handleToggleFollow() {
  if (submitting.value.follow || !post.value) return
  submitting.value.follow = true

  const prevFollowed = followed.value
  followed.value = !followed.value

  try {
    const res = await followUser(post.value.author.id)
    const result = res.data!
    followed.value = result.followed
    ElMessage.success(followed.value ? '已关注' : '已取消关注')
  } catch {
    followed.value = prevFollowed
    ElMessage.error('操作失败')
  } finally {
    submitting.value.follow = false
  }
}

function handleShare() {
  ElMessage.info('该功能还在开发')
}

function handleUserClick() {
  if (!post.value) return
  router.push(`/user/${post.value.author.id}`)
}

function handleBack() {
  router.push('/')
}

function handleCommentClick() {
  const el = document.querySelector('.post-detail__comments')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function handleCommentSubmitted() {
  resetComments()
  fetchCommentsList()
}

// ===== Lifecycle =====
onMounted(() => {
  loadPost()
  fetchCommentsList()
})

// Reload when navigating between posts
watch(postId, () => {
  loadPost()
  resetComments()
  nextTick(() => fetchCommentsList())
})
</script>

<template>
  <div class="post-detail">
    <!-- ===== Back Button ===== -->
    <button class="post-detail__back" aria-label="返回首页" @click="handleBack">
      <ArrowLeft :size="20" />
    </button>

    <!-- ===== Loading State ===== -->
    <template v-if="loading">
      <div class="post-detail__card post-detail__skeleton">
        <div class="skeleton-line skeleton-line--title"></div>
        <div class="skeleton-line skeleton-line--text"></div>
        <div class="skeleton-line skeleton-line--text skeleton-line--text--short"></div>
      </div>
    </template>

    <!-- ===== Error State ===== -->
    <template v-else-if="loadError">
      <div class="post-detail__card post-detail__error">
        <p class="post-detail__error-text">{{ loadError }}</p>
        <button class="post-detail__retry-btn" @click="loadPost">重试</button>
      </div>
    </template>

    <!-- ===== Post Content Card ===== -->
    <template v-else-if="post">
      <article class="post-detail__card">
        <!-- User Area -->
        <header class="post-card__header" tabindex="0" role="link" @click="handleUserClick">
          <ElAvatar :src="post.author.avatar ?? undefined" :size="42" class="post-card__avatar">
            {{ displayName.charAt(0) }}
          </ElAvatar>
          <div class="post-card__user-info">
            <span class="post-card__nickname">{{ displayName }}</span>
            <time class="post-card__time">{{ createdAt }}</time>
          </div>
          <button
            :class="['post-card__follow-btn', { 'post-card__follow-btn--followed': followed }]"
            :disabled="submitting.follow"
            @click.stop="handleToggleFollow"
          >
            <component :is="followed ? UserCheck : UserPlus" :size="16" />
            <span>{{ followed ? '已关注' : '关注' }}</span>
          </button>
        </header>

        <hr class="post-card__divider" />

        <!-- Post Body -->
        <div class="post-card__body">
          <!-- View count -->
          <div class="post-card__views">
            <Eye :size="15" />
            <span>{{ post.viewCount ?? 0 }} 次浏览</span>
          </div>

          <!-- Title -->
          <h1 class="post-card__title">{{ post.title }}</h1>

          <!-- Updated time (if edited) -->
          <time v-if="post.updatedAt !== post.createdAt" class="post-card__edited">
            编辑于 {{ updatedAt }}
          </time>

          <!-- Content HTML -->
          <div class="post-card__content" v-html="sanitizedHtml"></div>

          <!-- Interaction Buttons -->
          <div class="post-card__actions-row">
            <button class="post-card__action" aria-label="评论" @click="handleCommentClick">
              <MessageCircle :size="18" />
              <span>{{ post.commentCount || 0 }}</span>
            </button>

            <button
              :class="['post-card__action', { 'post-card__action--liked': liked }]"
              aria-label="点赞"
              :disabled="submitting.like"
              @click="handleToggleLike"
            >
              <Heart :size="18" :fill="liked ? 'currentColor' : 'none'" />
              <span>{{ likeCount || 0 }}</span>
            </button>

            <button
              :class="['post-card__action', { 'post-card__action--collected': collected }]"
              aria-label="收藏"
              :disabled="submitting.collect"
              @click="handleToggleCollect"
            >
              <Bookmark :size="18" :fill="collected ? 'currentColor' : 'none'" />
              <span>{{ collectCount || 0 }}</span>
            </button>

            <button class="post-card__action" aria-label="分享" @click="handleShare">
              <Share2 :size="18" />
            </button>
          </div>
        </div>
      </article>

      <!-- ===== Comments Card ===== -->
      <section class="post-detail__card post-detail__comments">
        <!-- Comment Create -->
        <CommentCreate :post-id="post.id" :parent-id="0" @submitted="handleCommentSubmitted" />

        <hr class="post-card__divider" />

        <!-- Comment Thread List (infinite scroll) -->
        <div
          class="post-detail__comment-list"
          v-infinite-scroll="fetchCommentsList"
          :infinite-scroll-disabled="commentsLoading || !commentsHasMore"
          :infinite-scroll-immediate="false"
          infinite-scroll-distance="120"
        >
          <CommentThread
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            :post-id="post.id"
            @reply-added="handleCommentSubmitted"
          />

          <p v-if="comments.length === 0 && !commentsLoading" class="post-detail__no-comments">
            还没有评论，来发表第一条评论吧
          </p>

          <p v-if="commentsLoading" class="post-detail__load-status">加载中...</p>
          <p v-else-if="!commentsHasMore && comments.length > 0" class="post-detail__load-status">
            没有更多评论了
          </p>
        </div>
      </section>
    </template>
  </div>
</template>

<style lang="scss" scoped>
// ===== Design Tokens =====
$bg-card: #ffffff;
$bg-page: #f8f9fb;
$border-color: #e8eaed;
$border-subtle: #f0f0f0;
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent-red: #ef4444;
$accent-yellow: #eab308;
$accent-blue: #3b82f6;
$radius-card: 12px;
$radius-btn: 8px;
$transition-fast: 150ms ease;
$transition-smooth: 250ms cubic-bezier(0.4, 0, 0.2, 1);

// ===== Page Container =====
.post-detail {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 20px 80px;
  position: relative;
  animation: fade-in 0.35s ease both;
}

// ===== Fade-in Animation =====
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// ===== Back Button =====
.post-detail__back {
  position: absolute;
  left: -52px;
  top: 32px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid $border-color;
  background: $bg-card;
  color: $text-secondary;
  border-radius: $radius-btn;
  cursor: pointer;
  transition:
    color $transition-fast,
    background $transition-fast,
    border-color $transition-fast,
    box-shadow $transition-fast;

  &:hover {
    color: $text-primary;
    background: #f9fafb;
    border-color: #d1d5db;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }
}

// ===== Card Base =====
.post-detail__card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-card;
  padding: 28px 32px;
  margin-bottom: 20px;
  transition: box-shadow $transition-fast;

  &:hover {
    box-shadow:
      0 1px 3px rgba(0, 0, 0, 0.04),
      0 4px 16px rgba(0, 0, 0, 0.03);
  }
}

// ===== Skeleton =====
.post-detail__skeleton {
  padding: 40px 32px;
}

.skeleton-line {
  height: 14px;
  background: #f3f4f6;
  border-radius: 6px;
  margin-bottom: 12px;
  animation: skeleton-pulse 1.5s ease-in-out infinite;

  &--title {
    height: 22px;
    width: 60%;
    margin-bottom: 20px;
  }

  &--text {
    width: 100%;

    &--short {
      width: 40%;
    }
  }
}

@keyframes skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

// ===== Error State =====
.post-detail__error {
  text-align: center;
  padding: 48px 32px;
}

.post-detail__error-text {
  color: $text-secondary;
  font-size: 15px;
  margin: 0 0 16px;
}

.post-detail__retry-btn {
  padding: 8px 24px;
  border: 1px solid $border-color;
  background: $bg-card;
  color: $text-primary;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  transition:
    background $transition-fast,
    border-color $transition-fast;

  &:hover {
    background: #f9fafb;
    border-color: #d1d5db;
  }
}

// ===== Header: User =====
.post-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
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
  height: 42px;
  flex: 1;
}

.post-card__nickname {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
    text-underline-offset: 2px;
  }
}

.post-card__time {
  font-size: 12px;
  color: $text-muted;
  line-height: 1.3;
}

// ===== Follow Button =====
.post-card__follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background $transition-smooth,
    color $transition-smooth,
    box-shadow $transition-smooth,
    transform $transition-fast;
  background: $accent-red;
  color: #fff;

  &:hover {
    background: #dc2626;
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }

  &--followed {
    background: #e5e7eb;
    color: $text-secondary;

    &:hover {
      background: #d1d5db;
      color: $text-primary;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }
  }
}

// ===== Divider =====
.post-card__divider {
  border: none;
  border-top: 1px solid $border-subtle;
  margin: 18px 0 0;

  &:last-child {
    margin-bottom: 0;
  }
}

// ===== Body: Post Content =====
.post-card__body {
  margin-top: 18px;
}

// ---- View Count ----
.post-card__views {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: $text-muted;
  margin-bottom: 14px;
  line-height: 1;
}

// ---- Title ----
.post-card__title {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.45;
  margin: 0 0 10px;
  letter-spacing: -0.01em;
}

// ---- Edited Time ----
.post-card__edited {
  display: block;
  font-size: 12px;
  color: $text-muted;
  margin-bottom: 20px;
}

// ---- Content HTML ----
.post-card__content {
  font-size: 15px;
  color: $text-primary;
  line-height: 1.85;
  word-break: break-word;

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 8px;
    margin: 16px 0;
    display: block;
  }

  :deep(p) {
    margin: 0 0 14px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(blockquote) {
    margin: 14px 0;
    padding: 4px 16px;
    border-left: 3px solid #e5e7eb;
    color: $text-secondary;
  }

  :deep(pre) {
    background: #f9fafb;
    border: 1px solid $border-subtle;
    border-radius: 8px;
    padding: 16px;
    overflow-x: auto;
    margin: 14px 0;
    font-size: 13px;
    line-height: 1.6;
  }

  :deep(code) {
    font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
    font-size: 0.9em;

    &:not(pre > code) {
      background: #f3f4f6;
      padding: 2px 6px;
      border-radius: 4px;
      color: #1f2937;
    }
  }

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 20px 0 10px;
    line-height: 1.4;
    font-weight: 600;
  }

  :deep(h1) {
    font-size: 1.5em;
  }
  :deep(h2) {
    font-size: 1.3em;
  }
  :deep(h3) {
    font-size: 1.15em;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 24px;
    margin: 10px 0;

    li {
      margin-bottom: 4px;
    }
  }

  :deep(a) {
    color: $accent-blue;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid $border-subtle;
    margin: 24px 0;
  }
}

// ===== Actions Row (aligned with PostCard footer) =====
.post-card__actions-row {
  display: flex;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid $border-subtle;
}

.post-card__action {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition:
    color $transition-fast,
    background $transition-fast;

  &:hover {
    background: #f3f4f6;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: none;
  }

  &--liked {
    color: $accent-red;

    &:hover {
      background: #fef2f2;
    }
  }

  &--collected {
    color: $accent-yellow;

    &:hover {
      background: #fefce8;
    }
  }
}

// ===== Comments Section =====
.post-detail__comments {
  padding: 24px 32px;
}

.post-detail__comment-list {
  margin-top: 6px;
}

.post-detail__no-comments {
  text-align: center;
  color: $text-muted;
  font-size: 14px;
  padding: 32px 0 16px;
  margin: 0;
}

.post-detail__load-status {
  text-align: center;
  color: $text-muted;
  font-size: 13px;
  padding: 16px 0 4px;
  margin: 0;
}

// ===== Responsive: Hide back button on narrow screens =====
@media (max-width: 960px) {
  .post-detail {
    padding: 20px 16px 80px;
  }

  .post-detail__back {
    display: none;
  }

  .post-detail__card {
    padding: 20px;
    border-radius: 10px;
  }

  .post-card__title {
    font-size: 20px;
  }
}
</style>
