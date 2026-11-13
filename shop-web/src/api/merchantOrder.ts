import http from './http'
import type { BatchShipResult, MerchantSubOrder, PageResult, SubOrderLogistics } from '@/types'

export interface MerchantOrderQuery {
  status?: string
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface ShipPayload {
  shipCompany?: string
  trackingNo?: string
}

export interface BatchShipPayload extends ShipPayload {
  subOrderIds: number[]
}

export const merchantOrderApi = {
  list: (params: MerchantOrderQuery) =>
    http.get<PageResult<MerchantSubOrder>>('/api/merchant/orders', { params }),
  detail: (id: number) => http.get<MerchantSubOrder>(`/api/merchant/orders/${id}`),
  /** 子订单物流轨迹（模拟数据） */
  logistics: (id: number) => http.get<SubOrderLogistics>(`/api/merchant/orders/${id}/logistics`),
  ship: (id: number, data: ShipPayload) =>
    http.post<MerchantSubOrder>(`/api/merchant/orders/${id}/ship`, data),
  updateLogistics: (id: number, data: ShipPayload) =>
    http.put<MerchantSubOrder>(`/api/merchant/orders/${id}/logistics`, data),
  updateRemark: (id: number, remark: string) =>
    http.put<MerchantSubOrder>(`/api/merchant/orders/${id}/remark`, { remark }),
  batchShip: (data: BatchShipPayload) =>
    http.post<BatchShipResult>('/api/merchant/orders/batch-ship', data)
}
