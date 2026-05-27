<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElAvatar } from 'element-plus'
import { formatRelativeTime } from '@/utils/time'
import type { FollowItemVO } from '@/api/modules/index'

const props = defineProps<{
  user: FollowItemVO
  label: string
}>()

const router = useRouter()

const displayName = computed(() => props.user.nickname || props.user.username)
const followedTime = computed(() => `${props.label}${formatRelativeTime(props.user.followedAt)}`)

function handleClick() {
  router.push(`/user/${props.user.id}`)
}
</script>

<template>
  <div class="user-card" tabindex="0" role="link" @click="handleClick">
    <ElAvatar :src="user.avatar ?? undefined" :size="40" class="user-card__avatar">
      {{ displayName.charAt(0) }}
    </ElAvatar>
    <div class="user-card__info">
      <span class="user-card__name">{{ displayName }}</span>
      <time class="user-card__time">{{ followedTime }}</time>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$text-primary: #1a1a2e;
$text-muted: #9ca3af;
$border-subtle: #f0f0f0;
$transition-fast: 150ms ease;

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  outline: none;
  transition:
    background $transition-fast,
    border-color $transition-fast;

  &:hover,
  &:focus-visible {
    background: #f8f9fa;
    border-color: $border-subtle;
  }
}

.user-card__avatar {
  flex-shrink: 0;
}

.user-card__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  min-width: 0;
  height: 40px;
}

.user-card__name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-card__time {
  font-size: 12px;
  color: $text-muted;
  line-height: 1.3;
}
</style>
