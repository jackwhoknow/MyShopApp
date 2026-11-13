import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, LoginResult } from '@/types'

/**
 * 统一的 axios 实例：
 * 1. 请求自动带 JWT；
 * 2. 响应自动解包 ApiResponse.data，业务码非 0 统一提示；
 * 3. access token 过期（401）时用 refresh token 自动续期并重放原请求，刷新失败才跳登录页。
 */
const baseURL = import.meta.env.VITE_API_BASE ?? ''
const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refresh_token'
const USER_KEY = 'user'

const instance = axios.create({ baseURL, timeout: 10000 })

let refreshPromise: Promise<string> | null = null

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 并发 401 只发起一次刷新，其余请求复用同一个 Promise */
function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = localStorage.getItem(REFRESH_KEY)
      if (!refreshToken) {
        throw new Error('缺少 refresh token')
      }
      // 用裸 axios，避免再次进入本拦截器造成递归
      const response = await axios.post(`${baseURL}/api/auth/refresh`, { refreshToken })
      const body = response.data as ApiResponse<LoginResult>
      if (body.code !== 0 || !body.data) {
        throw new Error(body.message || '刷新登录态失败')
      }
      localStorage.setItem(TOKEN_KEY, body.data.token)
      localStorage.setItem(REFRESH_KEY, body.data.refreshToken)
      return body.data.token
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  // 这里刻意返回业务数据而不是 AxiosResponse，类型在下方 Http 接口里收窄
  (response): any => {
    const body = response.data as ApiResponse<unknown>
    if (body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body.data
  },
  async (error) => {
    const status = error.response?.status
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const url = config?.url ?? ''
    const canRefresh =
      status === 401 &&
      config &&
      !config._retried &&
      !url.includes('/auth/login') &&
      !url.includes('/auth/refresh')

    if (canRefresh) {
      config._retried = true
      try {
        const token = await refreshAccessToken()
        config.headers.Authorization = `Bearer ${token}`
        return instance(config)
      } catch {
        clearSession()
        if (!location.pathname.startsWith('/login')) {
          location.href = '/login'
        }
        return Promise.reject(error)
      }
    }

    const message = error.response?.data?.message || '网络异常，请稍后重试'
    if (status === 401) {
      clearSession()
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

/** 因为响应拦截器已经返回 data，这里收窄类型，调用处直接拿到业务数据 */
interface Http {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T>
}

export default instance as unknown as Http
