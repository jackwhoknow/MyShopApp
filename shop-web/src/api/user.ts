import http from './http'
import type { UserInfo } from '@/types'

export interface ProfileUpdatePayload {
  nickname?: string
  phone?: string
  email?: string
}

export interface PasswordChangePayload {
  oldPassword: string
  newPassword: string
}

/** 个人数据导出结果，仅用于前端下载 JSON */
export interface UserDataExport {
  exportedAt: string
  profile: UserInfo
  addresses: unknown[]
  orders: unknown[]
  reviews: unknown[]
  coupons: unknown[]
  points: {
    balance: number
    totalEarned: number
    totalSpent: number
    transactions: unknown[]
  }
}

export const userApi = {
  me: () => http.get<UserInfo>('/api/users/me'),
  updateProfile: (data: ProfileUpdatePayload) => http.put<UserInfo>('/api/users/me', data),
  changePassword: (data: PasswordChangePayload) => http.put<void>('/api/users/me/password', data),
  exportData: () => http.get<UserDataExport>('/api/users/export'),
  deactivate: (password: string) => http.post<void>('/api/users/deactivate', { password })
}
