import http from './http'
import type { BrowseHistoryItem } from '@/types'

export const historyApi = {
  /** 记录一次商品浏览，重复浏览只刷新时间 */
  record: (productId: number) => http.post<void>(`/api/history/${productId}`),
  recent: (limit = 20) => http.get<BrowseHistoryItem[]>('/api/history', { params: { limit } }),
  clear: () => http.delete<void>('/api/history')
}
