import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/utils/request'
import type { CampusAuthPayload, LoginPayload, ProfileUpdatePayload, RegisterPayload, UserVO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const user = ref<UserVO | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isVerified = computed(() => user.value?.authStatus === 'VERIFIED')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const accountStatus = computed(() => user.value?.accountStatus ?? 'NORMAL')
  const canTrade = computed(
    () => isLoggedIn.value && isVerified.value && accountStatus.value !== 'BANNED'
  )

  function applyLogin(newToken: string, profile: UserVO) {
    token.value = newToken
    setToken(newToken)
    user.value = profile
  }

  async function login(payload: LoginPayload) {
    const result = await authApi.login(payload)
    applyLogin(result.token, result.user)
    return result.user
  }

  async function register(payload: RegisterPayload) {
    const result = await authApi.register(payload)
    applyLogin(result.token, result.user)
    return result.user
  }

  async function fetchMe(force = false) {
    if (!token.value) return null
    if (user.value && !force) return user.value
    loading.value = true
    try {
      user.value = await authApi.me()
      return user.value
    } catch (e) {
      logout()
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(payload: ProfileUpdatePayload) {
    user.value = await authApi.updateProfile(payload)
    return user.value
  }

  async function campusAuth(payload: CampusAuthPayload) {
    user.value = await authApi.campusAuth(payload)
    return user.value
  }

  function logout() {
    token.value = ''
    user.value = null
    clearToken()
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    isVerified,
    isAdmin,
    accountStatus,
    canTrade,
    login,
    register,
    fetchMe,
    updateProfile,
    campusAuth,
    logout
  }
})
