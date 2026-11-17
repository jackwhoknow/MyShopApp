import http from './http'
import type {
  CheckinStatus,
  PageResult,
  PointAccount,
  PointCoupon,
  PointQuote,
  PointTransaction
} from '@/types'

export interface PointTransactionQuery {
  page?: number
  size?: number
}

export const pointApi = {
  account: () => http.get<PointAccount>('/api/points/account'),
  transactions: (params: PointTransactionQuery) =>
    http.get<PageResult<PointTransaction>>('/api/points/transactions', { params }),
  /** 抵扣试算：amount 传扣券后的应付金额 */
  quote: (amount: number) => http.get<PointQuote>('/api/points/quote', { params: { amount } }),
  checkinStatus: () => http.get<CheckinStatus>('/api/points/checkin'),
  checkin: () => http.post<CheckinStatus>('/api/points/checkin'),
  exchangeableCoupons: () => http.get<PointCoupon[]>('/api/points/coupons'),
  exchangeCoupon: (couponId: number) =>
    http.post<PointCoupon>(`/api/points/coupons/${couponId}/exchange`)
}
