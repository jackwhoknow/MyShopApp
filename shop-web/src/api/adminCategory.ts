import http from './http'
import type { AdminCategoryNode } from '@/types'

export interface CategoryPayload {
  /** null 表示一级分类 */
  parentId?: number
  name: string
  sortOrder?: number
  enabled?: boolean
}

export const adminCategoryApi = {
  tree: () => http.get<AdminCategoryNode[]>('/api/admin/categories/tree'),
  create: (data: CategoryPayload) => http.post<AdminCategoryNode>('/api/admin/categories', data),
  update: (id: number, data: CategoryPayload) =>
    http.put<AdminCategoryNode>(`/api/admin/categories/${id}`, data),
  remove: (id: number) => http.delete<void>(`/api/admin/categories/${id}`)
}
