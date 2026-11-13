import http from './http'
import type { PageResult, Review } from '@/types'

export const adminReviewApi = {
  list: (params: { status?: string; keyword?: string; page?: number; size?: number }) =>
    http.get<PageResult<Review>>('/api/admin/reviews', { params }),
  approve: (id: number) => http.post<Review>(`/api/admin/reviews/${id}/approve`),
  reject: (id: number, remark?: string) =>
    http.post<Review>(`/api/admin/reviews/${id}/reject`, { remark })
}
