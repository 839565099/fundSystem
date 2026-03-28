import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import type { UserVO, LoginDTO, RegisterDTO } from '../types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const role = computed(() => user.value?.role || 'USER')

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
      console.log('fetchProfile result:', user.value)
      console.log('role:', user.value?.role)
    } catch (e) {
      console.error('fetchProfile error:', e)
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
    isAdmin,
    role,
    login,
    register,
    logout,
    fetchProfile,
  }
})
