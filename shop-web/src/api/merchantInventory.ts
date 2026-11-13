import http from './http'
import type { PageResult, StockAlert, StockAlertSummary } from '@/types'

export const merchantInventoryApi = {
  alerts: (params: { keyword?: string; page?: number; size?: number }) =>
    http.get<PageResult<StockAlert>>('/api/merchant/inventory/alerts', { params }),
  summary: () => http.get<StockAlertSummary>('/api/merchant/inventory/summary'),
  updateThreshold: (threshold: number) =>
    http.put<StockAlertSummary>('/api/merchant/inventory/threshold', { threshold }),
  restock: (skuId: number, stock: number) =>
    http.put<StockAlert>(`/api/merchant/inventory/skus/${skuId}/stock`, { stock })
}
