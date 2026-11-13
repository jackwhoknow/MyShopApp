import http from './http'
import type { Coupon, UserCoupon } from '@/types'

export const couponApi = {
  available: () => http.get<Coupon[]>('/api/coupons/available'),
  claim: (id: number) => http.post<UserCoupon>(`/api/coupons/${id}/claim`),
  mine: (state?: string) => http.get<UserCoupon[]>('/api/coupons/mine', { params: { state } })
}
