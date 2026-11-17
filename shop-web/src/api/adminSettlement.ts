import http from './http'
import type { AdminSettlementSummary, PageResult, WithdrawalRequest } from '@/types'

export interface AdminWithdrawalQuery {
  status?: string
  keyword?: string
  page?: number
  size?: number
}

export const adminSettlementApi = {
  summary: () => http.get<AdminSettlementSummary>('/api/admin/settlements/summary'),
  withdrawals: (params: AdminWithdrawalQuery) =>
    http.get<PageResult<WithdrawalRequest>>('/api/admin/settlements/withdrawals', { params }),
  approve: (id: number) =>
    http.post<WithdrawalRequest>(`/api/admin/settlements/withdrawals/${id}/approve`),
  reject: (id: number, remark: string) =>
    http.post<WithdrawalRequest>(`/api/admin/settlements/withdrawals/${id}/reject`, { remark }),
  pay: (id: number) =>
    http.post<WithdrawalRequest>(`/api/admin/settlements/withdrawals/${id}/pay`)
}
