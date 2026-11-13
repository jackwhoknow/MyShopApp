import http from './http'
import type { PageResult, Review } from '@/types'

export interface ReviewPayload {
  orderItemId: number
  rating: number
  content?: string
  /** 评价图片地址，最多 6 张 */
  images?: string[]
}

export const reviewApi = {
  create: (data: ReviewPayload) => http.post<Review>('/api/reviews', data),
  mine: (params?: { page?: number; size?: number }) =>
    http.get<PageResult<Review>>('/api/reviews/mine', { params }),
  /** 商品页评价，只返回审核通过的评价 */
  listByProduct: (productId: number, params?: { page?: number; size?: number }) =>
    http.get<PageResult<Review>>(`/api/products/${productId}/reviews`, { params })
}

export const merchantReviewApi = {
  list: (params: { page?: number; size?: number }) =>
    http.get<PageResult<Review>>('/api/merchant/reviews', { params }),
  reply: (id: number, reply: string) =>
    http.post<Review>(`/api/merchant/reviews/${id}/reply`, { reply })
}
