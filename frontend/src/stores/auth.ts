import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import type { UserVO, LoginDTO, RegisterDTO } from '../types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  const login = async (data: LoginDTO) => {
    const tokenValue = await authApi.login(data)
    token.value = tokenValue
    localStorage.setItem('token', tokenValue)
    await fetchProfile()
  }

  const register = async (data: RegisterDTO) => {
    const userVO = await authApi.register(data)
    user.value = userVO
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  const fetchProfile = async () => {
    if (!token.value) return
    try {
      user.value = await authApi.getProfile()
    } catch {
      logout()
    }
  }

  if (token.value) {
    fetchProfile()
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    register,
    logout,
    fetchProfile,
  }
})
