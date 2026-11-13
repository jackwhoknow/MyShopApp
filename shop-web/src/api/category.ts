import http from './http'
import type { CategoryNode } from '@/types'

export const categoryApi = {
  tree: () => http.get<CategoryNode[]>('/api/categories/tree')
}
