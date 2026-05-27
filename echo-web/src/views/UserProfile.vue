<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAvatar, ElMessage, ElMessageBox } from 'element-plus'
import { UserPlus, UserCheck, ArrowLeft, Loader2 } from '@lucide/vue'
import PostCard from '@/components/PostCard.vue'
import UserCard from '@/components/UserCard.vue'
import { useInfiniteList } from '@/composables/useInfiniteList'
import { useUserStore } from '@/stores/userStore'
import { getUserProfile, followUser, getFollowers, getFollowing } from '@/api/users'
import { fetchUserPosts as apiFetchUserPosts, deletePost, likePost, favoritePost } from '@/api/posts'
import { formatRelativeTime } from '@/utils/time'
import type { PublicUserVO, PostVO, FollowItemVO } from '@/api/modules/index'

// ===== Router =====
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ===== User profile data =====
const profile = ref<PublicUserVO | null>(null)
const profileLoading = ref(true)
const profileError = ref<string | null>(null)

// ===== Computed =====
const userId = computed(() => Number(route.params.id))
const isOwnProfile = computed(() => userId.value === userStore.userInfo?.id)
const displayName = computed(() => {
  if (!profile.value) return ''
  return profile.value.nickname || profile.value.username
})
const joinedAt = computed(() => {
  if (!profile.value) return ''
  return formatRelativeTime(profile.value.createdAt)
})

// ===== Follow state =====
const followed = ref(false)
const followSubmitting = ref(false)
const followerCount = ref(0)
const followingCount = ref(0)

// ===== Posts infinite list =====
const {
  posts,
  loading: postsLoading,
  hasMore: postsHasMore,
  likedPosts,
  collectedPosts,
  fetchPosts: fetchUserPosts,
  toggleLike: localToggleLike,
  toggleCollect: localToggleCollect,
  reset: resetPosts,
} = useInfiniteList<PostVO>({
  fetchFn: (params: Record<string, unknown>) => {
    return apiFetchUserPosts(userId.value, {
      cursor: (params.cursor as string | null) ?? null,
      size: (params.size as number) ?? 10,
    }) as unknown as Promise<{ data: Record<string, unknown> }>
  },
  mode: 'cursor',
  pageSize: 10,
})

// ===== Follow/Follower Dialog =====
const dialogVisible = ref(false)
const dialogTab = ref<'following' | 'followers'>('following')
const dialogUsers = ref<FollowItemVO[]>([])
const dialogLoading = ref(false)
const dialogTotal = ref(0)
const dialogPage = ref(1)
const DIALOG_PAGE_SIZE = 12

// ===== Load user profile =====
async function loadProfile() {
  profileLoading.value = true
  profileError.value = null
  try {
    const res = await getUserProfile(userId.value)
    const data = res.data!
    profile.value = data
    followed.value = data.isFollowed
    followerCount.value = data.followerCount
    followingCount.value = data.followingCount
  } catch {
    profileError.value = '用户信息加载失败'
    ElMessage.error('用户信息加载失败')
  } finally {
    profileLoading.value = false
  }
}

// ===== Follow/Unfollow =====
async function handleToggleFollow() {
  if (followSubmitting.value || !profile.value) return
  followSubmitting.value = true

  const prevFollowed = followed.value
  followed.value = !followed.value
  followerCount.value += followed.value ? 1 : -1

  try {
    const res = await followUser(profile.value.id)
    const result = res.data!
    followed.value = result.followed
    ElMessage.success(followed.value ? '已关注' : '已取消关注')
  } catch {
    followed.value = prevFollowed
    followerCount.value += prevFollowed ? 1 : -1
    ElMessage.error('操作失败')
  } finally {
    followSubmitting.value = false
  }
}

// ===== Post interaction =====
async function handleToggleLike(postId: number) {
  localToggleLike(postId)
  try {
    await likePost(postId)
  } catch {
    localToggleLike(postId)
    ElMessage.error('操作失败')
  }
}

async function handleToggleCollect(postId: number) {
  localToggleCollect(postId)
  try {
    await favoritePost(postId)
  } catch {
    localToggleCollect(postId)
    ElMessage.error('操作失败')
  }
}

// ===== Post edit/delete =====
function handleEditPost(postId: number) {
  router.push(`/post/create?editId=${postId}`)
}

async function handleDeletePost(postId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这篇帖子吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }

  try {
    await deletePost(postId)
    ElMessage.success('删除成功')
    resetPosts()
    fetchUserPosts()
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}

// ===== Back =====
function handleBack() {
  router.push('/')
}

// ===== Dialog operations =====
function openDialog(tab: 'following' | 'followers') {
  dialogTab.value = tab
  dialogPage.value = 1
  dialogUsers.value = []
  dialogVisible.value = true
  loadDialogUsers()
}

async function loadDialogUsers() {
  dialogLoading.value = true
  try {
    const fetcher = dialogTab.value === 'following' ? getFollowing : getFollowers
    const res = await fetcher(userId.value, {
      current: dialogPage.value,
      size: DIALOG_PAGE_SIZE,
    })
    const result = res.data!
    dialogUsers.value = result.records
    dialogTotal.value = result.total
  } catch {
    ElMessage.error('加载失败')
  } finally {
    dialogLoading.value = false
  }
}

function handleDialogTabChange(tab: 'following' | 'followers') {
  dialogTab.value = tab
  dialogPage.value = 1
  dialogUsers.value = []
  loadDialogUsers()
}

function handleDialogPageChange(page: number) {
  dialogPage.value = page
  loadDialogUsers()
}

const dialogLabel = computed(() =>
  dialogTab.value === 'following' ? '关注于' : '关注你于',
)

// ===== Lifecycle =====
onMounted(() => {
  loadProfile()
  fetchUserPosts()
})

watch(userId, () => {
  loadProfile()
  resetPosts()
  fetchUserPosts()
})
</script>

<template>
  <div class="user-profile">
    <!-- ===== Back Button ===== -->
    <button class="user-profile__back" aria-label="返回首页" @click="handleBack">
      <ArrowLeft :size="20" />
    </button>

    <!-- ===== Loading State ===== -->
    <template v-if="profileLoading">
      <div class="user-profile__card user-profile__skeleton">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-line skeleton-line--name"></div>
        <div class="skeleton-line skeleton-line--text"></div>
      </div>
    </template>

    <!-- ===== Error State ===== -->
    <template v-else-if="profileError">
      <div class="user-profile__card user-profile__error">
        <p class="user-profile__error-text">{{ profileError }}</p>
        <button class="user-profile__retry-btn" @click="loadProfile">重试</button>
      </div>
    </template>

    <!-- ===== Main Content ===== -->
    <template v-else-if="profile">
      <article class="user-profile__card">
        <!-- User Info Section -->
        <div class="user-profile__info">
          <div class="user-profile__header">
            <ElAvatar :src="profile.avatar ?? undefined" :size="64" class="user-profile__avatar">
              {{ displayName.charAt(0) }}
            </ElAvatar>
            <div class="user-profile__header-text">
              <h1 class="user-profile__name">{{ displayName }}</h1>
              <time class="user-profile__joined">加入于 {{ joinedAt }}</time>
            </div>

            <button
              v-if="!isOwnProfile"
              :class="['user-profile__follow-btn', { 'user-profile__follow-btn--followed': followed }]"
              :disabled="followSubmitting"
              @click="handleToggleFollow"
            >
              <component :is="followed ? UserCheck : UserPlus" :size="16" />
              <span>{{ followed ? '已关注' : '关注' }}</span>
            </button>
          </div>

          <p v-if="profile.bio" class="user-profile__bio">{{ profile.bio }}</p>

          <div class="user-profile__stats">
            <button class="user-profile__stat user-profile__stat--clickable" @click="openDialog('following')">
              <span class="user-profile__stat-value">{{ followingCount }}</span>
              <span class="user-profile__stat-label">关注</span>
            </button>
            <div class="user-profile__stat-divider"></div>
            <button class="user-profile__stat user-profile__stat--clickable" @click="openDialog('followers')">
              <span class="user-profile__stat-value">{{ followerCount }}</span>
              <span class="user-profile__stat-label">粉丝</span>
            </button>
            <div class="user-profile__stat-divider"></div>
            <div class="user-profile__stat">
              <span class="user-profile__stat-value">{{ profile.postCount }}</span>
              <span class="user-profile__stat-label">帖子</span>
            </div>
          </div>
        </div>

        <hr class="user-profile__divider" />

        <!-- Post List Section -->
        <div
          class="user-profile__posts"
          v-infinite-scroll="fetchUserPosts"
          :infinite-scroll-disabled="postsLoading || !postsHasMore"
          :infinite-scroll-immediate="false"
          infinite-scroll-distance="120"
        >
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            :is-liked="likedPosts.has(post.id)"
            :is-collected="collectedPosts.has(post.id)"
            :show-actions="isOwnProfile"
            @toggle-like="handleToggleLike"
            @toggle-collect="handleToggleCollect"
            @edit="handleEditPost"
            @delete="handleDeletePost"
          />

          <p v-if="posts.length === 0 && !postsLoading" class="user-profile__empty">还没有发布帖子</p>
          <p v-if="postsLoading" class="user-profile__load-status">加载中...</p>
          <p v-else-if="!postsHasMore && posts.length > 0" class="user-profile__load-status">没有更多了</p>
        </div>
      </article>
    </template>

    <!-- ===== Follow / Follower Dialog ===== -->
    <Teleport to="body">
      <Transition name="dialog-fade">
        <div v-if="dialogVisible" class="follow-dialog-overlay" @click.self="dialogVisible = false">
          <div class="follow-dialog">
            <div class="follow-dialog__tabs">
              <button
                :class="['follow-dialog__tab', { 'follow-dialog__tab--active': dialogTab === 'following' }]"
                @click="handleDialogTabChange('following')"
              >
                关注
              </button>
              <button
                :class="['follow-dialog__tab', { 'follow-dialog__tab--active': dialogTab === 'followers' }]"
                @click="handleDialogTabChange('followers')"
              >
                粉丝
              </button>
              <button class="follow-dialog__close" aria-label="关闭" @click="dialogVisible = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div class="follow-dialog__list">
              <template v-if="dialogLoading">
                <div class="follow-dialog__loading">
                  <Loader2 :size="24" class="follow-dialog__spinner" />
                </div>
              </template>

              <template v-else-if="dialogUsers.length > 0">
                <UserCard
                  v-for="user in dialogUsers"
                  :key="user.id"
                  :user="user"
                  :label="dialogLabel"
                />

                <div v-if="dialogTotal > DIALOG_PAGE_SIZE" class="follow-dialog__pagination">
                  <button
                    class="follow-dialog__page-btn"
                    :disabled="dialogPage <= 1"
                    @click="handleDialogPageChange(dialogPage - 1)"
                  >
                    上一页
                  </button>
                  <span class="follow-dialog__page-info">
                    {{ dialogPage }} / {{ Math.ceil(dialogTotal / DIALOG_PAGE_SIZE) }}
                  </span>
                  <button
                    class="follow-dialog__page-btn"
                    :disabled="dialogPage >= Math.ceil(dialogTotal / DIALOG_PAGE_SIZE)"
                    @click="handleDialogPageChange(dialogPage + 1)"
                  >
                    下一页
                  </button>
                </div>
              </template>

              <p v-else class="follow-dialog__empty">
                {{ dialogTab === 'following' ? '还没有关注任何人' : '还没有粉丝' }}
              </p>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
// ===== Design Tokens =====
$bg-card: #ffffff;
$border-color: #e8eaed;
$border-subtle: #f0f0f0;
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent-red: #ef4444;
$radius-card: 12px;
$radius-btn: 8px;
$transition-fast: 150ms ease;
$transition-smooth: 250ms cubic-bezier(0.4, 0, 0.2, 1);

// ===== Page Container =====
.user-profile {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 20px 80px;
  position: relative;
  animation: fade-in 0.35s ease both;
}

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
.user-profile__back {
  position: absolute;
  left: -52px;
  top: 26px;
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
.user-profile__card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius-card;
  padding: 28px 32px;
  transition: box-shadow $transition-fast;

  &:hover {
    box-shadow:
      0 1px 3px rgba(0, 0, 0, 0.04),
      0 4px 16px rgba(0, 0, 0, 0.03);
  }
}

// ===== Skeleton =====
.user-profile__skeleton {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 32px;
  gap: 14px;
}

.skeleton-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f3f4f6;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

.skeleton-line {
  height: 14px;
  background: #f3f4f6;
  border-radius: 6px;
  animation: skeleton-pulse 1.5s ease-in-out infinite;

  &--name {
    height: 20px;
    width: 140px;
  }

  &--text {
    width: 200px;
  }
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

// ===== Error State =====
.user-profile__error {
  text-align: center;
  padding: 48px 32px;
}

.user-profile__error-text {
  color: $text-secondary;
  font-size: 15px;
  margin: 0 0 16px;
}

.user-profile__retry-btn {
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

// ===== User Info Section =====
.user-profile__header {
  display: flex;
  align-items: center;
  gap: 20px;
  text-align: left;
}

.user-profile__avatar {
  flex-shrink: 0;
}

.user-profile__header-text {
  flex: 1;
  min-width: 0;
}

.user-profile__name {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.35;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-profile__joined {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: $text-muted;
}

// ===== Follow Button =====
.user-profile__follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
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

// ===== Bio =====
.user-profile__bio {
  margin: 16px 0 0;
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.7;
}

// ===== Stats =====
.user-profile__stats {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 18px;
  padding: 14px 0;
  border-top: 1px solid $border-subtle;
  border-bottom: 1px solid $border-subtle;
}

.user-profile__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 28px;
  border: none;
  background: transparent;

  &--clickable {
    cursor: pointer;
    border-radius: 8px;
    transition: background $transition-fast;

    &:hover {
      background: #f8f9fa;
    }
  }
}

.user-profile__stat-value {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;
}

.user-profile__stat-label {
  font-size: 12px;
  color: $text-muted;
  line-height: 1.2;
}

.user-profile__stat-divider {
  width: 1px;
  height: 28px;
  background: $border-subtle;
}

// ===== Divider =====
.user-profile__divider {
  border: none;
  border-top: 1px solid $border-subtle;
  margin: 18px 0 0;
}

// ===== Post List =====
.user-profile__posts {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-profile__empty {
  text-align: center;
  color: $text-muted;
  font-size: 14px;
  padding: 24px 0;
  margin: 0;
}

.user-profile__load-status {
  text-align: center;
  color: $text-muted;
  font-size: 13px;
  padding: 16px 0 4px;
  margin: 0;
}

// ===== Follow Dialog =====
.follow-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 8000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.follow-dialog {
  background: $bg-card;
  border-radius: 16px;
  width: 420px;
  max-width: 92vw;
  max-height: 72vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(0, 0, 0, 0.04);
}

.follow-dialog__tabs {
  display: flex;
  align-items: center;
  border-bottom: 1px solid $border-subtle;
  padding: 0 16px;
  position: relative;
}

.follow-dialog__tab {
  flex: 1;
  padding: 14px 0;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition:
    color $transition-fast,
    border-color $transition-fast;

  &:hover {
    color: $text-primary;
  }

  &--active {
    color: $text-primary;
    font-weight: 600;
    border-bottom-color: $text-primary;
  }
}

.follow-dialog__close {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: $text-muted;
  border-radius: 6px;
  cursor: pointer;
  transition:
    color $transition-fast,
    background $transition-fast;

  &:hover {
    color: $text-primary;
    background: #f3f4f6;
  }
}

.follow-dialog__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px 16px;
  max-height: 360px;
}

.follow-dialog__loading {
  display: flex;
  justify-content: center;
  padding: 32px 0;
  color: $text-muted;
}

.follow-dialog__spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.follow-dialog__empty {
  text-align: center;
  color: $text-muted;
  font-size: 14px;
  padding: 32px 0;
  margin: 0;
}

.follow-dialog__pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px 0 4px;
}

.follow-dialog__page-btn {
  padding: 5px 14px;
  border: 1px solid $border-color;
  background: $bg-card;
  color: $text-secondary;
  font-size: 13px;
  border-radius: 6px;
  cursor: pointer;
  transition:
    color $transition-fast,
    background $transition-fast,
    border-color $transition-fast;

  &:hover:not(:disabled) {
    color: $text-primary;
    background: #f9fafb;
    border-color: #d1d5db;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.follow-dialog__page-info {
  font-size: 13px;
  color: $text-muted;
  min-width: 48px;
  text-align: center;
}

// ===== Dialog Transition =====
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;

  .follow-dialog {
    transition: transform 0.2s ease;
  }
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;

  .follow-dialog {
    transform: scale(0.96);
  }
}

// ===== Responsive =====
@media (max-width: 960px) {
  .user-profile {
    padding: 20px 16px 80px;
  }

  .user-profile__back {
    display: none;
  }

  .user-profile__card {
    padding: 20px;
    border-radius: 10px;
  }

  .user-profile__header {
    gap: 14px;
  }

  .user-profile__name {
    font-size: 19px;
  }

  .user-profile__follow-btn {
    padding: 6px 16px;
    font-size: 13px;
  }
}
</style>
