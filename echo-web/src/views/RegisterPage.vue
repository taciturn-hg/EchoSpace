<template>
  <div class="register-page">
    <div class="register-card" :class="{ visible: mounted }">
      <div class="card-header">
        <img src="@/assets/header logo.png" alt="EchoSpace" class="logo" />
        <p class="tagline">注册</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="UserIcon"
            clearable
            maxlength="20"
          />
        </el-form-item>

        <!-- TODO: 后续可将手机号和邮箱分开注册，支持选择注册方式 -->
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="手机号"
            size="large"
            :prefix-icon="PhoneIcon"
            clearable
            maxlength="11"
          />
          <!-- TODO: 后续可接入短信验证码 -->
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱"
            size="large"
            :prefix-icon="MessageIcon"
            clearable
          />
          <!-- TODO: 后续可接入邮箱验证链接 -->
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（6-20 位）"
            size="large"
            :prefix-icon="LockIcon"
            show-password
            maxlength="20"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            :prefix-icon="LockIcon"
            show-password
            maxlength="20"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <!-- TODO: 后续可添加图形验证码或滑块验证机制 -->

        <div class="form-actions">
          <el-button
            type="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </div>
      </el-form>

      <div class="card-footer">
        <span class="hint">已有账号？</span>
        <router-link to="/login" class="link">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Phone, Message } from '@element-plus/icons-vue'
import type { RegisterDTO } from '@/api/modules'
import { register } from '@/api/auth'

const UserIcon = markRaw(User)
const LockIcon = markRaw(Lock)
const PhoneIcon = markRaw(Phone)
const MessageIcon = markRaw(Message)

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const mounted = ref(false)

const form = ref<RegisterDTO>({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== form.value.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度须在 2~20 之间', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度须在 6~20 之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  if (!formRef.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register(form.value)
    if (res.code !== 1) {
      ElMessage.error(res.msg || '注册失败')
      return
    }
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 异常已在 axios 响应拦截器中统一提示，这里仅吞掉避免 unhandledrejection
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  requestAnimationFrame(() => {
    mounted.value = true
  })
})
</script>

<style lang="scss" scoped>
@use 'sass:color';
$primary: #4f6ef7;
$primary-hover: #3b5bdb;
$gray: #6b7280;
$gray-hover: #4b5563;
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$border: #e5e7eb;
$radius-card: 16px;
$radius-input: 8px;

.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border: 1px solid $border;
  border-radius: $radius-card;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.06),
    0 8px 32px rgba(0, 0, 0, 0.08);
  padding: 30px 36px 32px;
  opacity: 0;
  transform: translateY(12px);
  transition:
    opacity 0.4s ease,
    transform 0.4s ease;

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
}

.logo {
  width: 288px;
  object-fit: contain;
}

.tagline {
  font-size: 32px;
  font-weight: 700;
  font-family: 'Noto Sans SC', 'Source Han Sans SC', sans-serif;
  color: $text-primary;
  margin-bottom: 16px;
}

.register-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-input__wrapper) {
    border-radius: $radius-input;
    box-shadow: 0 0 0 1px $border;
    transition:
      box-shadow 0.2s ease,
      background 0.2s ease;
    background: #fafafa;

    &:hover {
      box-shadow: 0 0 0 1px color.adjust($border, $lightness: 10%);
    }

    &.is-focus {
      box-shadow:
        0 0 0 2px rgba($primary, 0.25),
        0 0 0 1px $primary;
      background: #ffffff;
    }
  }

  :deep(.el-input__inner) {
    font-size: 14px;
    color: $text-primary;

    &::placeholder {
      color: #9ca3af;
    }
  }

  :deep(.el-input__prefix-inner .el-icon) {
    color: #9ca3af;
    font-size: 16px;
  }
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.register-btn {
  flex: 1;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: $radius-input;
  background: $primary;
  border-color: $primary;
  letter-spacing: 0.3px;
  transition:
    background 0.25s ease,
    box-shadow 0.25s ease,
    transform 0.15s ease;

  &:hover:not(:disabled) {
    background: $primary-hover;
    border-color: $primary-hover;
    box-shadow: 0 4px 16px rgba($primary, 0.35);
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: none;
  }
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 20px;
  font-size: 13px;
}

.hint {
  color: $text-secondary;
}

.link {
  color: $primary;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;

  &:hover {
    color: $primary-hover;
    text-decoration: underline;
  }
}
</style>
