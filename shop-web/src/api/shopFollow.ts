import http from './http'
import type { FollowedShop, FollowState } from '@/types'

/** 店铺关注：游客查询状态返回未关注 */
export const shopFollowApi = {
  status: (shopId: number) =>
    http.get<FollowState>('/api/shops/follow-status', { params: { shopId } }),
  follow: (shopId: number) => http.post<FollowState>(`/api/shops/${shopId}/follow`),
  unfollow: (shopId: number) => http.delete<FollowState>(`/api/shops/${shopId}/follow`),
  mine: () => http.get<FollowedShop[]>('/api/me/followed-shops')
}
