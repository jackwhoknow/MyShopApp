import http from './http'

export type PaymentChannel = 'ALIPAY' | 'WECHAT' | 'MOCK'

export interface PaymentPrepareResult {
  channel: PaymentChannel
  /** 支付宝收银台跳转地址 */
  payUrl?: string | null
  /** 微信 Native 支付二维码内容 */
  codeUrl?: string | null
  /** true 表示已直接完成（仅本地模拟支付） */
  paid: boolean
}

export interface PaymentStatusResult {
  orderId: number
  orderNo: string
  status: string
  paid: boolean
}

export const paymentApi = {
  prepare: (orderId: number, channel: PaymentChannel) =>
    http.post<PaymentPrepareResult>(`/api/payments/${orderId}/prepare`, { channel }),
  status: (orderId: number) => http.get<PaymentStatusResult>(`/api/payments/${orderId}/status`)
}
