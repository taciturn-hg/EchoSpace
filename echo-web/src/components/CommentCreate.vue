<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElAvatar, ElInput, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import { createComment } from '@/api/comments'
import type { CommentUser } from '@/api/modules/index'

const props = withDefaults(
  defineProps<{
    postId: number
    parentId: number
    replyToUser?: CommentUser | null
  }>(),
  {
    replyToUser: null,
  },
)

const emit = defineEmits<{
  submitted: []
  cancel: []
}>()

const userStore = useUserStore()

function getInitialContent(): string {
  if (props.replyToUser) {
    const name = props.replyToUser.nickname || props.replyToUser.username
    return `回复 @${name}：`
  }
  return ''
}

const content = ref(getInitialContent())
const submitting = ref(false)

const currentUser = computed(() => userStore.userInfo)
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '')

const placeholder = computed(() => {
  if (props.replyToUser) return ''
  return '写下你的评论...'
})

const canSubmit = computed(() => {
  if (submitting.value) return false
  const text = content.value.trim()
  if (text.length === 0) return false
  if (props.replyToUser && text === getInitialContent()) return false
  return true
})

async function handleSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    await createComment(props.postId, {
      parentId: props.parentId,
      replyToUid: props.replyToUser?.id,
      content: content.value.trim(),
    })
    ElMessage.success('评论发布成功')
    content.value = getInitialContent()
    emit('submitted')
  } catch {
    ElMessage.error('评论发布失败，请重试')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  content.value = getInitialContent()
  emit('cancel')
}
</script>

<template>
  <div class="comment-create">
    <!-- ===== 用户区 ===== -->
    <header class="comment-create__header">
      <ElAvatar :src="currentUser?.avatar ?? undefined" :size="36" class="comment-create__avatar">
        {{ displayName.charAt(0) }}
      </ElAvatar>
      <span class="comment-create__username">{{ displayName }}</span>
    </header>

    <!-- ===== 编辑区 ===== -->
    <div class="comment-create__body">
      <ElInput
        v-model="content"
        type="textarea"
        :rows="3"
        :maxlength="5000"
        :placeholder="placeholder"
        resize="vertical"
        class="comment-create__textarea"
      />
      <div class="comment-create__footer">
        <button class="comment-create__cancel" @click="handleCancel">取消</button>
        <button
          :class="['comment-create__submit', { 'comment-create__submit--active': canSubmit }]"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          {{ submitting ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$text-muted: #9ca3af;
$accent-blue: #3b82f6;
$border-color: #e8eaed;
$transition-fast: 150ms ease;

.comment-create {
  background: transparent;
  padding: 14px 0;
}

// ===== Header: Current User =====
.comment-create__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-create__avatar {
  flex-shrink: 0;
}

.comment-create__username {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

// ===== Body =====
.comment-create__body {
  margin-top: 10px;
  padding-left: 46px;
}

.comment-create__textarea {
  :deep(.el-textarea__inner) {
    min-height: 88px;
    font-size: 14px;
    color: $text-primary;
    line-height: 1.7;
    background: #f9fafb;
    border: 1px solid $border-color;
    border-radius: 8px;
    box-shadow: none;
    transition: border-color $transition-fast;
    font-family: inherit;
    resize: vertical;

    &::placeholder {
      color: $text-muted;
    }

    &:focus {
      border-color: $accent-blue;
      background: #fff;
      box-shadow: none;
    }
  }
}

.comment-create__footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.comment-create__cancel {
  padding: 6px 16px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 13px;
  cursor: pointer;
  border-radius: 6px;
  transition:
    color $transition-fast,
    background $transition-fast;

  &:hover {
    color: $text-primary;
    background: #f3f4f6;
  }
}

.comment-create__submit {
  padding: 6px 18px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition:
    background $transition-fast,
    opacity $transition-fast;
  background: #e5e7eb;
  color: #9ca3af;

  &--active {
    background: $accent-blue;
    color: #fff;

    &:hover {
      background: #2563eb;
    }
  }
}
</style>
