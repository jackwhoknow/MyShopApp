import http from './http'
import type { AfterSale, PageResult } from '@/types'

export interface AdminAfterSaleQuery {
  status?: string
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface AdminAfterSaleActionPayload {
  remark: string
}

/** 平台售后监管 */
export const adminAfterSaleApi = {
  list: (params: AdminAfterSaleQuery) =>
    http.get<PageResult<AfterSale>>('/api/admin/after-sales', { params }),
  refund: (id: number, data: AdminAfterSaleActionPayload) =>
    http.post<AfterSale>(`/api/admin/after-sales/${id}/refund`, data),
  close: (id: number, data: AdminAfterSaleActionPayload) =>
    http.post<AfterSale>(`/api/admin/after-sales/${id}/close`, data)
}
