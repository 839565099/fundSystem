import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginResponse } from '../api/auth'
import type { UserVO, LoginDTO, RegisterDTO } from '../types'

export interface SessionInfo {
  sessionId: string
  loginTime: string
  expireTime: string
  maxDurationMinutes: number
  warningMinutes: number
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserVO | null>(null)
  const sessionInfo = ref<SessionInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const role = computed(() => user.value?.role || 'USER')

  const login = async (data: LoginDTO) => {
    const loginResponse: LoginResponse = await authApi.login(data)
    token.value = loginResponse.token
    user.value = loginResponse.user
    sessionInfo.value = loginResponse.sessionInfo
    localStorage.setItem('token', loginResponse.token)
  }

  const emailLogin = async (email: string, code: string) => {
    const loginResponse: LoginResponse = await authApi.emailLogin(email, code)
    token.value = loginResponse.token
    user.value = loginResponse.user
    sessionInfo.value = loginResponse.sessionInfo
    localStorage.setItem('token', loginResponse.token)
  }

  const register = async (data: RegisterDTO) => {
    const userVO = await authApi.register(data)
    user.value = userVO
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch {
      // 即使后端登出失败也要清理前端状态
    }
    token.value = null
    user.value = null
    sessionInfo.value = null
    localStorage.removeItem('token')
  }

  const forceLogout = () => {
    token.value = null
    user.value = null
    sessionInfo.value = null
    localStorage.removeItem('token')
  }

  const fetchProfile = async () => {
    if (!token.value) return
    try {
      user.value = await authApi.getProfile()
    } catch (e) {
      console.error('fetchProfile error:', e)
      forceLogout()
    }
  }

  if (token.value) {
    fetchProfile()
  }

  return {
    token,
    user,
    sessionInfo,
    isLoggedIn,
    isAdmin,
    role,
    login,
    emailLogin,
    register,
    logout,
    forceLogout,
    fetchProfile,
  }
})
