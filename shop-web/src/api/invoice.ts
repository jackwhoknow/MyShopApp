import http from './http'
import type { Invoice, PageResult } from '@/types'

export interface InvoiceApplyPayload {
  type: 'PERSONAL' | 'COMPANY'
  title: string
  /** 单位抬头必填 */
  taxNo?: string
  email?: string
}

export interface InvoiceQuery {
  status?: string
  page?: number
  size?: number
}

export const invoiceApi = {
  /** 对已支付订单申请开票 */
  apply: (orderId: number, data: InvoiceApplyPayload) =>
    http.post<Invoice>(`/api/invoices/orders/${orderId}`, data),
  /** 查询某订单的发票，未申请时返回 null */
  findByOrder: (orderId: number) => http.get<Invoice | null>(`/api/invoices/orders/${orderId}`),
  mine: (params: { page?: number; size?: number } = {}) =>
    http.get<PageResult<Invoice>>('/api/invoices', { params })
}
