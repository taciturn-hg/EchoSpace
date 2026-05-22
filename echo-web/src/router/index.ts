import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/Register.vue'),
      meta: { guest: true },
    },
    {
      // TODO: 替换为独立的 NotFound.vue 页面，带返回首页按钮，而非静默重定向
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
    {
      path: '/',
      component: () => import('@/components/Layout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/Home.vue'),
        },
        {
          path: 'post/:id',
          name: 'post-detail',
          component: () => import('@/views/PostDetail.vue'),
        },
        {
          path: 'post/create',
          name: 'post-create',
          component: () => import('@/views/PostCreate.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/Search.vue'),
        },
        {
          path: 'user/:id',
          name: 'user-profile',
          component: () => import('@/views/UserProfile.vue'),
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/Settings.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
  ],
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const store = useUserStore()

  // 已登录用户访问登录/注册页 → 跳转首页
  if (to.meta.guest && store.isLoggedIn) {
    return next('/')
  }

  // 未登录用户访问需认证页面 → 跳转登录页
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return next('/login')
  }

  next()
})

export default router
