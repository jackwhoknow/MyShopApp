import http from './http'
import type { UserInfo } from '@/types'

export interface ProfileUpdatePayload {
  nickname?: string
  phone?: string
}

export interface PasswordChangePayload {
  oldPassword: string
  newPassword: string
}

export const userApi = {
  me: () => http.get<UserInfo>('/api/users/me'),
  updateProfile: (data: ProfileUpdatePayload) => http.put<UserInfo>('/api/users/me', data),
  changePassword: (data: PasswordChangePayload) => http.put<void>('/api/users/me/password', data)
}
