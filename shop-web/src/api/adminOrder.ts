import axios from 'axios'
import http from './http'
import type { AdminOrder, AdminOrderDetail, PageResult } from '@/types'

/** 后端 OrderStatus 枚举 */
export type AdminOrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'COMPLETED' | 'CANCELLED'

export interface AdminOrderQuery {
  status?: AdminOrderStatus
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export const adminOrderApi = {
  list: (params: AdminOrderQuery) =>
    http.get<PageResult<AdminOrder>>('/api/admin/orders', { params }),
  detail: (id: number) => http.get<AdminOrderDetail>(`/api/admin/orders/${id}`),
  /**
   * 导出订单 CSV：响应是二进制文件，不能走统一拦截器解包，
   * 这里直接用原生 axios 并手动带上 Authorization。
   */
  exportCsv: async (params: Omit<AdminOrderQuery, 'page' | 'size'>) => {
    const response = await axios.get<Blob>(
      `${import.meta.env.VITE_API_BASE ?? ''}/api/admin/orders/export`,
      {
        params,
        responseType: 'blob',
        headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` }
      }
    )
    return response.data
  }
}
