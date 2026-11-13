import http from './http'
import type { PageResult, Shop } from '@/types'

export interface AdminShopQuery {
  /** PENDING / ACTIVE / REJECTED，不传查全部 */
  status?: string
  page?: number
  size?: number
}

export const adminShopApi = {
  list: (params: AdminShopQuery) => http.get<PageResult<Shop>>('/api/admin/shops', { params }),
  approve: (id: number) => http.post<Shop>(`/api/admin/shops/${id}/approve`),
  reject: (id: number, reason: string) =>
    http.post<Shop>(`/api/admin/shops/${id}/reject`, { reason })
}
