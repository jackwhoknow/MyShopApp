import http from './http'
import type { MerchantStats } from '@/types'

export const merchantStatsApi = {
  overview: (days: number) => http.get<MerchantStats>('/api/merchant/stats/overview', { params: { days } })
}
