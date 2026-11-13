import http from './http'
import type { CartItem } from '@/types'

export interface CartAddPayload {
  skuId: number
  quantity: number
}

export const cartApi = {
  list: () => http.get<CartItem[]>('/api/cart'),
  add: (data: CartAddPayload) => http.post<CartItem>('/api/cart', data),
  updateQuantity: (id: number, quantity: number) =>
    http.put<CartItem>(`/api/cart/${id}`, { quantity }),
  remove: (id: number) => http.delete<void>(`/api/cart/${id}`),
  clear: () => http.delete<void>('/api/cart')
}
