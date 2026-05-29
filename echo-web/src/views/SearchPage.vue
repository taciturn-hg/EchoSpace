<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@lucide/vue'
import PostCard from '@/components/PostCard.vue'
import { useInfiniteList } from '@/composables/useInfiniteList'
import { searchPosts } from '@/api/posts'
import type { PostVO } from '@/api/modules/index'

const route = useRoute()
const router = useRouter()

const keyword = ref((route.query.q as string) || '')
const sort = ref('created_at')
const initialLoading = ref(true)

const {
  posts,
  loading,
  hasMore,
  error,
  fetchPosts,
  setBaseParam,
} = useInfiniteList<PostVO>({
  fetchFn: (params: Record<string, unknown>) => {
    return searchPosts({
      q: (params.q as string) || '',
      current: params.current as number,
      size: params.size as number,
      sort: (params.sort as string) || 'created_at',
    }) as unknown as Promise<{ data: Record<string, unknown> }>
  },
  baseParams: { q: keyword.value, sort: sort.value },
  mode: 'page',
  pageSize: 10,
})

const totalCount = computed(() => posts.value.length)
const isEmpty = computed(() => !loading.value && !initialLoading.value && posts.value.length === 0 && !error.value)

function handleSortChange(value: string) {
  sort.value = value
  initialLoading.value = true
  setBaseParam('sort', value)
  fetchPosts().finally(() => {
    initialLoading.value = false
  })
}

function handleBack() {
  router.push('/')
}

function handleKeywordClick() {
  router.push({ name: 'search', query: { q: keyword.value || '', _ts: Date.now().toString() } })
}

watch(
  () => route.query,
  (newQuery) => {
    const q = (newQuery.q as string) || ''
    keyword.value = q
    initialLoading.value = true
    setBaseParam('q', q)
    fetchPosts().finally(() => {
      initialLoading.value = false
    })
  },
)

onMounted(() => {
  fetchPosts().finally(() => {
    initialLoading.value = false
  })
})
</script>

<template>
  <div class="search-page">
    <button class="search-page__back" aria-label="返回首页" @click="handleBack">
      <ArrowLeft :size="20" />
    </button>

    <div class="search-page__header">
      <div class="search-page__header-top">
        <h2 v-if="keyword" class="search-page__query">
          搜索「<span class="search-page__keyword" @click="handleKeywordClick">{{ keyword }}</span>」
        </h2>
        <h2 v-else class="search-page__query">搜索全部帖子</h2>
      </div>

      <div v-if="!initialLoading" class="search-page__meta">
        <span class="search-page__stat">
          <strong>{{ totalCount }}</strong> 个结果
        </span>

        <select v-model="sort" class="search-page__sort" @change="handleSortChange(sort)">
          <option value="created_at">最新发布</option>
          <option value="hot">最热</option>
        </select>
      </div>
    </div>

    <div class="search-page__divider" />

    <div
      v-if="!isEmpty"
      class="search-page__list"
      v-infinite-scroll="fetchPosts"
      :infinite-scroll-disabled="loading || !hasMore"
      :infinite-scroll-immediate="false"
      infinite-scroll-distance="120"
    >
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
      />

      <p v-if="loading" class="search-page__loading">加载中...</p>
      <p v-if="!hasMore && posts.length > 0" class="search-page__end">没有更多了</p>

      <p v-if="error" class="search-page__error">{{ error }}</p>
    </div>

    <div v-if="!initialLoading && isEmpty" class="search-page__empty">
      <div class="search-page__empty-icon">
        <svg
          viewBox="0 0 24 24"
          width="48"
          height="48"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
        >
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
      </div>
      <p class="search-page__empty-title">未找到相关结果</p>
      <p class="search-page__empty-hint">换个关键词试试吧</p>
    </div>

    <div v-if="initialLoading" class="search-page__skeleton">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="skeleton-line skeleton-line--title"></div>
        <div class="skeleton-line skeleton-line--text"></div>
        <div class="skeleton-line skeleton-line--text skeleton-line--text--short"></div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent: #2563eb;
$bg-card: #ffffff;
$border-color: #e5e7eb;
$radius: 12px;
$radius-btn: 10px;

.search-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 28px 20px 80px;
  position: relative;
  animation: fadeSlideIn 0.35s ease both;
}

@keyframes fadeSlideIn {
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
.search-page__back {
  position: absolute;
  left: -52px;
  top: 28px;
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
    color 200ms ease,
    background 250ms ease,
    border-color 200ms ease,
    box-shadow 200ms ease,
    transform 200ms ease;

  &:hover {
    color: $text-primary;
    border-color: #c7d2fe;
    background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 50%, #ecfeff 100%);
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
    transform: translateX(-2px);
  }

  &:active {
    transform: scale(0.95) translateX(-2px);
  }
}

// ===== Header =====
.search-page__header {
  margin-bottom: 4px;
}

.search-page__header-top {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.search-page__query {
  font-size: 22px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
  line-height: 1.4;
}

.search-page__keyword {
  color: $accent;
  cursor: pointer;
  border-bottom: 2px dotted rgba(37, 99, 235, 0.3);
  transition: border-color 200ms ease;

  &:hover {
    border-bottom-color: $accent;
  }
}

.search-page__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.search-page__stat {
  font-size: 13px;
  color: $text-muted;

  strong {
    color: $text-primary;
    font-weight: 600;
  }
}

.search-page__sort {
  padding: 5px 30px 5px 10px;
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  background: #ffffff;
  color: $text-secondary;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;

  &:hover {
    border-color: #d1d5db;
  }

  &:focus {
    border-color: $accent;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  }
}

.search-page__divider {
  border: none;
  border-top: 1px solid #f3f4f6;
  margin: 16px 0;
}

// ===== Post List =====
.search-page__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-page__loading,
.search-page__end,
.search-page__error {
  text-align: center;
  font-size: 14px;
  padding: 20px 0;
}

.search-page__loading {
  color: $text-muted;
}

.search-page__end {
  color: #d1d5db;
}

.search-page__error {
  color: #ef4444;
}

// ===== Empty State =====
.search-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.search-page__empty-icon {
  color: #d1d5db;
  margin-bottom: 20px;
}

.search-page__empty-title {
  font-size: 17px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 8px;
}

.search-page__empty-hint {
  font-size: 14px;
  color: $text-muted;
  margin: 0;
}

// ===== Skeleton =====
.search-page__skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  background: $bg-card;
  border: 1px solid $border-color;
  border-radius: $radius;
  padding: 24px;
}

.skeleton-line {
  height: 14px;
  background: #f3f4f6;
  border-radius: 6px;
  animation: shimmer 1.6s ease-in-out infinite;
  background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
  background-size: 200% 100%;

  &--title {
    width: 45%;
    height: 18px;
    margin-bottom: 14px;
  }

  &--text {
    width: 90%;
    margin-bottom: 10px;

    &--short {
      width: 60%;
    }
  }
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

// ===== Responsive =====
@media (max-width: 960px) {
  .search-page {
    padding: 20px 16px 80px;
  }

  .search-page__back {
    display: none;
  }
}
</style>
