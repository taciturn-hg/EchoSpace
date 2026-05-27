<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElIcon } from 'element-plus'
import {
  ArrowDown,
  Setting,
  SwitchButton,
  Fold,
  Expand,
  Search,
  HomeFilled,
  UserFilled,
  Plus,
  Edit,
  Lock,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'
import { me } from '@/api/auth'
import logoUrl from '@/assets/header logo.png'

interface NavItem {
  key: 'home' | 'profile'
  label: string
  to: string
  icon: unknown
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mounted = ref(false)
const sidebarCollapsed = ref(false)
const searchQuery = ref('')

const userInfo = computed(() => userStore.userInfo)

const displayName = computed<string>(
  () => userInfo.value?.nickname || userInfo.value?.username || '未登录用户',
)

const avatarUrl = computed<string>(() => userInfo.value?.avatar ?? '')

const avatarFallback = computed<string>(() => {
  const source = userInfo.value?.nickname || userInfo.value?.username || 'E'
  return source.charAt(0).toUpperCase()
})

const navItems = computed<NavItem[]>(() => [
  { key: 'home', label: '首页', to: '/', icon: HomeFilled },
  {
    key: 'profile',
    label: '个人主页',
    to: userInfo.value?.id ? `/user/${userInfo.value.id}` : '/',
    icon: UserFilled,
  },
])

function isNavActive(item: NavItem): boolean {
  if (item.key === 'home') return route.path === '/'
  if (item.key === 'profile') return route.path.startsWith('/user/')
  return route.path.startsWith(item.to)
}

async function loadUserInfo(): Promise<void> {
  try {
    const res = await me()
    if (res.data) {
      userStore.setUserInfo({
        id: res.data.id,
        username: res.data.username,
        nickname: res.data.nickname,
        email: res.data.email,
        phone: res.data.phone,
        avatar: res.data.avatar,
        bio: res.data.bio,
      })
    }
  } catch {
    // result.ts 已统一处理错误提示，这里静默
  }
}

function handleDropdown(command: 'profile-settings' | 'settings' | 'change-password' | 'logout'): void {
  if (command === 'profile-settings') {
    router.push('/settings/profile')
    return
  }
  if (command === 'settings') {
    router.push('/settings')
    return
  }
  if (command === 'change-password') {
    router.push('/settings/change-password')
    return
  }
  userStore.clearAuth()
  ElMessage.success('已退出登录')
  router.push('/login')
}

function handleSearch(): void {
  const q = searchQuery.value.trim()
  if (!q) return
  router.push({ name: 'search', query: { q } })
}

onMounted(async () => {
  await loadUserInfo()
  requestAnimationFrame(() => {
    mounted.value = true
  })
})
</script>

<template>
  <div class="layout" :class="{ 'is-mounted': mounted }">
    <!-- 顶栏 -->
    <header class="layout-header">
      <div class="header-inner">
        <el-tooltip content="前往EchoSpace首页" placement="bottom" effect="dark" :show-after="100">
          <RouterLink to="/" class="brand" aria-label="EchoSpace 首页">
            <img :src="logoUrl" alt="EchoSpace" class="brand-logo" />
          </RouterLink>
        </el-tooltip>

        <!-- 搜索区 -->
        <div class="search-area">
          <div class="search-box">
            <input
              v-model="searchQuery"
              class="search-input"
              type="text"
              placeholder="搜索..."
              aria-label="搜索"
              @keydown.enter="handleSearch"
            />
            <button class="search-btn" type="button" aria-label="搜索" @click="handleSearch">
              <el-icon><Search /></el-icon>
            </button>
          </div>
        </div>

        <!-- 用户区 -->
        <div class="user-area">
          <RouterLink to="/post/create" class="btn-post">
            <el-icon><Plus /></el-icon>
            <span>发布帖子</span>
          </RouterLink>

          <RouterLink
            :to="userInfo?.id ? `/user/${userInfo.id}` : '/'"
            class="user-card"
            :title="displayName"
          >
            <div class="avatar">
              <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName" />
              <span v-else class="avatar-fallback">{{ avatarFallback }}</span>
            </div>
            <span class="username">{{ displayName }}</span>
          </RouterLink>

          <el-dropdown
            trigger="click"
            placement="bottom-end"
            :teleported="true"
            popper-class="layout-dropdown-popper"
            @command="handleDropdown"
          >
            <button class="dropdown-trigger" type="button" aria-label="账户菜单">
              <el-icon class="dropdown-caret"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile-settings">
                  <el-icon class="menu-icon"><Edit /></el-icon>
                  <span>资料设置</span>
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon class="menu-icon"><Setting /></el-icon>
                  <span>账号设置</span>
                </el-dropdown-item>
                <el-dropdown-item command="change-password">
                  <el-icon class="menu-icon"><Lock /></el-icon>
                  <span>修改密码</span>
                </el-dropdown-item>
                <el-dropdown-item command="logout" class="dropdown-logout" divided>
                  <el-icon class="menu-icon"><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 主体：左导航 + 内容 -->
    <div class="layout-body">
      <!-- 左侧导航 -->
      <aside class="sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <nav class="sidebar-nav" aria-label="侧边导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.key"
            :to="item.to"
            class="sidebar-item"
            :class="{ 'is-active': isNavActive(item) }"
          >
            <el-icon class="sidebar-icon"><component :is="item.icon" /></el-icon>
            <span class="sidebar-label">{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="sidebar-divider" />

        <button
          class="sidebar-toggle"
          type="button"
          :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="sidebarCollapsed = !sidebarCollapsed"
        >
          <el-icon>
            <Expand v-if="sidebarCollapsed" />
            <Fold v-else />
          </el-icon>
        </button>
      </aside>

      <!-- 内容区 -->
      <main
        class="layout-content"
        :style="{ marginLeft: sidebarCollapsed ? '60px' : 'clamp(140px, 11.11vw, 200px)' }"
      >
        <RouterView v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
$header-height: 64px;
$sidebar-width: 11.11vw;
$sidebar-collapsed-width: 60px;
$border-color: #e5e7eb;
$bg-page: #ffffff;
$bg-header: rgba(249, 250, 251, 0.85);
$bg-sidebar: #f9fafb;
$text-primary: #111827;
$text-secondary: #4b5563;
$text-muted: #6b7280;
$accent: #2563eb;
$accent-soft: rgba(37, 99, 235, 0.08);

.layout {
  min-height: 100vh;
  background: $bg-page;
  color: $text-primary;
  opacity: 0;
  transition:
    opacity 420ms ease;

  &.is-mounted {
    opacity: 1;
  }
}

.layout-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: $bg-header;
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid $border-color;
}

.header-inner {
  height: $header-height;
  padding: 0 24px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
}

.brand {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  border-radius: 8px;
  transition: opacity 200ms ease;

  &:hover {
    opacity: 0.85;
  }
}

.brand-logo {
  height: 60px;
  width: auto;
  display: block;
  user-select: none;
  -webkit-user-drag: none;
}

.search-area {
  justify-self: center;
  width: 100%;
  max-width: 480px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  flex: 1;
  height: 38px;
  padding: 0 16px;
  border: 1px solid $border-color;
  border-radius: 999px;
  background: #ffffff;
  font-size: 14px;
  color: $text-primary;
  outline: none;
  box-shadow: 0 1px 4px rgba(17, 24, 39, 0.06);
  transition:
    border-color 220ms ease,
    box-shadow 220ms ease;

  &::placeholder {
    color: $text-muted;
  }

  &:focus {
    border-color: $accent;
    box-shadow:
      0 0 0 3px rgba(37, 99, 235, 0.1),
      0 1px 4px rgba(17, 24, 39, 0.06);
  }
}

.search-btn {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid $border-color;
  background: #ffffff;
  color: $text-muted;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  box-shadow: 0 1px 4px rgba(17, 24, 39, 0.06);
  transition:
    background 240ms ease,
    color 240ms ease,
    border-color 240ms ease,
    box-shadow 240ms ease,
    transform 240ms ease;

  &:hover {
    background: linear-gradient(135deg, $accent, #7c3aed);
    color: #ffffff;
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  &:focus-visible {
    outline: 2px solid $accent;
    outline-offset: 2px;
  }
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-post {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
  transition:
    background 240ms ease,
    box-shadow 240ms ease,
    transform 240ms ease;

  .el-icon {
    font-size: 15px;
  }

  &:hover {
    background: linear-gradient(135deg, #1d4ed8, #6d28d9);
    box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
  }
}

.user-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 16px 4px 4px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid $border-color;
  text-decoration: none;
  cursor: pointer;
  transition:
    background 280ms ease,
    border-color 280ms ease,
    box-shadow 280ms ease,
    transform 280ms ease;

  &:hover {
    background: linear-gradient(180deg, #ffffff, #f3f4f6);
    border-color: #d1d5db;
    box-shadow: 0 4px 16px rgba(17, 24, 39, 0.06);
    transform: translateY(-1px);
  }
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #e0e7ff, #ede9fe);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.avatar-fallback {
  font-size: 13px;
  font-weight: 600;
  color: $accent;
  letter-spacing: 0.02em;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid $border-color;
  border-radius: 8px;
  background: #ffffff;
  color: $text-muted;
  cursor: pointer;
  transition:
    background 240ms ease,
    color 240ms ease,
    border-color 240ms ease,
    transform 240ms ease;

  &:hover {
    background: linear-gradient(180deg, #f9fafb, #f3f4f6);
    color: $text-primary;
    border-color: #d1d5db;
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  &:focus-visible {
    outline: 2px solid $accent;
    outline-offset: 2px;
  }
}

.dropdown-caret {
  font-size: 12px;
  transition: transform 220ms ease;
}

.dropdown-trigger:hover .dropdown-caret {
  transform: translateY(1px);
}

.layout-body {
  display: flex;
  padding-top: $header-height;
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  top: $header-height;
  left: 0;
  bottom: 0;
  z-index: 50;
  width: $sidebar-width;
  min-width: 140px;
  max-width: 200px;
  background: $bg-sidebar;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  padding: 20px 0 16px;
  transition:
    width 280ms cubic-bezier(0.16, 1, 0.3, 1),
    min-width 280ms cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;

  &.is-collapsed {
    width: $sidebar-collapsed-width;
    min-width: $sidebar-collapsed-width;
  }
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 10px;
  overflow: hidden;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
  text-decoration: none;
  white-space: nowrap;
  transition:
    color 200ms ease,
    background-color 200ms ease;

  &:hover {
    color: $text-primary;
    background: rgba(17, 24, 39, 0.05);
  }

  &.is-active {
    color: $accent;
    background: $accent-soft;
  }
}

.sidebar-icon {
  flex-shrink: 0;
  font-size: 16px;
}

.sidebar-label {
  overflow: hidden;
  text-overflow: ellipsis;
  transition: opacity 200ms ease;

  .is-collapsed & {
    opacity: 0;
    pointer-events: none;
  }
}

.sidebar-divider {
  height: 1px;
  background: $border-color;
  margin: 12px 10px;
  flex-shrink: 0;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid $border-color;
  background: #ffffff;
  color: $text-muted;
  cursor: pointer;
  font-size: 16px;
  margin: 0 auto;
  flex-shrink: 0;
  transition:
    background 240ms ease,
    color 240ms ease,
    border-color 240ms ease,
    box-shadow 240ms ease,
    transform 240ms ease;

  &:hover {
    background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
    color: $text-primary;
    border-color: #d1d5db;
    box-shadow: 0 2px 8px rgba(17, 24, 39, 0.08);
    transform: scale(1.06);
  }

  &:active {
    transform: scale(1);
  }

  &:focus-visible {
    outline: 2px solid $accent;
    outline-offset: 2px;
  }
}

.layout-content {
  flex: 1;
  min-width: 0;
  min-height: calc(100vh - $header-height);
  padding: 24px;
  transition: margin-left 280ms cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-enter-active {
  transition:
    opacity 280ms ease,
    transform 280ms ease;
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.fade-slide-leave-active {
  transition:
    opacity 200ms ease,
    transform 200ms ease;
}

@media (max-width: 900px) {
  .username {
    display: none;
  }
}

@media (max-width: 600px) {
  .header-inner {
    gap: 12px;
    padding: 0 16px;
  }

  .brand-logo {
    height: 36px;
  }
}
</style>

<style lang="scss">
.layout-dropdown-popper.el-popper {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 12px 32px rgba(17, 24, 39, 0.08);
  padding: 4px;
  min-width: 168px;

  .el-dropdown-menu {
    padding: 4px;
    background: #ffffff;
    border: none;
  }

  .el-dropdown-menu__item {
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 14px;
    color: #374151;
    gap: 8px;
    transition:
      background-color 200ms ease,
      color 200ms ease;

    .menu-icon {
      flex-shrink: 0;
      color: #6b7280;
      transition: color 200ms ease;
    }

    &:not(.is-disabled):hover,
    &:not(.is-disabled):focus {
      background: linear-gradient(90deg, rgba(37, 99, 235, 0.06), rgba(124, 58, 237, 0.06));
      color: #111827;

      .menu-icon {
        color: #2563eb;
      }
    }
  }

  .dropdown-logout {
    color: #ef4444;

    .menu-icon {
      color: #ef4444;
    }

    &:not(.is-disabled):hover,
    &:not(.is-disabled):focus {
      background: linear-gradient(90deg, rgba(239, 68, 68, 0.08), rgba(239, 68, 68, 0.14));
      color: #dc2626;

      .menu-icon {
        color: #dc2626;
      }
    }
  }
}
</style>
