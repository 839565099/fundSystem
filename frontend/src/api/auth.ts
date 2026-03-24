import api from './index'
import type { LoginDTO, RegisterDTO, UserVO, UserStats, Result } from '../types'

const handleResponse = <T>(response: { data: Result<T> }): T => {
  const res = response.data
  if (res.code !== 200) {
    throw new Error(res.message || '请求失败')
  }
  return res.data
}

export const authApi = {
  login: async (data: LoginDTO) => {
    const response = await api.post<Result<string>>('/auth/login', data)
    return handleResponse(response)
  },

  register: async (data: RegisterDTO) => {
    const response = await api.post<Result<UserVO>>('/auth/register', data)
    return handleResponse(response)
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

  // 密码重置相关
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
}
