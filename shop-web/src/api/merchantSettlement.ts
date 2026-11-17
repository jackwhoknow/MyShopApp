import http from './http'
import type { PageResult, SettlementBill, SettlementSummary, WithdrawalRequest } from '@/types'

export interface SettlementBillQuery {
  status?: string
  keyword?: string
  page?: number
  size?: number
}

export interface WithdrawalCreatePayload {
  amount: number
  accountName: string
  accountNo: string
  bankName: string
}

export const merchantSettlementApi = {
  summary: () => http.get<SettlementSummary>('/api/merchant/settlements/summary'),
  bills: (params: SettlementBillQuery) =>
    http.get<PageResult<SettlementBill>>('/api/merchant/settlements/bills', { params }),
  withdrawals: (params: { page?: number; size?: number }) =>
    http.get<PageResult<WithdrawalRequest>>('/api/merchant/settlements/withdrawals', { params }),
  createWithdrawal: (payload: WithdrawalCreatePayload) =>
    http.post<WithdrawalRequest>('/api/merchant/settlements/withdrawals', payload)
}
