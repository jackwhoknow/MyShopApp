import http from './http'
import type { ShopPromotion } from '@/types'

export interface PromotionPayload {
  title: string
  thresholdAmount: number
  reduceAmount: number
  startAt: string
  endAt: string
  enabled?: boolean
}

/** 满减活动：商家管理与前台查询 */
export const promotionApi = {
  merchantList: () => http.get<ShopPromotion[]>('/api/merchant/promotions'),
  create: (data: PromotionPayload) => http.post<ShopPromotion>('/api/merchant/promotions', data),
  update: (id: number, data: PromotionPayload) =>
    http.put<ShopPromotion>(`/api/merchant/promotions/${id}`, data),
  remove: (id: number) => http.delete<void>(`/api/merchant/promotions/${id}`),
  activeByShop: (shopId: number) =>
    http.get<ShopPromotion[]>('/api/promotions', { params: { shopId } })
}
