import http from './http'
import type { PageResult, ProductDetail, ProductListItem } from '@/types'

/** 后端 ProductStatus 枚举 */
export type AdminProductStatus = 'PENDING_REVIEW' | 'ON_SHELF' | 'OFF_SHELF'

export interface AdminProductQuery {
  status?: AdminProductStatus
  keyword?: string
  page?: number
  size?: number
}

export const adminProductApi = {
  list: (params: AdminProductQuery) =>
    http.get<PageResult<ProductListItem>>('/api/admin/products', { params }),
  approve: (id: number) => http.post<ProductDetail>(`/api/admin/products/${id}/approve`),
  reject: (id: number, reason: string) =>
    http.post<ProductDetail>(`/api/admin/products/${id}/reject`, { reason })
}
