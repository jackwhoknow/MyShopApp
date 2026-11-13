import http from './http'
import type { FavoriteProduct, PageResult } from '@/types'

export const favoriteApi = {
  add: (productId: number) => http.post<void>(`/api/favorites/${productId}`),
  remove: (productId: number) => http.delete<void>(`/api/favorites/${productId}`),
  mine: (params: { page?: number; size?: number } = {}) =>
    http.get<PageResult<FavoriteProduct>>('/api/favorites', { params }),
  /** 批量查询已收藏的商品 id，用于列表页标记状态 */
  ids: (productIds: number[]) =>
    http.get<number[]>('/api/favorites/ids', {
      params: { productIds: productIds.join(',') }
    })
}
