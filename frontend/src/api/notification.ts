import api from './index'
import type { Result } from '../types'

const handleResponse = <T>(response: { data: Result<T> }): T => {
  const res = response.data
  if (res.code !== 200) {
    throw new Error(res.message || '请求失败')
  }
  return res.data
}

export interface Notification {
  id: number
  userId: number
  type: 'ALERT' | 'SYSTEM' | 'ACTIVITY'
  title: string
  content: string
  link: string | null
  isRead: number
  readTime: string | null
  createTime: string
}

export interface NotificationPage {
  records: Notification[]
  total: number
  pages: number
  current: number
}

export const notificationApi = {
  // 获取通知列表
  getNotifications: async (type?: string, pageNum: number = 1, pageSize: number = 20) => {
    const response = await api.get<Result<NotificationPage>>('/notification/list', {
      params: { type, pageNum, pageSize }
    })
    return handleResponse(response)
  },

  // 获取未读数量
  getUnreadCount: async () => {
    const response = await api.get<Result<number>>('/notification/unread-count')
    return handleResponse(response)
  },

  // 标记单条已读
  markAsRead: async (id: number) => {
    const response = await api.post<Result<boolean>>(`/notification/read/${id}`)
    return handleResponse(response)
  },

  // 批量标记已读
  batchMarkAsRead: async (ids: number[]) => {
    const response = await api.post<Result<boolean>>('/notification/read', ids)
    return handleResponse(response)
  },

  // 标记全部已读
  markAllAsRead: async () => {
    const response = await api.post<Result<number>>('/notification/read-all')
    return handleResponse(response)
  },

  // 删除通知
  delete: async (id: number) => {
    const response = await api.delete<Result<boolean>>(`/notification/${id}`)
    return handleResponse(response)
  },
}
