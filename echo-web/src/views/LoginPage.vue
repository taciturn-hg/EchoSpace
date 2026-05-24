<template>
  <div class="login-page">
    <div class="login-card" :class="{ visible: mounted }">
      <div class="card-header">
        <img src="@/assets/header logo.png" alt="EchoSpace" class="logo" />
        <p class="tagline">登录</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="account">
          <el-input
            v-model="form.account"
            placeholder="用户名 / 邮箱 / 手机号"
            size="large"
            :prefix-icon="UserIcon"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="LockIcon"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-actions">
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </div>
      </el-form>

      <div class="card-footer">
        <!-- <router-link to="/forgot-password" class="link">忘记密码？</router-link> -->
        <span class="link link--disabled">忘记密码？</span>
        <span class="divider">·</span>
        <router-link to="/register" class="link">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'
import { login } from '@/api/auth'
import type { LoginDTO } from '@/api/modules'

const UserIcon = markRaw(User)
const LockIcon = markRaw(Lock)

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const mounted = ref(false)

const form = ref<LoginDTO>({
  account: '',
  password: '',
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form.value)
    const { accessToken, refreshToken } = res.data!
    userStore.setToken(accessToken, refreshToken)
    ElMessage.success('登录成功')
    router.push('/')
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
$text-primary: #1a1a2e;
$text-secondary: #6b7280;
$border: #e5e7eb;
$radius-card: 16px;
$radius-input: 8px;

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
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

.brand {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}

.tagline {
  font-size: 32px;
  font-weight: 700;
  font-family: 'Noto Sans SC', 'Source Han Sans SC', sans-serif;
  color: $text-primary;
  margin-bottom: 16px;
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
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
  margin-top: 8px;
}

.login-btn {
  width: 100%;
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
  gap: 8px;
  margin-top: 24px;
  font-size: 13px;
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

.link--disabled {
  color: #9ca3af;
  cursor: not-allowed;
  font-weight: 500;
}

.divider {
  color: $border;
  user-select: none;
}
</style>
