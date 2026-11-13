import http from './http'
import type { Order, PageResult, ReorderResult, SubOrderLogistics } from '@/types'

export interface CreateOrderPayload {
  addressId: number
  remark?: string
  cartItemIds?: number[]
  /** 本次使用的领券记录 id，每个店铺最多一张 */
  userCouponIds?: number[]
}

/** 秒杀下单：requestId 为幂等键，同一请求重复提交不会生成多张订单 */
export interface SeckillOrderPayload extends CreateOrderPayload {
  requestId?: string
}

/** 排队下单结果：PENDING 继续轮询，SUCCESS 带回订单 id，FAILED 带回失败原因 */
export interface AsyncOrderResult {
  requestId: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
  orderId?: number
  totalAmount?: number
  message?: string
}

export interface OrderQuery {
  status?: string
  page?: number
  size?: number
}

export const orderApi = {
  create: (data: CreateOrderPayload) => http.post<Order>('/api/orders', data),
  /** 秒杀下单：Redis 预扣库存后进入排队队列 */
  seckill: (data: SeckillOrderPayload) =>
    http.post<AsyncOrderResult>('/api/orders/seckill', data),
  /** 轮询排队下单结果 */
  seckillStatus: (requestId: string) =>
    http.get<AsyncOrderResult>(`/api/orders/seckill/${requestId}`),
  list: (params: OrderQuery) => http.get<PageResult<Order>>('/api/orders', { params }),
  detail: (id: number) => http.get<Order>(`/api/orders/${id}`),
  /** 某个子订单的物流轨迹（模拟数据） */
  logistics: (orderId: number, subOrderId: number) =>
    http.get<SubOrderLogistics>(`/api/orders/${orderId}/sub-orders/${subOrderId}/logistics`),
  cancel: (id: number) => http.post<Order>(`/api/orders/${id}/cancel`),
  /** 一键复购：按当前库存把订单商品加入购物车 */
  reorder: (id: number) => http.post<ReorderResult>(`/api/orders/${id}/reorder`),
  confirm: (id: number) => http.post<Order>(`/api/orders/${id}/confirm`)
}
