import http from './http'
import type { AdminUser, PageResult } from '@/types'

export interface AdminUserQuery {
  keyword?: string
  /** BUYER / MERCHANT / ADMIN，不传查全部 */
  role?: string
  /** ACTIVE / DISABLED，不传查全部 */
  status?: string
  page?: number
  size?: number
}

export const adminUserApi = {
  list: (params: AdminUserQuery) => http.get<PageResult<AdminUser>>('/api/admin/users', { params }),
  updateStatus: (id: number, status: 'ACTIVE' | 'DISABLED') =>
    http.put<AdminUser>(`/api/admin/users/${id}/status`, { status })
}
