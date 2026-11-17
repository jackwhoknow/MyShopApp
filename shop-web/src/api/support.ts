import http from './http'
import type { PageResult, SupportConversation, SupportMessage } from '@/types'

/** 买家客服接口 */
export const supportApi = {
  start: (shopId: number, orderId?: number) =>
    http.post<SupportConversation>('/api/support/conversations', { shopId, orderId }),
  conversations: () => http.get<SupportConversation[]>('/api/support/conversations'),
  messages: (id: number, afterId = 0) =>
    http.get<SupportMessage[]>(`/api/support/conversations/${id}/messages`, {
      params: { afterId }
    }),
  send: (id: number, content: string) =>
    http.post<SupportMessage>(`/api/support/conversations/${id}/messages`, { content }),
  unread: () => http.get<number>('/api/support/unread-count')
}

/** 商家客服接口 */
export const merchantSupportApi = {
  conversations: () => http.get<SupportConversation[]>('/api/merchant/support/conversations'),
  messages: (id: number, afterId = 0) =>
    http.get<SupportMessage[]>(`/api/merchant/support/conversations/${id}/messages`, {
      params: { afterId }
    }),
  send: (id: number, content: string) =>
    http.post<SupportMessage>(`/api/merchant/support/conversations/${id}/messages`, { content }),
  unread: () => http.get<number>('/api/merchant/support/unread-count')
}

/** 平台客服接口：跨店铺查看与介入 */
export const adminSupportApi = {
  conversations: (page = 0, size = 50) =>
    http.get<PageResult<SupportConversation>>('/api/admin/support/conversations', {
      params: { page, size }
    }),
  messages: (id: number, afterId = 0) =>
    http.get<SupportMessage[]>(`/api/admin/support/conversations/${id}/messages`, {
      params: { afterId }
    }),
  send: (id: number, content: string) =>
    http.post<SupportMessage>(`/api/admin/support/conversations/${id}/messages`, { content })
}
