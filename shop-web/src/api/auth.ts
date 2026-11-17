import http from './http'
import type { LoginResult, UserInfo } from '@/types'

export interface LoginPayload {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  phone?: string
  email?: string
  merchant?: boolean
}

/** 图形验证码：id 用于校验，image 为 base64 图片 */
export interface Captcha {
  captchaId: string
  image: string
  expiresIn: number
}

export const authApi = {
  captcha: () => http.get<Captcha>('/api/auth/captcha'),
  login: (data: LoginPayload) => http.post<LoginResult>('/api/auth/login', data),
  refresh: (refreshToken: string) =>
    http.post<LoginResult>('/api/auth/refresh', { refreshToken }),
  logout: (refreshToken?: string) =>
    http.post<void>('/api/auth/logout', refreshToken ? { refreshToken } : undefined),
  register: (data: RegisterPayload) => http.post<UserInfo>('/api/auth/register', data),
  /** 忘记密码：发送邮箱验证码 */
  sendResetCode: (email: string) => http.post<void>('/api/auth/password/code', { email }),
  /** 忘记密码：邮箱验证码重置密码 */
  resetPassword: (data: { email: string; code: string; newPassword: string }) =>
    http.post<void>('/api/auth/password/reset', data)
}
