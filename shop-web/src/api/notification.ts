import http from './http'
import type { NotificationMessage, PageResult } from '@/types'

export const notificationApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    http.get<PageResult<NotificationMessage>>('/api/notifications', { params }),
  unreadCount: () => http.get<number>('/api/notifications/unread-count'),
  markRead: (id: number) => http.post<void>(`/api/notifications/${id}/read`),
  markAllRead: () => http.post<void>('/api/notifications/read-all')
}
