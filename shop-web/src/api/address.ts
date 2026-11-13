import http from './http'
import type { Address } from '@/types'

export interface AddressPayload {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  defaultAddress: boolean
}

export const addressApi = {
  list: () => http.get<Address[]>('/api/addresses'),
  create: (data: AddressPayload) => http.post<Address>('/api/addresses', data),
  update: (id: number, data: AddressPayload) => http.put<Address>(`/api/addresses/${id}`, data),
  remove: (id: number) => http.delete<void>(`/api/addresses/${id}`)
}
