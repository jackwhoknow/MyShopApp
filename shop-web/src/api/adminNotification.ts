import http from './http'
import type { AdminBroadcast, AudienceCounts, NotificationAudience, PageResult } from '@/types'

export interface AdminBroadcastPayload {
  title: string
  content: string
  audience: NotificationAudience
  link?: string
}

export const adminNotificationApi = {
  audienceCounts: () =>
    http.get<AudienceCounts>('/api/admin/notifications/audience-counts'),
  broadcast: (payload: AdminBroadcastPayload) =>
    http.post<AdminBroadcast>('/api/admin/notifications/broadcast', payload),
  history: (params: { page?: number; size?: number }) =>
    http.get<PageResult<AdminBroadcast>>('/api/admin/notifications/broadcast', { params })
}
