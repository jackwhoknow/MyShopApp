import http from './http'
import type { Shop } from '@/types'

export interface ShopApplyPayload {
  name: string
  description?: string
}

export const merchantShopApi = {
  /** 当前商家最新一次店铺（没有申请过会返回 404） */
  mine: () => http.get<Shop>('/api/merchant/shops/mine'),
  apply: (data: ShopApplyPayload) => http.post<Shop>('/api/merchant/shops/apply', data)
}
