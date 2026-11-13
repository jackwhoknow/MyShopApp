import http from './http'
import type { MerchantCoupon } from '@/types'
import type { CouponCreatePayload } from './merchantCoupon'

export const adminCouponApi = {
  list: () => http.get<MerchantCoupon[]>('/api/admin/coupons'),
  create: (data: CouponCreatePayload) => http.post<MerchantCoupon>('/api/admin/coupons', data),
  updateStatus: (id: number, enabled: boolean) =>
    http.put<MerchantCoupon>(`/api/admin/coupons/${id}/status`, { enabled })
}
