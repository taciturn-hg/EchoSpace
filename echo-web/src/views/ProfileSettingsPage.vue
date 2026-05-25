<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Upload, EditPen, Plus } from '@element-plus/icons-vue'
import { getProfile, updateProfile } from '@/api/users'
import { useUserStore } from '@/stores/userStore'
import type { UpdateProfileDTO, UserProfileVO } from '@/api/modules'

const userStore = useUserStore()
const fileInput = ref<HTMLInputElement>()
const profileFormRef = ref<FormInstance>()

const rules: FormRules = {
  nickname: [{ max: 50, message: '昵称不能超过 50 个字符', trigger: 'blur' }],
  bio: [{ max: 500, message: '简介不能超过 500 个字符', trigger: 'blur' }],
}

const form = reactive<UpdateProfileDTO>({
  avatar: undefined,
  nickname: '',
  bio: '',
})

const original = reactive<UpdateProfileDTO>({
  avatar: undefined,
  nickname: '',
  bio: '',
})

const uploading = ref(false)
const saving = ref(false)
const loading = ref(true)

const userInfo = computed(() => userStore.userInfo)

const avatarSrc = computed(() => form.avatar || userInfo.value?.avatar || '')

const avatarFallback = computed(() => {
  const name = form.nickname || userInfo.value?.nickname || userInfo.value?.username || 'E'
  return name.charAt(0).toUpperCase()
})

const isDirty = computed(
  () =>
    form.avatar !== original.avatar ||
    form.nickname !== original.nickname ||
    form.bio !== original.bio,
)

function applyProfile(p: UserProfileVO) {
  form.avatar = p.avatar ?? undefined
  form.nickname = p.nickname ?? ''
  form.bio = p.bio ?? ''
  original.avatar = p.avatar ?? undefined
  original.nickname = p.nickname ?? ''
  original.bio = p.bio ?? ''
}

async function fetchProfile() {
  try {
    const res = await getProfile()
    if (res.code === 1 && res.data) {
      applyProfile(res.data)
      return true
    }
    return false
  } catch {
    // 拦截器统一处理
    return false
  } finally {
    loading.value = false
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  try {
    const file = input.files?.[0]
    if (!file) return

    const allowed = ['image/jpeg', 'image/png']
    if (!allowed.includes(file.type)) {
      ElMessage.error('仅支持 JPG、PNG 格式')
      return
    }
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.error('头像大小不能超过 2MB')
      return
    }

    uploading.value = true
    try {
      // TODO：后续补上头像上传接口请求逻辑
      // const res = await uploadAvatar(file)
      // if (res.code === 1 && res.data) {
      //   form.avatar = res.data.url
      // }
    } catch {
      // 拦截器统一处理
    } finally {
      uploading.value = false
    }
  } catch {
    // 拦截器统一处理
  } finally {
    input.value = ''
  }
}

async function handleSave() {
  try {
    await profileFormRef.value!.validate()
  } catch {
    return
  }

  const dto: UpdateProfileDTO = {}
  if (form.avatar !== original.avatar) dto.avatar = form.avatar
  if (form.nickname !== original.nickname) dto.nickname = form.nickname
  if (form.bio !== original.bio) dto.bio = form.bio

  if (Object.keys(dto).length === 0) {
    ElMessage.info('没有修改的内容')
    return
  }

  saving.value = true
  try {
    const res = await updateProfile(dto)
    if (res.code === 1) {
      ElMessage.success('资料已更新')
      const info = userInfo.value
      if (info) {
        userStore.setUserInfo({
          ...info,
          avatar: dto.avatar ?? info.avatar,
          nickname: dto.nickname ?? info.nickname,
          bio: dto.bio ?? info.bio,
        })
      }
      original.avatar = form.avatar
      original.nickname = form.nickname
      original.bio = form.bio
    }
  } catch {
    // 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleCancel() {
  loading.value = true
  const ok = await fetchProfile()
  profileFormRef.value?.clearValidate()
  if (ok) {
    ElMessage.info('已还原为服务器数据')
  } else {
    ElMessage.warning('还原失败，请稍后重试')
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<template>
  <div class="profile-settings">
    <div class="settings-card">
      <!-- 头像区 -->
      <div class="avatar-section">
        <div class="avatar-circle" :class="{ 'has-image': !!avatarSrc }">
          <img v-if="avatarSrc" :src="avatarSrc" alt="用户头像" />
          <span v-else class="avatar-letter">{{ avatarFallback }}</span>
          <div v-if="uploading" class="avatar-overlay">
            <el-icon class="spin"><Upload /></el-icon>
          </div>
        </div>
        <button class="upload-btn" type="button" :disabled="uploading" @click="triggerUpload">
          <el-icon><Plus /></el-icon>
          <span>{{ uploading ? '上传中…' : '更换头像' }}</span>
        </button>
        <input
          ref="fileInput"
          type="file"
          accept="image/jpeg,image/png"
          hidden
          @change="handleFileChange"
        />
      </div>

      <!-- 表单区 -->
      <el-form
        ref="profileFormRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="profile-form"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="form.nickname"
            maxlength="50"
            show-word-limit
            placeholder="输入昵称"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="form.bio"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="写一句简介介绍一下自己吧…"
          />
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <button class="btn btn-cancel" type="button" :disabled="loading" @click="handleCancel">
          <el-icon><EditPen /></el-icon>
          <span>取消</span>
        </button>
        <button
          class="btn btn-save"
          type="button"
          :disabled="!isDirty || saving"
          @click="handleSave"
        >
          <span>{{ saving ? '保存中…' : '保存' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// ---------- Design tokens ----------
$card-width: 432px;
$text-primary: #111827;
$text-secondary: #4b5563;
$text-muted: #9ca3af;
$border: #e5e7eb;
$border-focus: #6366f1;
$green: #10b981;
$green-hover: #059669;
$radius-sm: 8px;
$radius-md: 10px;
$radius-full: 999px;

// ---------- Page ----------
.profile-settings {
  display: flex;
  justify-content: center;
  padding: 40px 24px 80px;
  animation: fade-in 420ms ease both;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// ---------- Card ----------
.settings-card {
  width: 100%;
  max-width: $card-width;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

// ---------- Avatar section ----------
.avatar-section {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding-bottom: 4px;
}

.avatar-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eef2ff, #f0fdf4);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid $border;
  position: relative;
  overflow: hidden;
  transition:
    border-color 240ms ease,
    box-shadow 240ms ease;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &.has-image {
    border-color: transparent;
    box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.08);
  }
}

.avatar-letter {
  font-size: 32px;
  font-weight: 600;
  color: #6366f1;
  user-select: none;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(17, 24, 39, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  backdrop-filter: blur(2px);
}

.spin {
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  margin-bottom: 14px;
  border-radius: $radius-full;
  border: 1px solid $border;
  background: #fff;
  color: $text-secondary;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background 240ms ease,
    color 240ms ease,
    border-color 240ms ease,
    box-shadow 240ms ease,
    transform 240ms ease;

  .el-icon {
    font-size: 14px;
  }

  &:hover:not(:disabled) {
    color: $text-primary;
    background: linear-gradient(135deg, #f9fafb, #f3f4f6);
    border-color: #d1d5db;
    box-shadow: 0 2px 8px rgba(17, 24, 39, 0.06);
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:focus-visible {
    outline: 2px solid $border-focus;
    outline-offset: 2px;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// ---------- Form ----------
.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  padding-bottom: 6px;
  line-height: 1;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: #fff;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.04);
  transition:
    border-color 220ms ease,
    box-shadow 220ms ease;

  &:hover {
    border-color: $border;
  }
}

:deep(.el-input.is-focus .el-input__wrapper) {
  border-color: $border-focus;
  box-shadow:
    0 0 0 3px rgba(99, 102, 241, 0.08),
    0 1px 3px rgba(17, 24, 39, 0.04);
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: $text-primary;

  &::placeholder {
    color: $text-muted;
  }
}

:deep(.el-input__count) {
  font-size: 12px;
  color: $text-muted;
}

:deep(.el-textarea__inner) {
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: #fff;
  font-size: 15px;
  color: $text-primary;
  font-family: inherit;
  line-height: 1.6;
  resize: none;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.04);
  transition:
    border-color 220ms ease,
    box-shadow 220ms ease;

  &::placeholder {
    color: $text-muted;
  }
}

:deep(.el-textarea.is-focus .el-textarea__inner) {
  border-color: $border-focus;
  box-shadow:
    0 0 0 3px rgba(99, 102, 241, 0.08),
    0 1px 3px rgba(17, 24, 39, 0.04);
}

:deep(.el-textarea .el-input__count) {
  font-size: 12px;
  color: $text-muted;
  bottom: 8px;
  right: 14px;
}

:deep(.el-form-item__error) {
  font-size: 12px;
  padding-top: 4px;
}

// ---------- Actions ----------
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 4px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 24px;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  white-space: nowrap;
  transition:
    background 280ms ease,
    color 280ms ease,
    border-color 280ms ease,
    box-shadow 280ms ease,
    transform 280ms ease;

  .el-icon {
    font-size: 15px;
  }

  &:focus-visible {
    outline: 2px solid $border-focus;
    outline-offset: 2px;
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-cancel {
  background: #fff;
  color: $text-secondary;
  border: 1px solid $border;
  border-radius: $radius-sm;

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #f9fafb, #f3f4f6);
    color: $text-primary;
    border-color: #d1d5db;
    box-shadow: 0 2px 8px rgba(17, 24, 39, 0.06);
    transform: translateY(-1px);
  }
}

.btn-save {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  border-radius: $radius-sm;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #059669, #047857);
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
    transform: translateY(-1px);
  }
}
</style>
