import http from './http'
import type { AdminAuditLog, PageResult } from '@/types'

export interface AuditLogQuery {
  keyword?: string
  page?: number
  size?: number
}

export const adminAuditApi = {
  list: (params: AuditLogQuery) =>
    http.get<PageResult<AdminAuditLog>>('/api/admin/audit-logs', { params })
}
