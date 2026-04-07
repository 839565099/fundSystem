import api from './index'
import type { Result, UserVO } from '@/types'

interface PageParams {
  page: number
  pageSize: number
  keyword?: string
  role?: string
  status?: number
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

interface SystemStats {
  totalUsers: number
  activeUsers: number
  adminCount: number
  todayNewUsers: number
}

// 操作日志接口
export interface OperationLogItem {
  id: number
  userId: number
  username: string
  operation: string
  targetType: string
  targetId: string
  detail: string
  ip: string
  createTime: string
}

// 系统日志接口
export interface SystemLogItem {
  id: number
  logType: string
  content: string
  status: string
  errorMsg: string
  durationMs: number
  createTime: string
}

interface LogPageParams {
  page: number
  pageSize: number
  operation?: string
  username?: string
  logType?: string
}

export const adminApi = {
  // 获取系统统计
  getStats: async (): Promise<SystemStats> => {
    const res = await api.get<Result<SystemStats>>('/admin/stats')
    return res.data.data
  },

  // 获取用户列表
  getUsers: async (params: PageParams): Promise<PageResult<UserVO>> => {
    const res = await api.get<Result<PageResult<UserVO>>>('/admin/users', { params })
    return res.data.data
  },

  // 获取用户详情
  getUser: async (id: number): Promise<UserVO> => {
    const res = await api.get<Result<UserVO>>(`/admin/users/${id}`)
    return res.data.data
  },

  // 更新用户状态
  updateUserStatus: async (id: number, status: number): Promise<void> => {
    await api.put<Result<null>>(`/admin/users/${id}/status`, null, {
      params: { status }
    })
  },

  // 更新用户角色
  updateUserRole: async (id: number, role: string): Promise<void> => {
    await api.put<Result<null>>(`/admin/users/${id}/role`, null, {
      params: { role }
    })
  },

  // 更新用户基本信息
  updateUserInfo: async (id: number, data: { nickname?: string; email?: string; phone?: string }): Promise<void> => {
    await api.put<Result<null>>(`/admin/users/${id}`, data)
  },

  // 删除用户
  deleteUser: async (id: number): Promise<void> => {
    await api.delete<Result<null>>(`/admin/users/${id}`)
  },

  // 获取操作日志
  getOperationLogs: async (params: LogPageParams): Promise<PageResult<OperationLogItem>> => {
    const res = await api.get<Result<PageResult<OperationLogItem>>>('/admin/logs/operations', { params })
    return res.data.data
  },

  // 获取系统日志
  getSystemLogs: async (params: LogPageParams): Promise<PageResult<SystemLogItem>> => {
    const res = await api.get<Result<PageResult<SystemLogItem>>>('/admin/logs/systems', { params })
    return res.data.data
  },
}
