import api from './index'
import type { Result } from '@/types'

export interface SessionInfo {
  userId: number
  username: string
  role: string
  sessionId: string
  loginTime: string
  expireTime: string
  remainingMinutes: number
  ipAddress: string
}

export interface SessionConfig {
  id: number
  roleName: string
  maxDurationMinutes: number
  warningMinutes: number
  isEnabled: boolean
}

export interface SessionLog {
  id: number
  userId: number
  username: string
  sessionId: string
  eventType: string
  ipAddress: string
  loginTime: string
  expireTime: string
  eventTime: string
  remark: string
}

export interface SessionConfigUpdateDTO {
  maxDurationMinutes: number
  warningMinutes: number
  isEnabled: boolean
}

export interface SessionLogPageResult {
  records: SessionLog[]
  total: number
  size: number
  current: number
  pages: number
}

export const sessionApi = {
  getActiveSessions: async (): Promise<SessionInfo[]> => {
    const res = await api.get<Result<SessionInfo[]>>('/admin/sessions')
    return res.data.data
  },

  kickUser: async (userId: number): Promise<void> => {
    await api.delete<Result<null>>(`/admin/sessions/${userId}`)
  },

  getSessionConfigs: async (): Promise<SessionConfig[]> => {
    const res = await api.get<Result<SessionConfig[]>>('/admin/sessions/config')
    return res.data.data
  },

  updateSessionConfig: async (roleName: string, data: SessionConfigUpdateDTO): Promise<void> => {
    await api.put<Result<null>>(`/admin/sessions/config/${roleName}`, data)
  },

  getSessionLogs: async (params: {
    page?: number
    pageSize?: number
    eventType?: string
    username?: string
    startTime?: string
    endTime?: string
  }): Promise<SessionLogPageResult> => {
    const res = await api.get<Result<SessionLogPageResult>>('/admin/sessions/logs', { params })
    return res.data.data
  },
}
