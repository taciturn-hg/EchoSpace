<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { changePassword } from '@/api/users'
import type { ChangePasswordDTO } from '@/api/modules'

const formRef = ref<FormInstance>()

const form = reactive<ChangePasswordDTO>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' },
    { max: 20, message: '新密码长度不能超过 20 位', trigger: 'blur' },
    {
      validator: (_rule, value: unknown, callback) => {
        if (value && value === form.oldPassword) {
          return callback(new Error('新密码不能与原密码相同'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: unknown, callback) => {
        if (value && value !== form.newPassword) {
          return callback(new Error('两次输入的密码不一致'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const initial = reactive<ChangePasswordDTO>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const saving = ref(false)

const isDirty = computed(() =>
  form.oldPassword !== initial.oldPassword
  || form.newPassword !== initial.newPassword
  || form.confirmPassword !== initial.confirmPassword,
)

function resetForm() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  initial.oldPassword = ''
  initial.newPassword = ''
  initial.confirmPassword = ''
  formRef.value?.clearValidate()
}

async function handleSave() {
  try {
    await formRef.value!.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const res = await changePassword({ ...form })
    if (res.code === 1) {
      ElMessage.success(res.msg || '密码修改成功')
      resetForm()
    }
  } catch {
    // 拦截器统一处理
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  resetForm()
  ElMessage.info('已清空表单')
}
</script>

<template>
  <div class="change-password-page">
    <div class="settings-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="password-form"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            maxlength="20"
            placeholder="输入当前密码"
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            maxlength="20"
            placeholder="输入新密码（6~20 位）"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            maxlength="20"
            placeholder="再次输入新密码"
            autocomplete="new-password"
          />
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <button
          class="btn btn-cancel"
          type="button"
          :disabled="saving"
          @click="handleCancel"
        >
          <el-icon><EditPen /></el-icon>
          <span>清空</span>
        </button>
        <button
          class="btn btn-save"
          type="button"
          :disabled="!isDirty || saving"
          @click="handleSave"
        >
          <span>{{ saving ? '保存中…' : '修改密码' }}</span>
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

// ---------- Page ----------
.change-password-page {
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
.password-form {
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
