import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { LoginResult, UserInfo } from '@/types'

const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refresh_token'
const USER_KEY = 'user'

function readUser(): UserInfo | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

/**
 * 登录态：token 同时存 localStorage，刷新页面不丢失。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserInfo | null>(readUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const isMerchant = computed(() =>
    (user.value?.roles ?? []).some((role) => role === 'MERCHANT' || role === 'ADMIN')
  )
  const isAdmin = computed(() => (user.value?.roles ?? []).includes('ADMIN'))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function setLogin(result: LoginResult) {
    token.value = result.token
    user.value = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(REFRESH_KEY, result.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  /** 资料变更后同步内存与 localStorage，顶栏昵称即时刷新 */
  function updateUser(patch: Partial<UserInfo>) {
    if (!user.value) {
      return
    }
    user.value = { ...user.value, ...patch }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, isMerchant, isAdmin, displayName, setLogin, updateUser, logout }
})
