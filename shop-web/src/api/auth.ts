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
  register: (data: RegisterPayload) => http.post<UserInfo>('/api/auth/register', data)
}
