import http from './http'
import type { Invoice, PageResult } from '@/types'

export interface AdminInvoiceQuery {
  /** PENDING / ISSUED，不传查全部 */
  status?: string
  page?: number
  size?: number
}

export const adminInvoiceApi = {
  list: (params: AdminInvoiceQuery) =>
    http.get<PageResult<Invoice>>('/api/admin/invoices', { params }),
  issue: (id: number) => http.post<Invoice>(`/api/admin/invoices/${id}/issue`)
}
