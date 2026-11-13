import http from './http'
import type { AfterSale, PageResult } from '@/types'

export interface AfterSaleApplyPayload {
  subOrderId: number
  reason: string
}

export interface AfterSaleQuery {
  status?: string
  page?: number
  size?: number
}

export interface AfterSaleHandlePayload {
  merchantRemark?: string
}

export interface ReturnShipmentPayload {
  returnCompany: string
  returnTrackingNo: string
}

/** 买家售后 */
export const afterSaleApi = {
  apply: (data: AfterSaleApplyPayload) => http.post<AfterSale>('/api/after-sales', data),
  listMine: (params: { page?: number; size?: number }) =>
    http.get<PageResult<AfterSale>>('/api/after-sales', { params }),
  returnShipment: (id: number, data: ReturnShipmentPayload) =>
    http.post<AfterSale>(`/api/after-sales/${id}/return-shipment`, data)
}

/** 商家售后处理 */
export const merchantAfterSaleApi = {
  list: (params: AfterSaleQuery) =>
    http.get<PageResult<AfterSale>>('/api/merchant/after-sales', { params }),
  approve: (id: number, data: AfterSaleHandlePayload = {}) =>
    http.post<AfterSale>(`/api/merchant/after-sales/${id}/approve`, data),
  reject: (id: number, data: AfterSaleHandlePayload = {}) =>
    http.post<AfterSale>(`/api/merchant/after-sales/${id}/reject`, data),
  confirmReceipt: (id: number) =>
    http.post<AfterSale>(`/api/merchant/after-sales/${id}/confirm-receipt`)
}
