<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PostCard from '@/components/PostCard.vue'
import { useInfiniteList } from '@/composables/useInfiniteList'

const sort = ref('created_at')

const {
  posts,
  loading,
  hasMore,
  likedPosts,
  collectedPosts,
  fetchPosts,
  toggleLike,
  toggleCollect,
  setBaseParam,
} = useInfiniteList({ baseParams: { sort: sort.value } })

function handleSortChange(value: string) {
  sort.value = value
  setBaseParam('sort', value)
  fetchPosts()
}

onMounted(() => {
  fetchPosts()
})
</script>

<template>
  <div class="home-page">
    <!-- Sort Bar -->
    <div class="home-page__toolbar">
      <div class="home-page__spacer" />
      <select v-model="sort" class="home-page__sort" @change="handleSortChange(sort)">
        <option value="created_at">最新发布</option>
        <option value="hot">最热</option>
      </select>
    </div>

    <!-- Post List -->
    <div
      class="home-page__list"
      v-infinite-scroll="fetchPosts"
      :infinite-scroll-disabled="loading || !hasMore"
      :infinite-scroll-immediate="false"
      infinite-scroll-distance="120"
    >
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        :is-liked="likedPosts.has(post.id)"
        :is-collected="collectedPosts.has(post.id)"
        @toggle-like="toggleLike"
        @toggle-collect="toggleCollect"
      />

      <p v-if="loading" class="home-page__loading">加载中...</p>
      <p v-if="!hasMore && posts.length > 0" class="home-page__end">没有更多了</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.home-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 24px 20px 60px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// ===== Toolbar =====
.home-page__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.home-page__sort {
  padding: 6px 32px 6px 12px;
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  background: #ffffff;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;

  &:hover {
    border-color: #d1d5db;
  }

  &:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  }
}

// ===== Post List =====
.home-page__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.home-page__loading,
.home-page__end {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 16px 0;
}
</style>
