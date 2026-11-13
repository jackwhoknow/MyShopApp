import http from './http'
import type { AdminDashboard } from '@/types'

export const adminDashboardApi = {
  overview: (days = 7) => http.get<AdminDashboard>('/api/admin/dashboard', { params: { days } })
}
