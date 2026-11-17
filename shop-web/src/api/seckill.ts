import http from './http'
import type { SeckillActivity } from '@/types'

export interface SeckillCreatePayload {
  skuId: number
  activityPrice: number
  totalStock: number
  perUserLimit: number
  startAt: string
  endAt: string
}

export const seckillApi = {
  /** 秒杀专区：进行中 + 即将开始，公开访问 */
  list: () => http.get<SeckillActivity[]>('/api/seckill')
}

export const merchantSeckillApi = {
  list: () => http.get<SeckillActivity[]>('/api/merchant/seckill-activities'),
  create: (data: SeckillCreatePayload) =>
    http.post<SeckillActivity>('/api/merchant/seckill-activities', data),
  updateStatus: (id: number, enabled: boolean) =>
    http.put<SeckillActivity>(`/api/merchant/seckill-activities/${id}/status`, { enabled })
}
