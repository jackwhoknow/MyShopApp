import http from './http'
import type { MerchantCoupon } from '@/types'

export interface CouponCreatePayload {
  title: string
  thresholdAmount: number
  discountAmount: number
  totalCount: number
  /** 积分兑换所需积分，0 表示不参与积分兑换 */
  pointsPrice?: number
  startAt: string
  endAt: string
}

export const merchantCouponApi = {
  list: () => http.get<MerchantCoupon[]>('/api/merchant/coupons'),
  create: (data: CouponCreatePayload) => http.post<MerchantCoupon>('/api/merchant/coupons', data),
  updateStatus: (id: number, enabled: boolean) =>
    http.put<MerchantCoupon>(`/api/merchant/coupons/${id}/status`, { enabled })
}
