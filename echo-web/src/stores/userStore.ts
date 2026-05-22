import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserInfo } from '@/api/modules'

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const refreshToken = ref<string>('')
    const isLoggedIn = computed(() => !!token.value)
    const userInfo = ref<UserInfo | null>(null)
    const refreshingPromise = ref<Promise<void> | null>(null)

    function setToken(access: string, refresh: string) {
      token.value = access
      refreshToken.value = refresh
    }

    function clearAuth() {
      token.value = ''
      refreshToken.value = ''
      userInfo.value = null
      refreshingPromise.value = null
    }

    function setUserInfo(info: UserInfo) {
      userInfo.value = info
    }

    return {
      token,
      refreshToken,
      isLoggedIn,
      userInfo,
      refreshingPromise,
      setToken,
      clearAuth,
      setUserInfo,
    }
  },
  {
    persist: true,
  },
)
