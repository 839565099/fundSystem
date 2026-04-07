import api from './index'
import type { LoginDTO, RegisterDTO, UserVO, UserStats, Result } from '../types'

export interface LoginResponse {
  token: string
  user: UserVO
  sessionInfo: {
    sessionId: string
    loginTime: string
    expireTime: string
    maxDurationMinutes: number
    warningMinutes: number
  }
}

const handleResponse = <T>(response: { data: Result<T> }): T => {
  const res = response.data
  if (res.code !== 200) {
    throw new Error(res.message || '请求失败')
  }
  return res.data
}

export const authApi = {
  login: async (data: LoginDTO): Promise<LoginResponse> => {
    const response = await api.post<Result<LoginResponse>>('/auth/login', data)
    return handleResponse(response)
  },

  register: async (data: RegisterDTO) => {
    const response = await api.post<Result<UserVO>>('/auth/register', data)
    return handleResponse(response)
  },

  logout: async () => {
    await api.post<Result<null>>('/auth/logout')
  },

  getProfile: async () => {
    const response = await api.get<Result<UserVO>>('/auth/info')
    return handleResponse(response)
  },

  updateProfile: async (data: Partial<UserVO>) => {
    const response = await api.put<Result<UserVO>>('/auth/profile', data)
    return handleResponse(response)
  },

  changePassword: async (oldPassword: string, newPassword: string) => {
    const response = await api.put<Result<null>>('/auth/password', { oldPassword, newPassword })
    return handleResponse(response)
  },

  getUserStats: async () => {
    const response = await api.get<Result<UserStats>>('/auth/stats')
    return handleResponse(response)
  },

  forgotPassword: async (email: string, type: 'link' | 'code' = 'link') => {
    const response = await api.post<Result<string>>('/auth/forgot-password', { email, type })
    return handleResponse(response)
  },

  validateResetToken: async (token: string) => {
    const response = await api.get<Result<boolean>>('/auth/validate-reset-token', { params: { token } })
    return handleResponse(response)
  },

  validateResetCode: async (email: string, code: string) => {
    const response = await api.post<Result<boolean>>('/auth/validate-reset-code', null, { params: { email, code } })
    return handleResponse(response)
  },

  resetPassword: async (data: { token?: string; email?: string; code?: string; newPassword: string }) => {
    const response = await api.post<Result<string>>('/auth/reset-password', data)
    return handleResponse(response)
  },

  checkGoogleAuth: async (): Promise<{ enabled: boolean }> => {
    try {
      const response = await api.get<Result<{ enabled: boolean }>>('/auth/google/config')
      return handleResponse(response)
    } catch {
      return { enabled: false }
    }
  },

  sendEmailLoginCode: async (email: string) => {
    const response = await api.post<Result<string>>('/auth/email-login-code', { email })
    return handleResponse(response)
  },

  emailLogin: async (email: string, code: string): Promise<LoginResponse> => {
    const response = await api.post<Result<LoginResponse>>('/auth/email-login', { email, code })
    return handleResponse(response)
  },

  updateUsername: async (username: string) => {
    const response = await api.put<Result<UserVO>>('/auth/username', { username })
    return handleResponse(response)
  },

  setPassword: async (password: string) => {
    const response = await api.post<Result<string>>('/auth/set-password', { password })
    return handleResponse(response)
  },

  hasPassword: async (): Promise<{ hasPassword: boolean }> => {
    const response = await api.get<Result<{ hasPassword: boolean }>>('/auth/has-password')
    return handleResponse(response)
  },
}
