<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { getSettings, updateSettings } from '@/api/users'
import { useUserStore } from '@/stores/userStore'
import type { UpdateSettingsDTO, UserSettingsVO } from '@/api/modules'

const userStore = useUserStore()

const formRef = ref<FormInstance>()

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (_rule, value: unknown, callback) => {
        if (!value) return callback()
        if (!/^1[3-9]\d{9}$/.test(value as string)) return callback(new Error('手机号格式不正确'))
        callback()
      },
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      validator: (_rule, value: unknown, callback) => {
        if (!value) return callback()
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value as string))
          return callback(new Error('邮箱格式不正确'))
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const form = reactive<UpdateSettingsDTO>({
  phone: undefined,
  email: undefined,
})

const original = reactive<UpdateSettingsDTO>({
  phone: undefined,
  email: undefined,
})

const saving = ref(false)
const loading = ref(true)

const isDirty = computed(() => form.phone !== original.phone || form.email !== original.email)

function applySettings(s: UserSettingsVO) {
  form.phone = s.phone ?? undefined
  form.email = s.email ?? undefined
  original.phone = s.phone ?? undefined
  original.email = s.email ?? undefined
}

async function fetchSettings() {
  try {
    const res = await getSettings()
    if (res.data) {
      applySettings(res.data)
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

async function handleSave() {
  try {
    await formRef.value!.validate()
  } catch {
    return
  }

  const dto: UpdateSettingsDTO = {}
  if (form.phone !== original.phone) dto.phone = form.phone
  if (form.email !== original.email) dto.email = form.email

  if (Object.keys(dto).length === 0) {
    ElMessage.info('没有修改的内容')
    return
  }

  saving.value = true
  try {
    await updateSettings(dto)
    ElMessage.success('账号信息已更新')
    const info = userStore.userInfo
    if (info) {
      userStore.setUserInfo({
        ...info,
        phone: dto.phone ?? info.phone,
        email: dto.email ?? info.email,
      })
    }
    original.phone = form.phone
    original.email = form.email
  } catch {
    // 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleCancel() {
  loading.value = true
  const ok = await fetchSettings()
  formRef.value?.clearValidate()
  if (ok) {
    ElMessage.info('已还原为服务器数据')
  } else {
    ElMessage.warning('还原失败，请稍后重试')
  }
}

onMounted(() => {
  fetchSettings()
})
</script>

<template>
  <div class="settings-page">
    <div class="settings-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="settings-form"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            maxlength="11"
            placeholder="输入手机号"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            maxlength="320"
            placeholder="输入邮箱地址"
            autocomplete="off"
          />
        </el-form-item>
      </el-form>

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
$radius-sm: 8px;
$radius-md: 10px;

// ---------- Page ----------
.settings-page {
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

// ---------- Form ----------
.settings-form {
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
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #059669, #047857);
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
    transform: translateY(-1px);
  }
}
</style>
